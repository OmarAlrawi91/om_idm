package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.DownloadRepository
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.FileType
import com.example.util.MxPlayerHelper
import com.example.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var observerJob: Job? = null
    private lateinit var networkMonitor: NetworkMonitor

    companion object {
        const val CHANNEL_ID = "om_idm_downloads_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val ACTION_CANCEL = "com.example.service.ACTION_CANCEL"
        const val ACTION_PLAY_MX = "com.example.service.ACTION_PLAY_MX"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"

        fun startService(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        networkMonitor = NetworkMonitor(applicationContext)
        createNotificationChannel()

        val initialNotif = buildNotification(
            title = "OM IDM Ready",
            content = "Idle",
            hasActiveDownloads = false
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotif,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotif)
        }

        val repository = DownloadRepository.getInstance(applicationContext)
        observerJob = serviceScope.launch {
            repository.allDownloads.collectLatest { downloads ->
                val isOnline = networkMonitor.isConnected()

                val activeDownloads = downloads.filter { it.status == DownloadStatus.DOWNLOADING }
                val completedMedia = downloads.firstOrNull {
                    it.status == DownloadStatus.COMPLETED &&
                            (it.fileType == FileType.VIDEO || it.fileType == FileType.AUDIO) &&
                            it.completedAt != null &&
                            (System.currentTimeMillis() - it.completedAt < 300_000)
                }

                if (!isOnline && activeDownloads.isNotEmpty()) {
                    updateNotification(
                        title = "OM IDM • Offline / Paused",
                        content = "No internet connection. Active downloads paused.",
                        progress = 0,
                        isIndeterminate = false,
                        hasActiveDownloads = false,
                        subText = "Offline",
                        activeItem = null,
                        completedMedia = completedMedia
                    )
                    return@collectLatest
                }

                if (activeDownloads.isNotEmpty()) {
                    val totalSpeed = activeDownloads.sumOf { it.speedBytesPerSec }
                    val speedText = formatSpeed(totalSpeed)

                    if (activeDownloads.size == 1) {
                        val active = activeDownloads.first()
                        val downloadedStr = formatBytes(active.downloadedBytes)
                        val totalStr = if (active.totalBytes > 0) formatBytes(active.totalBytes) else "Unknown"
                        val percent = if (active.totalBytes > 0) {
                            ((active.downloadedBytes * 100) / active.totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            active.progressPercent.coerceIn(0, 100)
                        }
                        val isIndeterminate = active.totalBytes <= 0 && percent <= 0

                        val title = "Downloading: ${active.fileName}"
                        val content = "$downloadedStr / $totalStr ($percent%) • $speedText"
                        val subText = "$speedText • $percent%"

                        updateNotification(
                            title = title,
                            content = content,
                            progress = percent,
                            isIndeterminate = isIndeterminate,
                            hasActiveDownloads = true,
                            subText = subText,
                            activeItem = active,
                            completedMedia = completedMedia
                        )
                    } else {
                        // Multiple active downloads
                        val totalDownloaded = activeDownloads.sumOf { it.downloadedBytes }
                        val totalBytesKnown = activeDownloads.filter { it.totalBytes > 0 }.sumOf { it.totalBytes }
                        val overallPercent = if (totalBytesKnown > 0) {
                            ((totalDownloaded * 100) / totalBytesKnown).toInt().coerceIn(0, 100)
                        } else {
                            val avg = activeDownloads.map { it.progressPercent }.average()
                            avg.toInt().coerceIn(0, 100)
                        }

                        val downloadedStr = formatBytes(totalDownloaded)
                        val totalStr = if (totalBytesKnown > 0) formatBytes(totalBytesKnown) else "Unknown"
                        val title = "Downloading ${activeDownloads.size} files"
                        val firstFile = activeDownloads.first().fileName
                        val content = "$firstFile (+${activeDownloads.size - 1} more) • $downloadedStr / $totalStr"
                        val subText = "$speedText • $overallPercent%"

                        updateNotification(
                            title = title,
                            content = content,
                            progress = overallPercent,
                            isIndeterminate = false,
                            hasActiveDownloads = true,
                            subText = subText,
                            activeItem = activeDownloads.first(),
                            completedMedia = completedMedia
                        )
                    }
                } else {
                    val completedCount = downloads.count { it.status == DownloadStatus.COMPLETED }
                    val pausedCount = downloads.count { it.status == DownloadStatus.PAUSED }
                    val statusText = when {
                        !isOnline -> "Offline / Idle"
                        completedCount > 0 && pausedCount > 0 -> "$completedCount completed • $pausedCount paused"
                        completedCount > 0 -> "$completedCount downloads completed"
                        pausedCount > 0 -> "$pausedCount downloads paused"
                        else -> "Downloads are idle or completed"
                    }
                    updateNotification(
                        title = "OM IDM",
                        content = statusText,
                        progress = 0,
                        isIndeterminate = false,
                        hasActiveDownloads = false,
                        subText = if (!isOnline) "Offline" else null,
                        activeItem = null,
                        completedMedia = completedMedia
                    )
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val downloadId = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1L) ?: -1L
        val repository = DownloadRepository.getInstance(applicationContext)

        when (action) {
            ACTION_PAUSE -> {
                if (downloadId != -1L) {
                    repository.pauseDownload(downloadId)
                } else {
                    repository.pauseAll()
                }
            }
            ACTION_RESUME -> {
                if (downloadId != -1L) {
                    repository.resumeDownload(downloadId)
                } else {
                    repository.resumeAll()
                }
            }
            ACTION_CANCEL -> {
                if (downloadId != -1L) {
                    repository.deleteDownload(downloadId, deleteFile = false)
                } else {
                    repository.pauseAll()
                }
            }
            ACTION_PLAY_MX -> {
                if (downloadId != -1L) {
                    serviceScope.launch {
                        val item = repository.getDownloadByIdDirect(downloadId)
                        if (item != null) {
                            MxPlayerHelper.playWithMxPlayer(applicationContext, item)
                        }
                    }
                }
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OM IDM Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time download speed, progress and interactive actions"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        title: String,
        content: String,
        progress: Int = 0,
        isIndeterminate: Boolean = false,
        hasActiveDownloads: Boolean = false,
        subText: String? = null,
        activeItem: DownloadItem? = null,
        completedMedia: DownloadItem? = null
    ): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(hasActiveDownloads)
            .setOnlyAlertOnce(true)
            .setSilent(true)

        if (subText != null) {
            builder.setSubText(subText)
        }

        if (hasActiveDownloads) {
            builder.setProgress(100, progress.coerceIn(0, 100), isIndeterminate)

            // Quick Interactive Action: Pause
            val pauseIntent = Intent(this, DownloadService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_DOWNLOAD_ID, activeItem?.id ?: -1L)
            }
            val pausePendingIntent = PendingIntent.getService(
                this,
                101,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)

            // Quick Interactive Action: Cancel
            val cancelIntent = Intent(this, DownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_DOWNLOAD_ID, activeItem?.id ?: -1L)
            }
            val cancelPendingIntent = PendingIntent.getService(
                this,
                102,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
        } else {
            builder.setProgress(0, 0, false)

            // Quick Interactive Action: Resume All if idle/paused
            val resumeIntent = Intent(this, DownloadService::class.java).apply {
                action = ACTION_RESUME
                putExtra(EXTRA_DOWNLOAD_ID, -1L)
            }
            val resumePendingIntent = PendingIntent.getService(
                this,
                103,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume All", resumePendingIntent)
        }

        // MX Player direct action when media finishes
        if (completedMedia != null) {
            val mxIntent = Intent(this, DownloadService::class.java).apply {
                action = ACTION_PLAY_MX
                putExtra(EXTRA_DOWNLOAD_ID, completedMedia.id)
            }
            val mxPendingIntent = PendingIntent.getService(
                this,
                104,
                mxIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Play with MX Player", mxPendingIntent)
        }

        return builder.build()
    }

    private fun updateNotification(
        title: String,
        content: String,
        progress: Int = 0,
        isIndeterminate: Boolean = false,
        hasActiveDownloads: Boolean = false,
        subText: String? = null,
        activeItem: DownloadItem? = null,
        completedMedia: DownloadItem? = null
    ) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(
            NOTIFICATION_ID,
            buildNotification(
                title = title,
                content = content,
                progress = progress,
                isIndeterminate = isIndeterminate,
                hasActiveDownloads = hasActiveDownloads,
                subText = subText,
                activeItem = activeItem,
                completedMedia = completedMedia
            )
        )
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format(Locale.US, "%.2f MB/s", bytesPerSec / (1024f * 1024f))
            bytesPerSec >= 1024 -> String.format(Locale.US, "%.1f KB/s", bytesPerSec / 1024f)
            else -> "$bytesPerSec B/s"
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val group = digitGroups.coerceIn(0, units.size - 1)
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, group.toDouble()), units[group])
    }

    override fun onDestroy() {
        observerJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
