package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val fileName: String,
    val fileType: FileType,
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = -1L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val isResumable: Boolean = true,
    val numSegments: Int = 1,
    val savePath: String = "",
    val mimeType: String = "",
    val etag: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isTorrent: Boolean = false,
    val torrentInfoHash: String? = null,
    val torrentSeeders: Int = 0,
    val torrentLeechers: Int = 0,
    val torrentPiecesTotal: Int = 0,
    val torrentPiecesDone: Int = 0,
    val scheduledStartTime: Long? = null,
    val scheduledPauseTime: Long? = null,
    val isScheduled: Boolean = false,
    val speedLimitBytesPerSec: Long = 0L, // 0 = unlimited
    val segmentProgressJson: String = ""  // JSON or comma-separated thread segment offsets
) {
    val progress: Float
        get() = if (totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else if (status == DownloadStatus.COMPLETED) {
            1f
        } else {
            0f
        }

    val progressPercent: Int
        get() = (progress * 100).toInt()
}
