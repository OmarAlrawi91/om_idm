package com.example.engine

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class DownloadEngine(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .cookieJar(object : CookieJar {
            private val cookieStore = java.util.concurrent.ConcurrentHashMap<String, List<Cookie>>()
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
            }
            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        })
        .build()
) {

    companion object {
        private const val TAG = "DownloadEngine"
        private const val BUFFER_SIZE = 64 * 1024 // 64 KB buffer
        private const val MIN_CHUNK_SIZE_FOR_MULTI_THREAD = 256 * 1024L // 256 KB
    }

    /**
     * Injects standard modern browser headers (User-Agent, Referer, Accept, Sec-Fetch)
     * to bypass HTTP 403 Forbidden errors across CDNs, social platforms, and media servers.
     */
    fun applyBrowserHeaders(builder: Request.Builder, url: String, fileType: FileType? = null): Request.Builder {
        val uri = Uri.parse(url)
        val host = uri.host?.lowercase() ?: ""
        val scheme = uri.scheme ?: "https"
        var referer = if (host.isNotBlank()) "$scheme://$host/" else "https://www.google.com/"
        var origin = if (host.isNotBlank()) "$scheme://$host" else "https://www.google.com"

        when {
            host.contains("tiktok") -> {
                referer = "https://www.tiktok.com/"
                origin = "https://www.tiktok.com"
            }
            host.contains("instagram") || host.contains("cdninstagram") -> {
                referer = "https://www.instagram.com/"
                origin = "https://www.instagram.com"
            }
            host.contains("twitter") || host.contains("twimg") || host.contains("x.com") -> {
                referer = "https://twitter.com/"
                origin = "https://twitter.com"
            }
            host.contains("facebook") || host.contains("fbcdn") -> {
                referer = "https://www.facebook.com/"
                origin = "https://www.facebook.com"
            }
            host.contains("youtube") || host.contains("googlevideo") -> {
                referer = "https://www.youtube.com/"
                origin = "https://www.youtube.com"
            }
        }

        return builder
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.6613.127 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,video/webm,video/mp4,video/*,audio/*,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Accept-Encoding", "identity")
            .header("Sec-Ch-Ua", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128\"")
            .header("Sec-Ch-Ua-Mobile", "?1")
            .header("Sec-Ch-Ua-Platform", "\"Android\"")
            .header("Sec-Fetch-Site", "cross-site")
            .header("Sec-Fetch-Mode", "no-cors")
            .header("Sec-Fetch-Dest", when (fileType) {
                FileType.VIDEO -> "video"
                FileType.AUDIO -> "audio"
                else -> "empty"
            })
            .header("Origin", origin)
            .header("Referer", referer)
            .header("Connection", "keep-alive")
    }

    data class ProbeResult(
        val fileName: String,
        val totalBytes: Long,
        val isResumable: Boolean,
        val mimeType: String,
        val etag: String?,
        val fileType: FileType
    )

    data class SegmentInfo(
        val threadIndex: Int,
        var startOffset: Long,
        var currentOffset: Long,
        var endOffset: Long
    ) {
        val totalBytes: Long get() = (endOffset - startOffset + 1).coerceAtLeast(0)
        val downloadedBytes: Long get() = (currentOffset - startOffset).coerceAtLeast(0)
        val progressPercent: Int get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
    }

    /**
     * Probes URL to detect file name, size, mime type, and resumability.
     */
    suspend fun probeUrl(url: String): ProbeResult = withContext(Dispatchers.IO) {
        val streamUrl = resolveStreamUrl(url, FileType.VIDEO)
        val headReqBuilder = Request.Builder().url(streamUrl).head()
        applyBrowserHeaders(headReqBuilder, streamUrl, FileType.VIDEO)
        val request = headReqBuilder.build()

        var response: Response? = null
        try {
            response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful && (response.code == 403 || response.code == 405)) {
                response.close()
                response = null
            }
        } catch (_: Exception) {
            response = null
        }

        if (response == null || !response.isSuccessful) {
            try {
                val getRequestBuilder = Request.Builder()
                    .url(streamUrl)
                    .header("Range", "bytes=0-0")
                applyBrowserHeaders(getRequestBuilder, streamUrl, FileType.VIDEO)
                response = okHttpClient.newCall(getRequestBuilder.build()).execute()
                if (response.code == 403 || response.code == 416) {
                    response.close()
                    val plainGet = Request.Builder().url(streamUrl)
                    applyBrowserHeaders(plainGet, streamUrl, FileType.VIDEO)
                    response = okHttpClient.newCall(plainGet.build()).execute()
                }
            } catch (fallbackEx: Exception) {
                Log.w(TAG, "Fallback probe failed: ${fallbackEx.message}")
            }
        }

        var detectedName = extractFileNameFromUrl(url)
        var totalBytes = -1L
        var isResumable = false
        var mimeType = "application/octet-stream"
        var etag: String? = null

        if (response != null && response.isSuccessful) {
            val contentDisposition = response.header("Content-Disposition")
            if (!contentDisposition.isNullOrBlank()) {
                val parsedName = parseFileNameFromContentDisposition(contentDisposition)
                if (parsedName.isNotEmpty()) {
                    detectedName = parsedName
                }
            }

            mimeType = response.header("Content-Type") ?: "application/octet-stream"
            etag = response.header("ETag")

            val acceptRanges = response.header("Accept-Ranges")
            val contentRange = response.header("Content-Range")
            isResumable = acceptRanges?.equals("bytes", ignoreCase = true) == true || contentRange != null

            val lenHeader = response.header("Content-Length")
            if (contentRange != null) {
                val totalStr = contentRange.substringAfterLast("/", "")
                totalBytes = totalStr.toLongOrNull() ?: -1L
            } else if (!lenHeader.isNullOrBlank()) {
                totalBytes = lenHeader.toLongOrNull() ?: -1L
            }
            response.close()
        }

        val fileType = FileType.fromFileNameOrMime(detectedName, mimeType)

        ProbeResult(
            fileName = sanitizeFileName(detectedName),
            totalBytes = totalBytes,
            isResumable = isResumable,
            mimeType = mimeType,
            etag = etag,
            fileType = fileType
        )
    }

    /**
     * Executes download supporting:
     * 1. Multi-threaded dynamic chunk acceleration (up to 32 threads with random-access file writing)
     * 2. Smooth token-bucket speed throttling
     * 3. Continuous pause and resume
     */
    suspend fun download(
        downloadItem: DownloadItem,
        speedLimiter: TokenBucketLimiter,
        onProgress: suspend (downloaded: Long, total: Long, speedBytesPerSec: Long, etaSeconds: Long, segmentJson: String) -> Unit,
        onStatusChange: suspend (status: DownloadStatus, error: String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        val networkMonitor = com.example.util.NetworkMonitor(context)
        if (!networkMonitor.isConnected()) {
            onStatusChange(DownloadStatus.PAUSED, "Offline / Paused (No Internet)")
            return@withContext
        }

        val downloadDir = getDownloadDirectory(downloadItem.fileType)
        val finalFile = File(downloadDir, downloadItem.fileName)
        val partFile = File(downloadDir, "${downloadItem.fileName}.part")

        if (finalFile.exists() && downloadItem.totalBytes > 0 && finalFile.length() == downloadItem.totalBytes) {
            onProgress(downloadItem.totalBytes, downloadItem.totalBytes, 0, 0, "")
            onStatusChange(DownloadStatus.COMPLETED, null)
            return@withContext
        }

        val totalBytes = downloadItem.totalBytes
        val threadCount = downloadItem.numSegments.coerceIn(1, 32)
        val useMultiThreading = threadCount > 1 && downloadItem.isResumable && totalBytes > MIN_CHUNK_SIZE_FOR_MULTI_THREAD

        try {
            if (useMultiThreading) {
                try {
                    executeMultiSegmentDownload(
                        downloadItem = downloadItem,
                        partFile = partFile,
                        finalFile = finalFile,
                        totalBytes = totalBytes,
                        threadCount = threadCount,
                        speedLimiter = speedLimiter,
                        onProgress = onProgress,
                        onStatusChange = onStatusChange
                    )
                } catch (multiEx: Exception) {
                    if (!coroutineContext.isActive) throw multiEx
                    Log.w(TAG, "Multi-segment acceleration failed (${multiEx.message}). Falling back to single stream.")
                    if (partFile.exists()) partFile.delete() // pre-allocated file is not valid resume data
                    executeSingleStreamDownload(
                        downloadItem = downloadItem,
                        partFile = partFile,
                        finalFile = finalFile,
                        speedLimiter = speedLimiter,
                        onProgress = onProgress,
                        onStatusChange = onStatusChange
                    )
                }
            } else {
                executeSingleStreamDownload(
                    downloadItem = downloadItem,
                    partFile = partFile,
                    finalFile = finalFile,
                    speedLimiter = speedLimiter,
                    onProgress = onProgress,
                    onStatusChange = onStatusChange
                )
            }
        } catch (e: Exception) {
            if (!coroutineContext.isActive) {
                onStatusChange(DownloadStatus.PAUSED, null)
            } else {
                Log.e(TAG, "Download error: ${e.message}", e)
                onStatusChange(DownloadStatus.FAILED, e.localizedMessage ?: "Network error")
            }
        }
    }

    /**
     * Accelerated Multi-Segment Parallel Download
     */
    private suspend fun executeMultiSegmentDownload(
        downloadItem: DownloadItem,
        partFile: File,
        finalFile: File,
        totalBytes: Long,
        threadCount: Int,
        speedLimiter: TokenBucketLimiter,
        onProgress: suspend (Long, Long, Long, Long, String) -> Unit,
        onStatusChange: suspend (DownloadStatus, String?) -> Unit
    ) = coroutineScope {
        // Pre-allocate part file (always fresh: segment offsets are not persisted)
        if (partFile.exists()) partFile.delete()
        RandomAccessFile(partFile, "rw").use { raf ->
            if (raf.length() < totalBytes) {
                raf.setLength(totalBytes)
            }
        }

        val chunkSize = totalBytes / threadCount
        val segments = (0 until threadCount).map { i ->
            val start = i * chunkSize
            val end = if (i == threadCount - 1) totalBytes - 1 else (start + chunkSize - 1)
            SegmentInfo(threadIndex = i, startOffset = start, currentOffset = start, endOffset = end)
        }.toMutableList()

        val totalDownloadedAtomic = AtomicLong(0L)
        val lastUpdateTimestampAtomic = AtomicLong(System.currentTimeMillis())
        val bytesSinceLastSpeedSampleAtomic = AtomicLong(0L)
        val progressLock = Any()

        // Supervisor scope for parallel segment downloads
        supervisorScope {
            val jobs = segments.map { segment ->
                async(Dispatchers.IO) {
                    val streamUrl = resolveStreamUrl(downloadItem.url, downloadItem.fileType)
                    val reqBuilder = Request.Builder()
                        .url(streamUrl)
                        .header("Range", "bytes=${segment.currentOffset}-${segment.endOffset}")
                    applyBrowserHeaders(reqBuilder, streamUrl, downloadItem.fileType)
                    val req = reqBuilder.build()

                    okHttpClient.newCall(req).execute().use { response ->
                        // Must be 206: a 200 means the server ignored Range and would corrupt the file
                        if (response.code != 206) {
                            throw RuntimeException("HTTP ${response.code} on segment ${segment.threadIndex} (server does not support ranges)")
                        }

                        val inStream = response.body?.byteStream() ?: throw RuntimeException("Empty body")
                        RandomAccessFile(partFile, "rw").use { raf ->
                            raf.seek(segment.currentOffset)
                            val buffer = ByteArray(BUFFER_SIZE)
                            var read: Int

                            while (coroutineContext.isActive && segment.currentOffset <= segment.endOffset) {
                                val remainingInSegment = segment.endOffset - segment.currentOffset + 1
                                val toRead = min(buffer.size.toLong(), remainingInSegment).toInt()
                                if (toRead <= 0) break

                                read = inStream.read(buffer, 0, toRead)
                                if (read == -1) break

                                // Speed limiter throttle
                                speedLimiter.acquire(read)

                                raf.seek(segment.currentOffset)
                                raf.write(buffer, 0, read)
                                segment.currentOffset += read
                                totalDownloadedAtomic.addAndGet(read.toLong())
                                bytesSinceLastSpeedSampleAtomic.addAndGet(read.toLong())

                                val now = System.currentTimeMillis()
                                if (now - lastUpdateTimestampAtomic.get() >= 350) {
                                    var speed = 0L
                                    var fire = false
                                    synchronized(progressLock) {
                                        val elapsed = now - lastUpdateTimestampAtomic.get()
                                        if (elapsed >= 350) {
                                            speed = (bytesSinceLastSpeedSampleAtomic.getAndSet(0L) * 1000L) / elapsed
                                            lastUpdateTimestampAtomic.set(now)
                                            fire = true
                                        }
                                    }
                                    if (fire) {
                                        val downloaded = totalDownloadedAtomic.get()
                                        val eta = if (speed > 0 && totalBytes > downloaded) (totalBytes - downloaded) / speed else -1L
                                        val segmentJson = segments.joinToString(";") { "${it.threadIndex}:${it.progressPercent}%" }
                                        onProgress(downloaded, totalBytes, speed, eta, segmentJson)
                                    }
                                }
                            }
                        }
                        if (coroutineContext.isActive && segment.currentOffset <= segment.endOffset) {
                            throw RuntimeException("Segment ${segment.threadIndex} ended early at ${segment.currentOffset}/${segment.endOffset}")
                        }
                    }
                }
            }

            jobs.awaitAll()
        }

        if (!coroutineContext.isActive) {
            onProgress(totalDownloadedAtomic.get(), totalBytes, 0L, -1L, "")
            onStatusChange(DownloadStatus.PAUSED, null)
            return@coroutineScope
        }

        // Rename part file to final file
        if (partFile.exists()) {
            if (finalFile.exists()) finalFile.delete()
            if (!partFile.renameTo(finalFile)) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }
        }

        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(finalFile.absolutePath),
                arrayOf(downloadItem.mimeType.ifBlank { null }),
                null
            )
        } catch (_: Exception) {}

        onProgress(totalBytes, totalBytes, 0L, 0L, "")
        onStatusChange(DownloadStatus.COMPLETED, null)
    }

    /**
     * Single Connection Continuous Range Download with Throttling
     */
    private suspend fun executeSingleStreamDownload(
        downloadItem: DownloadItem,
        partFile: File,
        finalFile: File,
        speedLimiter: TokenBucketLimiter,
        onProgress: suspend (Long, Long, Long, Long, String) -> Unit,
        onStatusChange: suspend (DownloadStatus, String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        var downloadedBytes = if (partFile.exists()) partFile.length() else 0L

        val streamUrl = resolveStreamUrl(downloadItem.url, downloadItem.fileType)
        val requestBuilder = Request.Builder().url(streamUrl)
        applyBrowserHeaders(requestBuilder, streamUrl, downloadItem.fileType)

        if (downloadedBytes > 0 && downloadItem.isResumable) {
            requestBuilder.header("Range", "bytes=$downloadedBytes-")
            if (!downloadItem.etag.isNullOrBlank()) {
                requestBuilder.header("If-Range", downloadItem.etag)
            }
        }

        var response = okHttpClient.newCall(requestBuilder.build()).execute()
        // If server returns HTTP 403 or 416 with Range, retry fresh stream without Range header
        if (response.code == 403 || response.code == 416) {
            response.close()
            Log.w(TAG, "Server returned HTTP ${response.code} with Range. Retrying stream without Range...")
            val fallbackBuilder = Request.Builder().url(streamUrl)
            applyBrowserHeaders(fallbackBuilder, streamUrl, downloadItem.fileType)
            downloadedBytes = 0L
            response = okHttpClient.newCall(fallbackBuilder.build()).execute()
        }

        if (!response.isSuccessful && response.code != 206) {
            onStatusChange(DownloadStatus.FAILED, "HTTP ${response.code}: ${response.message}")
            response.close()
            return@withContext
        }

        val body = response.body
        if (body == null) {
            response.close()
            onStatusChange(DownloadStatus.FAILED, "Empty response body")
            return@withContext
        }

        var totalBytes = downloadItem.totalBytes
        val appendMode: Boolean

        if (response.code == 206) {
            appendMode = true
            val contentRange = response.header("Content-Range")
            if (contentRange != null) {
                val totalFromHeader = contentRange.substringAfterLast("/").toLongOrNull()
                if (totalFromHeader != null && totalFromHeader > 0) totalBytes = totalFromHeader
            }
            if (totalBytes <= 0) totalBytes = downloadedBytes + body.contentLength()
        } else {
            appendMode = false
            downloadedBytes = 0L
            totalBytes = body.contentLength()
            if (partFile.exists()) partFile.delete()
        }

        val inputStream: InputStream = body.byteStream()
        val outputStream = FileOutputStream(partFile, appendMode)
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        var lastUpdateTimestamp = System.currentTimeMillis()
        var bytesReadSinceLastUpdate = 0L
        var currentSpeed = 0L

        try {
            while (coroutineContext.isActive) {
                bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break

                // Obey speed limiter
                speedLimiter.acquire(bytesRead)

                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                bytesReadSinceLastUpdate += bytesRead

                val now = System.currentTimeMillis()
                val elapsed = now - lastUpdateTimestamp
                if (elapsed >= 350) {
                    currentSpeed = (bytesReadSinceLastUpdate * 1000L) / elapsed
                    val eta = if (currentSpeed > 0 && totalBytes > downloadedBytes) {
                        (totalBytes - downloadedBytes) / currentSpeed
                    } else -1L

                    onProgress(downloadedBytes, totalBytes, currentSpeed, eta, "")
                    lastUpdateTimestamp = now
                    bytesReadSinceLastUpdate = 0L
                }
            }
            outputStream.flush()
        } finally {
            outputStream.close()
            inputStream.close()
            response.close()
        }

        if (!coroutineContext.isActive) {
            onProgress(downloadedBytes, totalBytes, 0, -1, "")
            onStatusChange(DownloadStatus.PAUSED, null)
            return@withContext
        }

        if (totalBytes > 0 && downloadedBytes != totalBytes) {
            onStatusChange(DownloadStatus.FAILED, "Incomplete download ($downloadedBytes / $totalBytes bytes)")
            return@withContext
        }

        if (partFile.exists()) {
            if (finalFile.exists()) finalFile.delete()
            val renamed = partFile.renameTo(finalFile)
            if (!renamed) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }
        }

        // Notify Android MediaStore so file is immediately available to MX Player & system
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(finalFile.absolutePath),
                arrayOf(downloadItem.mimeType.ifBlank { null }),
                null
            )
        } catch (_: Exception) {}

        onProgress(downloadedBytes, if (totalBytes > 0) totalBytes else downloadedBytes, 0, 0, "")
        onStatusChange(DownloadStatus.COMPLETED, null)
    }

    fun getDownloadDirectory(fileType: FileType? = null): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val candidatePublic = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && publicDownloads != null) {
            File(publicDownloads, "OM_IDM")
        } else null

        // Verify public storage is actually writable without EACCES / Scoped Storage crashes
        val isPublicWritable = candidatePublic != null && try {
            if (!candidatePublic.exists()) candidatePublic.mkdirs()
            val testFile = File(candidatePublic, ".write_test_${System.currentTimeMillis()}")
            if (testFile.createNewFile()) {
                testFile.delete()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }

        val baseDir = if (isPublicWritable && candidatePublic != null) {
            candidatePublic
        } else {
            val ext = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (ext != null) {
                File(ext, "OM_IDM")
            } else {
                File(context.filesDir, "OM_IDM")
            }
        }

        val targetDir = when (fileType) {
            FileType.VIDEO -> File(baseDir, "Videos")
            FileType.AUDIO -> File(baseDir, "Music")
            FileType.DOCUMENT -> File(baseDir, "Documents")
            FileType.ARCHIVE -> File(baseDir, "Archives")
            FileType.TORRENT -> File(baseDir, "Torrents")
            FileType.OTHER -> File(baseDir, "Others")
            null -> baseDir
        }
        if (!targetDir.exists()) targetDir.mkdirs()
        return targetDir
    }

    fun resolveStreamUrl(url: String, fileType: FileType = FileType.VIDEO): String = url.trim()

    private fun extractFileNameFromUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            val path = uri.lastPathSegment
            if (!path.isNullOrBlank()) URLDecoder.decode(path, "UTF-8") else "download_${System.currentTimeMillis()}"
        } catch (_: Exception) {
            "download_${System.currentTimeMillis()}"
        }
    }

    private fun parseFileNameFromContentDisposition(cd: String): String {
        return try {
            val utf8Match = Regex("filename\\*=(?:UTF-8|utf-8)''([^;]+)").find(cd)
            if (utf8Match != null) return URLDecoder.decode(utf8Match.groupValues[1], "UTF-8")
            val match = Regex("filename=\"?([^\";]+)\"?").find(cd)
            if (match != null) return match.groupValues[1].trim()
            ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "file_${System.currentTimeMillis()}" }
    }
}
