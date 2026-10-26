package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.FileType
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE fileType = :fileType ORDER BY createdAt DESC")
    fun getDownloadsByType(fileType: FileType): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE isTorrent = 1 ORDER BY createdAt DESC")
    fun getTorrentDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun getDownloadById(id: Long): Flow<DownloadItem?>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadByIdDirect(id: Long): DownloadItem?

    @Query("SELECT * FROM downloads WHERE status = 'DOWNLOADING' OR status = 'QUEUED' ORDER BY createdAt ASC")
    suspend fun getActiveOrQueuedDownloads(): List<DownloadItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DownloadItem): Long

    @Update
    suspend fun update(item: DownloadItem)

    @Delete
    suspend fun delete(item: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()

    @Query("UPDATE downloads SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, speedBytesPerSec = :speed, etaSeconds = :eta WHERE id = :id")
    suspend fun updateProgress(id: Long, downloadedBytes: Long, totalBytes: Long, speed: Long, eta: Long)

    @Query("UPDATE downloads SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, speedBytesPerSec = :speed, etaSeconds = :eta, segmentProgressJson = :segmentJson WHERE id = :id")
    suspend fun updateProgressWithSegments(id: Long, downloadedBytes: Long, totalBytes: Long, speed: Long, eta: Long, segmentJson: String)

    @Query("UPDATE downloads SET speedLimitBytesPerSec = :limit WHERE id = :id")
    suspend fun updateSpeedLimit(id: Long, limit: Long)

    @Query("UPDATE downloads SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DownloadStatus, error: String? = null)

    @Query("UPDATE downloads SET status = 'PAUSED', speedBytesPerSec = 0 WHERE status = 'DOWNLOADING'")
    suspend fun pauseAllDownloading()

    @Query("SELECT * FROM downloads WHERE isScheduled = 1 OR scheduledStartTime IS NOT NULL OR scheduledPauseTime IS NOT NULL")
    suspend fun getScheduledDownloads(): List<DownloadItem>
}
