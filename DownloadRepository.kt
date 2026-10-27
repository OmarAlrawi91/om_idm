package com.example.data

import android.content.Context
import android.util.Log
import com.example.engine.DownloadEngine
import com.example.engine.MagnetParser
import com.example.engine.TorrentEngine
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.FileType
import com.example.model.TorrentMetadata
import com.example.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineStart

import com.example.engine.TokenBucketLimiter

class DownloadRepository(
    private val context: Context,
    private val downloadDao: DownloadDao,
    val downloadEngine: DownloadEngine = DownloadEngine(context),
    val torrentEngine: TorrentEngine = TorrentEngine(context, downloadEngine),
    val tokenBucketLimiter: TokenBucketLimiter = TokenBucketLimiter(0L)
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    val networkMonitor = NetworkMonitor(context)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    // Global speed throttle setting (0 = Unlimited)
    val globalSpeedLimit = MutableStateFlow(0L)

    // Concurrency limit setting
    private val _maxConcurrentDownloads = MutableStateFlow(3)
    val maxConcurrentDownloads = _maxConcurrentDownloads.asStateFlow()

    // Global off-peak schedule window settings
    val globalScheduleEnabled = MutableStateFlow(false)
    val globalStartHour = MutableStateFlow(2)
    val globalStartMinute = MutableStateFlow(0)
    val globalPauseHour = MutableStateFlow(7)
    val globalPauseMinute = MutableStateFlow(0)

    // Schedule notification event flow
    private val _scheduleEvents = MutableSharedFlow<String>()
    val scheduleEvents = _scheduleEvents.asSharedFlow()

    init {
        monitorNetworkState()
        startSchedulerLoop()
    }

    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()
    val torrentDownloads: Flow<List<DownloadItem>> = downloadDao.getTorrentDownloads()

    companion object {
        private const val TAG = "DownloadRepository"

        @Volatile
        private var INSTANCE: DownloadRepository? = null

        fun getInstance(context: Context): DownloadRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val instance = DownloadRepository(context.applicationContext, db.downloadDao())
                INSTANCE = instance
                instance
            }
        }
    }

    private fun monitorNetworkState() {
        repositoryScope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                if (!online) {
                    Log.d(TAG, "Network disconnected. Halting active downloads.")
                    // Immediately cancel all active download worker jobs
                    activeJobs.values.forEach { if (it.isActive) it.cancel() }
                    activeJobs.clear()

                    // Update active downloads to Offline / Paused
                    val actives = downloadDao.getActiveOrQueuedDownloads()
                    for (item in actives) {
                        if (item.status == DownloadStatus.DOWNLOADING) {
                            downloadDao.updateProgress(item.id, item.downloadedBytes, item.totalBytes, 0L, -1L)
                            downloadDao.updateStatus(item.id, DownloadStatus.PAUSED, "Offline / Paused (No Internet)")
                        }
                    }
                    _scheduleEvents.emit("Network disconnected. Downloads paused.")
                } else {
                    Log.d(TAG, "Network online. Resuming network-paused downloads.")
                    for (item in downloadDao.getActiveOrQueuedDownloads()) {
                        if (item.status == DownloadStatus.PAUSED && item.errorMessage?.startsWith("Offline") == true) {
                            downloadDao.updateStatus(item.id, DownloadStatus.QUEUED, null)
                        }
                    }
                    checkAndStartNextQueued()
                }
            }
        }
    }

    fun setMaxConcurrentDownloads(limit: Int) {
        _maxConcurrentDownloads.value = limit.coerceIn(1, 10)
        checkAndStartNextQueued()
    }

    fun setGlobalSpeedLimit(bytesPerSec: Long) {
        val safe = bytesPerSec.coerceAtLeast(0L)
        globalSpeedLimit.value = safe
        tokenBucketLimiter.setRateLimit(safe)
    }

    suspend fun probeUrl(url: String): DownloadEngine.ProbeResult {
        return downloadEngine.probeUrl(url)
    }

    suspend fun getDownloadByIdDirect(id: Long): DownloadItem? {
        return downloadDao.getDownloadByIdDirect(id)
    }

    /**
     * Add new regular HTTP/HTTPS download with auto-folder sorting based on FileType
     */
    suspend fun addDownload(
        url: String,
        fileName: String,
        fileType: FileType,
        totalBytes: Long = -1L,
        numSegments: Int = 1,
        autoStart: Boolean = true
    ): Long {
        val sortedDir = downloadEngine.getDownloadDirectory(fileType)
        val savePath = File(sortedDir, fileName).absolutePath
        val isOnlineNow = networkMonitor.isConnected()

        val initialStatus = when {
            !autoStart -> DownloadStatus.PAUSED
            !isOnlineNow -> DownloadStatus.PAUSED
            else -> DownloadStatus.QUEUED
        }

        val item = DownloadItem(
            url = url,
            fileName = fileName,
            fileType = fileType,
            totalBytes = totalBytes,
            downloadedBytes = 0L,
            status = initialStatus,
            errorMessage = if (!isOnlineNow) "Offline / Paused (No Internet)" else null,
            savePath = savePath,
            numSegments = numSegments,
            isTorrent = fileType == FileType.TORRENT || fileName.endsWith(".torrent")
        )

        val id = downloadDao.insert(item)
        if (autoStart && isOnlineNow) {
            checkAndStartNextQueued()
        }
        return id
    }

    /**
     * Add Torrent file or Magnet URL with auto-folder sorting
     */
    suspend fun addTorrent(rawInput: String, autoStart: Boolean = true): Long {
        val isOnlineNow = networkMonitor.isConnected()

        val item = if (rawInput.startsWith("magnet:?")) {
            val magnet = MagnetParser.parse(rawInput)
                ?: throw IllegalArgumentException("Invalid Magnet link format")
            torrentEngine.createDownloadItemFromMagnet(magnet, rawInput)
        } else if (rawInput.startsWith("http://") || rawInput.startsWith("https://")) {
            try {
                val (metadata, _) = torrentEngine.fetchAndParseTorrentUrl(rawInput)
                torrentEngine.createDownloadItemFromMetadata(metadata, rawInput)
            } catch (_: Exception) {
                val probe = downloadEngine.probeUrl(rawInput)
                val savePath = File(downloadEngine.getDownloadDirectory(FileType.TORRENT), probe.fileName).absolutePath
                DownloadItem(
                    url = rawInput,
                    fileName = probe.fileName,
                    fileType = FileType.TORRENT,
                    totalBytes = probe.totalBytes,
                    status = DownloadStatus.QUEUED,
                    savePath = savePath,
                    isTorrent = true
                )
            }
        } else {
            val file = File(rawInput)
            val metadata = torrentEngine.parseTorrentFile(file)
            torrentEngine.createDownloadItemFromMetadata(metadata, file.toURI().toString())
        }

        val initialStatus = when {
            !autoStart -> DownloadStatus.PAUSED
            !isOnlineNow -> DownloadStatus.PAUSED
            else -> DownloadStatus.QUEUED
        }

        val updatedItem = item.copy(
            status = initialStatus,
            errorMessage = if (!isOnlineNow) "Offline / Paused (No Internet)" else null
        )
        val id = downloadDao.insert(updatedItem)
        if (autoStart && isOnlineNow) {
            checkAndStartNextQueued()
        }
        return id
    }

    fun resumeDownload(id: Long) {
        repositoryScope.launch {
            if (!networkMonitor.isConnected()) {
                _scheduleEvents.emit("Cannot resume: No internet connection (Offline)")
                return@launch
            }
            downloadDao.updateStatus(id, DownloadStatus.QUEUED, null)
            checkAndStartNextQueued()
        }
    }

    fun restartDownload(id: Long) {
        val job = activeJobs.remove(id)
        job?.cancel()

        repositoryScope.launch {
            val item = downloadDao.getDownloadByIdDirect(id) ?: return@launch
            try {
                val file = File(item.savePath)
                if (file.exists()) file.delete()
                val part = File(downloadEngine.getDownloadDirectory(item.fileType), "${item.fileName}.part")
                if (part.exists()) part.delete()
                val torrentPart = File(downloadEngine.getDownloadDirectory(FileType.TORRENT), "${item.fileName}.torrentpart")
                if (torrentPart.exists()) torrentPart.delete()
            } catch (_: Exception) {}

            val isOnlineNow = networkMonitor.isConnected()
            downloadDao.update(
                item.copy(
                    downloadedBytes = 0L,
                    speedBytesPerSec = 0L,
                    etaSeconds = -1L,
                    status = if (isOnlineNow) DownloadStatus.QUEUED else DownloadStatus.PAUSED,
                    errorMessage = if (!isOnlineNow) "Offline / Paused (No Internet)" else null,
                    completedAt = null,
                    torrentPiecesDone = 0
                )
            )
            if (isOnlineNow) {
                checkAndStartNextQueued()
            }
        }
    }

    fun pauseDownload(id: Long) {
        val activeJob = activeJobs[id]
        if (activeJob != null && activeJob.isActive) {
            activeJob.cancel()
        }
        activeJobs.remove(id)

        repositoryScope.launch {
            val item = downloadDao.getDownloadByIdDirect(id)
            val downloadedBytes = item?.downloadedBytes ?: 0L
            downloadDao.updateProgress(id = id, downloadedBytes = downloadedBytes, totalBytes = item?.totalBytes ?: -1L, speed = 0L, eta = -1L)
            downloadDao.updateStatus(id, DownloadStatus.PAUSED, null)
            checkAndStartNextQueued()
        }
    }

    fun pauseAll() {
        activeJobs.values.forEach { if (it.isActive) it.cancel() }
        activeJobs.clear()

        repositoryScope.launch {
            downloadDao.pauseAllDownloading()
        }
    }

    fun resumeAll() {
        repositoryScope.launch {
            if (!networkMonitor.isConnected()) {
                _scheduleEvents.emit("Cannot resume all: No internet connection (Offline)")
                return@launch
            }
            // Resume all paused items
            val candidates = downloadDao.getActiveOrQueuedDownloads()
            for (c in candidates) {
                if (c.status == DownloadStatus.PAUSED) {
                    downloadDao.updateStatus(c.id, DownloadStatus.QUEUED, null)
                }
            }
            checkAndStartNextQueued()
        }
    }

    fun deleteDownload(id: Long, deleteFile: Boolean) {
        val activeJob = activeJobs.remove(id)
        activeJob?.cancel()

        repositoryScope.launch {
            val item = downloadDao.getDownloadByIdDirect(id)
            if (item != null && deleteFile) {
                try {
                    val file = File(item.savePath)
                    if (file.exists()) file.delete()
                    val part = File(downloadEngine.getDownloadDirectory(item.fileType), "${item.fileName}.part")
                    if (part.exists()) part.delete()
                    val torrentPart = File(downloadEngine.getDownloadDirectory(FileType.TORRENT), "${item.fileName}.torrentpart")
                    if (torrentPart.exists()) torrentPart.delete()
                } catch (_: Exception) {}
            }
            downloadDao.deleteById(id)
            checkAndStartNextQueued()
        }
    }

    fun clearCompleted() {
        repositoryScope.launch {
            downloadDao.clearCompleted()
        }
    }

    private val queueMutex = kotlinx.coroutines.sync.Mutex()

    private fun checkAndStartNextQueued() {
        repositoryScope.launch {
            queueMutex.withLock {
            if (!networkMonitor.isConnected()) {
                Log.d(TAG, "checkAndStartNextQueued: device is offline. Aborting queue processing.")
                return@launch
            }

            val activeCount = activeJobs.size
            val slotsAvailable = _maxConcurrentDownloads.value - activeCount
            if (slotsAvailable <= 0) return@launch

            val candidates = downloadDao.getActiveOrQueuedDownloads()
            val queued = candidates.filter { it.status == DownloadStatus.QUEUED && !activeJobs.containsKey(it.id) }

            for (i in 0 until minOf(slotsAvailable, queued.size)) {
                startDownloadJob(queued[i])
            }
            }
        }
    }

    private fun startDownloadJob(item: DownloadItem) {
        if (activeJobs.containsKey(item.id)) return
        if (!networkMonitor.isConnected()) {
            repositoryScope.launch {
                downloadDao.updateStatus(item.id, DownloadStatus.PAUSED, "Offline / Paused (No Internet)")
            }
            return
        }

        val job = repositoryScope.launch(start = CoroutineStart.LAZY) {
          try {
            downloadDao.updateStatus(item.id, DownloadStatus.DOWNLOADING, null)

            if (item.isTorrent) {
                torrentEngine.downloadTorrent(
                    item = item,
                    onProgress = { downloaded, total, speed, eta, piecesDone, seeders, leechers ->
                        if (!networkMonitor.isConnected()) {
                            // Dropped offline during torrent download
                            downloadDao.updateProgress(item.id, downloaded, total, 0L, -1L)
                            downloadDao.updateStatus(item.id, DownloadStatus.PAUSED, "Offline / Paused (No Internet)")
                            return@downloadTorrent
                        }

                        downloadDao.updateProgress(item.id, downloaded, total, speed, eta)
                        val current = downloadDao.getDownloadByIdDirect(item.id)
                        if (current != null) {
                            downloadDao.update(
                                current.copy(
                                    downloadedBytes = downloaded,
                                    totalBytes = total,
                                    speedBytesPerSec = speed,
                                    etaSeconds = eta,
                                    torrentPiecesDone = piecesDone,
                                    torrentSeeders = seeders,
                                    torrentLeechers = leechers
                                )
                            )
                        }
                    },
                    onStatusChange = { status, error, outputPath ->
                        downloadDao.updateStatus(item.id, status, error)
                        if (status == DownloadStatus.COMPLETED) {
                            val curr = downloadDao.getDownloadByIdDirect(item.id)
                            if (curr != null) {
                                downloadDao.update(
                                    curr.copy(
                                        status = status,
                                        savePath = outputPath?.takeIf { File(it).isFile } ?: curr.savePath,
                                        fileName = outputPath?.let { File(it).name } ?: curr.fileName,
                                        mimeType = outputPath?.let { path ->
                                            when (File(path).extension.lowercase()) {
                                                "mkv" -> "video/x-matroska"
                                                "avi" -> "video/x-msvideo"
                                                "webm" -> "video/webm"
                                                "mov" -> "video/quicktime"
                                                "mp3" -> "audio/mpeg"
                                                "m4a" -> "audio/mp4"
                                                else -> "video/mp4"
                                            }
                                        } ?: curr.mimeType,
                                        completedAt = System.currentTimeMillis(),
                                        speedBytesPerSec = 0
                                    )
                                )
                            }
                        }
                        activeJobs.remove(item.id)
                        checkAndStartNextQueued()
                    }
                )
            } else {
                downloadEngine.download(
                    downloadItem = item,
                    speedLimiter = tokenBucketLimiter,
                    onProgress = { downloaded, total, speed, eta, segmentJson ->
                        if (!networkMonitor.isConnected()) {
                            // Dropped offline during HTTP download
                            downloadDao.updateProgress(item.id, downloaded, total, 0L, -1L)
                            downloadDao.updateStatus(item.id, DownloadStatus.PAUSED, "Offline / Paused (No Internet)")
                            return@download
                        }
                        downloadDao.updateProgressWithSegments(item.id, downloaded, total, speed, eta, segmentJson)
                    },
                    onStatusChange = { status, error, outputPath ->
                        downloadDao.updateStatus(item.id, status, error)
                        if (status == DownloadStatus.COMPLETED) {
                            val curr = downloadDao.getDownloadByIdDirect(item.id)
                            if (curr != null) {
                                downloadDao.update(
                                    curr.copy(
                                        status = status,
                                        savePath = outputPath?.takeIf { File(it).isFile } ?: curr.savePath,
                                        fileName = outputPath?.let { File(it).name } ?: curr.fileName,
                                        mimeType = outputPath?.let { path ->
                                            when (File(path).extension.lowercase()) {
                                                "mkv" -> "video/x-matroska"
                                                "avi" -> "video/x-msvideo"
                                                "webm" -> "video/webm"
                                                "mov" -> "video/quicktime"
                                                "mp3" -> "audio/mpeg"
                                                "m4a" -> "audio/mp4"
                                                else -> "video/mp4"
                                            }
                                        } ?: curr.mimeType,
                                        completedAt = System.currentTimeMillis(),
                                        speedBytesPerSec = 0
                                    )
                                )
                            }
                        }
                        activeJobs.remove(item.id)
                        checkAndStartNextQueued()
                    }
                )
            }
          } finally {
            activeJobs.remove(item.id)
          }
        }

        if (activeJobs.putIfAbsent(item.id, job) != null) {
            job.cancel()
            return
        }
        job.start()
    }

    private fun startSchedulerLoop() {
        repositoryScope.launch {
            var windowWasActive = false

            while (isActive) {
                try {
                    val now = System.currentTimeMillis()
                    val calendar = Calendar.getInstance()
                    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                    val currentMinute = calendar.get(Calendar.MINUTE)
                    val currentDayMinutes = currentHour * 60 + currentMinute

                    val scheduledItems = downloadDao.getScheduledDownloads()
                    for (item in scheduledItems) {
                        if (item.scheduledStartTime != null && now >= item.scheduledStartTime) {
                            Log.d(TAG, "Scheduler starting item ${item.id}: ${item.fileName}")
                            _scheduleEvents.emit("Scheduled download started: ${item.fileName}")
                            val updated = item.copy(
                                scheduledStartTime = null,
                                isScheduled = item.scheduledPauseTime != null
                            )
                            downloadDao.update(updated)
                            resumeDownload(item.id)
                        }

                        if (item.scheduledPauseTime != null && now >= item.scheduledPauseTime) {
                            Log.d(TAG, "Scheduler pausing item ${item.id}: ${item.fileName}")
                            _scheduleEvents.emit("Scheduled download paused: ${item.fileName}")
                            val updated = item.copy(
                                scheduledPauseTime = null,
                                isScheduled = false
                            )
                            downloadDao.update(updated)
                            pauseDownload(item.id)
                        }
                    }

                    if (globalScheduleEnabled.value) {
                        val startDayMinutes = globalStartHour.value * 60 + globalStartMinute.value
                        val pauseDayMinutes = globalPauseHour.value * 60 + globalPauseMinute.value

                        val isInsideWindow = if (startDayMinutes <= pauseDayMinutes) {
                            currentDayMinutes in startDayMinutes until pauseDayMinutes
                        } else {
                            currentDayMinutes >= startDayMinutes || currentDayMinutes < pauseDayMinutes
                        }

                        if (isInsideWindow && !windowWasActive) {
                            windowWasActive = true
                            _scheduleEvents.emit("Scheduled window started! Resuming downloads...")
                            resumeAll()
                        } else if (!isInsideWindow && windowWasActive) {
                            windowWasActive = false
                            _scheduleEvents.emit("Scheduled window ended! Pausing active downloads...")
                            pauseAll()
                        }
                    }

                } catch (e: Exception) {
                    Log.w(TAG, "Scheduler loop exception: ${e.message}")
                }
                delay(5000)
            }
        }
    }

    suspend fun scheduleDownload(id: Long, startTime: Long?, pauseTime: Long?) {
        val item = downloadDao.getDownloadByIdDirect(id) ?: return
        val willStartLater = startTime != null && startTime > System.currentTimeMillis()
        if (willStartLater && item.status == DownloadStatus.DOWNLOADING) {
            pauseDownload(id)
        }

        val updated = item.copy(
            scheduledStartTime = startTime,
            scheduledPauseTime = pauseTime,
            isScheduled = startTime != null || pauseTime != null,
            status = if (willStartLater && item.status == DownloadStatus.DOWNLOADING) DownloadStatus.PAUSED else item.status
        )
        downloadDao.update(updated)
        _scheduleEvents.emit("Schedule updated for ${item.fileName}")
    }

    suspend fun clearSchedule(id: Long) {
        val item = downloadDao.getDownloadByIdDirect(id) ?: return
        val updated = item.copy(
            scheduledStartTime = null,
            scheduledPauseTime = null,
            isScheduled = false
        )
        downloadDao.update(updated)
        _scheduleEvents.emit("Schedule cleared for ${item.fileName}")
    }

    fun setGlobalSchedule(enabled: Boolean, startH: Int, startM: Int, pauseH: Int, pauseM: Int) {
        globalScheduleEnabled.value = enabled
        globalStartHour.value = startH
        globalStartMinute.value = startM
        globalPauseHour.value = pauseH
        globalPauseMinute.value = pauseM
    }
}
