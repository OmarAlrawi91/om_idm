package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DownloadRepository
import com.example.engine.DownloadEngine
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.FileType
import com.example.service.DownloadService
import com.example.ui.theme.OneUiAccent
import com.example.util.MxPlayerHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class DownloadFilterTab {
    ACTIVE,
    COMPLETED_FAILED
}

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository.getInstance(application)

    val isOnline: StateFlow<Boolean> = repository.isOnline

    val allDownloads = repository.allDownloads.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val torrentDownloads = repository.torrentDownloads.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val maxConcurrentDownloads = repository.maxConcurrentDownloads

    private val _selectedFilterTab = MutableStateFlow(DownloadFilterTab.ACTIVE)
    val selectedFilterTab: StateFlow<DownloadFilterTab> = _selectedFilterTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow<FileType?>(null)
    val selectedCategory: StateFlow<FileType?> = _selectedCategory.asStateFlow()

    private val _selectedStatus = MutableStateFlow<DownloadStatus?>(null)
    val selectedStatus: StateFlow<DownloadStatus?> = _selectedStatus.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isProbing = MutableStateFlow(false)
    val isProbing: StateFlow<Boolean> = _isProbing.asStateFlow()

    private val _probeResult = MutableStateFlow<DownloadEngine.ProbeResult?>(null)
    val probeResult: StateFlow<DownloadEngine.ProbeResult?> = _probeResult.asStateFlow()

    val globalSpeedLimit: StateFlow<Long> = repository.globalSpeedLimit.asStateFlow()

    private val _darkThemePreset = MutableStateFlow(com.example.ui.theme.DarkThemePreset.VIOLET)
    val darkThemePreset: StateFlow<com.example.ui.theme.DarkThemePreset> = _darkThemePreset.asStateFlow()

    private val _isOledMode = MutableStateFlow(false)
    val isOledMode: StateFlow<Boolean> = _isOledMode.asStateFlow()

    private val _oneUiAccent = MutableStateFlow(OneUiAccent.VELVET_VIOLET)
    val oneUiAccent: StateFlow<OneUiAccent> = _oneUiAccent.asStateFlow()

    private val _formatModalUrl = MutableStateFlow<String?>(null)
    val formatModalUrl: StateFlow<String?> = _formatModalUrl.asStateFlow()

    fun showFormatModal(url: String) {
        _formatModalUrl.value = url
    }

    fun dismissFormatModal() {
        _formatModalUrl.value = null
    }

    fun setDarkThemePreset(preset: com.example.ui.theme.DarkThemePreset) {
        _darkThemePreset.value = preset
        _isOledMode.value = (preset == com.example.ui.theme.DarkThemePreset.OLED_BLACK)
    }

    fun setOledMode(enabled: Boolean) {
        _isOledMode.value = enabled
        _darkThemePreset.value = if (enabled) com.example.ui.theme.DarkThemePreset.OLED_BLACK else com.example.ui.theme.DarkThemePreset.MIDNIGHT_VELVET
    }

    fun setOneUiAccent(accent: OneUiAccent) {
        _oneUiAccent.value = accent
    }

    fun setGlobalSpeedLimit(bytesPerSec: Long) {
        repository.setGlobalSpeedLimit(bytesPerSec)
        viewModelScope.launch {
            if (bytesPerSec <= 0L) {
                _toastEvent.emit("Speed limit removed (Unlimited)")
            } else {
                val formatted = if (bytesPerSec >= 1024 * 1024) "${bytesPerSec / (1024 * 1024)} MB/s" else "${bytesPerSec / 1024} KB/s"
                _toastEvent.emit("Global download speed capped at $formatted")
            }
        }
    }

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    val globalScheduleEnabled = repository.globalScheduleEnabled
    val globalStartHour = repository.globalStartHour
    val globalStartMinute = repository.globalStartMinute
    val globalPauseHour = repository.globalPauseHour
    val globalPauseMinute = repository.globalPauseMinute

    init {
        try {
            DownloadService.startService(application)
        } catch (_: Exception) {}

        viewModelScope.launch {
            repository.scheduleEvents.collectLatest { msg ->
                _toastEvent.emit(msg)
            }
        }
    }

    fun scheduleDownload(id: Long, startTime: Long?, pauseTime: Long?) {
        viewModelScope.launch {
            repository.scheduleDownload(id, startTime, pauseTime)
        }
    }

    fun clearSchedule(id: Long) {
        viewModelScope.launch {
            repository.clearSchedule(id)
        }
    }

    fun setGlobalSchedule(enabled: Boolean, startH: Int, startM: Int, pauseH: Int, pauseM: Int) {
        repository.setGlobalSchedule(enabled, startH, startM, pauseH, pauseM)
    }

    val filteredDownloads: StateFlow<List<DownloadItem>> = combine(
        allDownloads,
        _selectedFilterTab,
        _searchQuery
    ) { list, filterTab, query ->
        list.filter { item ->
            val matchesTab = when (filterTab) {
                DownloadFilterTab.ACTIVE -> item.status == DownloadStatus.DOWNLOADING ||
                        item.status == DownloadStatus.PAUSED ||
                        item.status == DownloadStatus.QUEUED
                DownloadFilterTab.COMPLETED_FAILED -> item.status == DownloadStatus.COMPLETED ||
                        item.status == DownloadStatus.FAILED ||
                        item.status == DownloadStatus.CANCELLED
            }
            val matchesQuery = query.isBlank() ||
                    item.fileName.contains(query, ignoreCase = true) ||
                    item.url.contains(query, ignoreCase = true)
            matchesTab && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectFilterTab(tab: DownloadFilterTab) {
        _selectedFilterTab.value = tab
    }

    val overallSpeed: StateFlow<Long> = allDownloads.combine(isOnline) { list, online ->
        if (!online) 0L else list.filter { it.status == DownloadStatus.DOWNLOADING }.sumOf { it.speedBytesPerSec }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    val activeCount: StateFlow<Int> = allDownloads.combine(isOnline) { list, online ->
        if (!online) 0 else list.count { it.status == DownloadStatus.DOWNLOADING }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun selectCategory(category: FileType?) {
        _selectedCategory.value = category
    }

    fun selectStatus(status: DownloadStatus?) {
        _selectedStatus.value = status
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMaxConcurrent(limit: Int) {
        repository.setMaxConcurrentDownloads(limit)
    }

    fun probeUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _isProbing.value = true
            try {
                val result = repository.probeUrl(url.trim())
                _probeResult.value = result
            } catch (e: Exception) {
                _toastEvent.emit("Could not probe link: ${e.message}")
            } finally {
                _isProbing.value = false
            }
        }
    }

    fun clearProbeResult() {
        _probeResult.value = null
    }

    fun addDownload(
        url: String,
        fileName: String,
        fileType: FileType,
        totalBytes: Long = -1L,
        numSegments: Int = 1,
        autoStart: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                val cleanUrl = url.trim()
                if (cleanUrl.startsWith("magnet:?") || cleanUrl.endsWith(".torrent")) {
                    repository.addTorrent(cleanUrl, autoStart)
                    _toastEvent.emit("Torrent task added: $fileName")
                } else {
                    repository.addDownload(cleanUrl, fileName, fileType, totalBytes, numSegments, autoStart)
                    _toastEvent.emit("Download added: $fileName")
                }
            } catch (e: Exception) {
                _toastEvent.emit("Error adding download: ${e.message}")
            }
        }
    }

    fun addTorrent(rawInput: String, autoStart: Boolean = true) {
        viewModelScope.launch {
            try {
                repository.addTorrent(rawInput.trim(), autoStart)
                _toastEvent.emit("Torrent task queued")
            } catch (e: Exception) {
                _toastEvent.emit("Failed to add torrent: ${e.message}")
            }
        }
    }

    fun addBatchDownloads(urls: List<String>) {
        viewModelScope.launch {
            var added = 0
            for (u in urls) {
                val clean = u.trim()
                if (clean.isBlank()) continue
                try {
                    val probe = repository.probeUrl(clean)
                    repository.addDownload(clean, probe.fileName, probe.fileType, probe.totalBytes, 4, true)
                    added++
                } catch (_: Exception) {
                    val name = "download_${System.currentTimeMillis()}"
                    repository.addDownload(clean, name, FileType.OTHER, -1L, 1, true)
                    added++
                }
            }
            _toastEvent.emit("Added $added batch download tasks")
        }
    }

    fun pauseDownload(id: Long) = repository.pauseDownload(id)
    fun resumeDownload(id: Long) = repository.resumeDownload(id)
    fun restartDownload(id: Long) = repository.restartDownload(id)
    fun deleteDownload(id: Long, deleteFile: Boolean) = repository.deleteDownload(id, deleteFile)
    fun pauseAll() = repository.pauseAll()
    fun resumeAll() = repository.resumeAll()
    fun clearCompleted() = repository.clearCompleted()

    fun copyDownloadLink(context: Context, url: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Download Link", url)
        clipboard.setPrimaryClip(clip)
        viewModelScope.launch {
            _toastEvent.emit("Link copied to clipboard")
        }
    }

    fun openDownloadedFile(context: Context, item: DownloadItem) {
        if (item.fileType == FileType.VIDEO) {
            playWithMxPlayer(context, item)
            return
        }

        val file = File(item.savePath)
        if (!file.exists()) {
            viewModelScope.launch { _toastEvent.emit("File not found on device storage") }
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                clipData = android.content.ClipData.newRawUri("File", uri)
            }
            val chooser = Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri("File", uri)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            viewModelScope.launch {
                _toastEvent.emit("Could not open file: ${e.localizedMessage}")
            }
        }
    }

    fun playWithMxPlayer(context: Context, item: DownloadItem) {
        MxPlayerHelper.playWithMxPlayer(context, item)
    }

    fun shareDownloadedFile(context: Context, item: DownloadItem) {
        val file = File(item.savePath)
        if (!file.exists()) {
            viewModelScope.launch { _toastEvent.emit("File not found to share") }
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share file"))
        } catch (e: Exception) {
            viewModelScope.launch {
                _toastEvent.emit("Could not share file: ${e.localizedMessage}")
            }
        }
    }

    fun turnOffApp(context: Context) {
        viewModelScope.launch {
            pauseAll()
            try {
                DownloadService.stopService(context)
            } catch (_: Exception) {}
            if (context is android.app.Activity) {
                context.finishAffinity()
            }
        }
    }
}
