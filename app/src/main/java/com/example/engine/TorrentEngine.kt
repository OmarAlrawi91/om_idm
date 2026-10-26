package com.example.engine

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.FileType
import com.example.model.TorrentMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.libtorrent4j.Sha1Hash
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.swig.torrent_flags_t
import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Real BitTorrent engine backed by libtorrent4j (libtorrent / rasterbar).
 * Supports magnet links, .torrent URLs and local .torrent files with real DHT,
 * trackers, peer wire protocol, piece verification and resume.
 */
class TorrentEngine(
    private val context: Context,
    private val downloadEngine: DownloadEngine,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    private val bencodeParser = BencodeParser()

    companion object {
        private const val TAG = "TorrentEngine"
        private const val METADATA_TIMEOUT_MS = 3 * 60 * 1000L

        /** One shared libtorrent session for the whole app (DHT, trackers, listen port). */
        private val session: SessionManager by lazy {
            SessionManager().also { it.start() }
        }
    }

    /** Parses a local .torrent file */
    fun parseTorrentFile(file: File): TorrentMetadata = bencodeParser.parse(file.readBytes())

    /** Fetches a .torrent file from URL and parses it */
    suspend fun fetchAndParseTorrentUrl(url: String): Pair<TorrentMetadata, File> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to download torrent: HTTP ${response.code}")
            }
            val bytes = response.body?.bytes() ?: throw IllegalStateException("Empty torrent body")
            val metadata = bencodeParser.parse(bytes)
            val torrentFile = File(downloadEngine.getDownloadDirectory(FileType.TORRENT), "${metadata.name}.torrent")
            torrentFile.writeBytes(bytes)
            Pair(metadata, torrentFile)
        }
    }

    fun createDownloadItemFromMetadata(metadata: TorrentMetadata, sourceUrl: String): DownloadItem {
        val downloadDir = downloadEngine.getDownloadDirectory(FileType.TORRENT)
        return DownloadItem(
            url = sourceUrl,
            fileName = metadata.name,
            fileType = FileType.fromFileNameOrMime(metadata.name),
            totalBytes = metadata.totalSize,
            status = DownloadStatus.QUEUED,
            isResumable = true,
            savePath = File(downloadDir, metadata.name).absolutePath,
            mimeType = "application/x-bittorrent",
            isTorrent = true,
            torrentInfoHash = metadata.infoHash,
            torrentPiecesTotal = metadata.pieceCount.coerceAtLeast(1)
        )
    }

    /** Magnet: real size/name are unknown until metadata arrives from peers (DHT). */
    suspend fun createDownloadItemFromMagnet(magnetInfo: MagnetInfo, rawUri: String): DownloadItem = withContext(Dispatchers.IO) {
        val downloadDir = downloadEngine.getDownloadDirectory(FileType.TORRENT)
        val hash = magnetInfo.infoHash.lowercase()
        val name = magnetInfo.displayName.ifBlank { hash }
        DownloadItem(
            url = rawUri,
            fileName = name,
            fileType = FileType.fromFileNameOrMime(name),
            totalBytes = magnetInfo.exactSizeBytes,
            status = DownloadStatus.QUEUED,
            isResumable = true,
            savePath = File(downloadDir, name).absolutePath,
            mimeType = "application/x-bittorrent",
            isTorrent = true,
            torrentInfoHash = hash
        )
    }

    /**
     * Real torrent download through libtorrent4j. Cancelling the coroutine pauses the torrent
     * (files stay on disk, libtorrent re-checks and resumes on the next start).
     */
    suspend fun downloadTorrent(
        item: DownloadItem,
        onProgress: suspend (downloaded: Long, total: Long, speed: Long, eta: Long, piecesDone: Int, seeders: Int, leechers: Int) -> Unit,
        onStatusChange: suspend (status: DownloadStatus, error: String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        val saveDir = downloadEngine.getDownloadDirectory(FileType.TORRENT)
        val sm = session
        var handle: TorrentHandle? = null
        var finished = false
        var lastDone = 0L
        var lastTotal = item.totalBytes
        var lastPieces = 0
        var realName: String? = null

        try {
            val src = item.url
            var info: TorrentInfo? = null
            when {
                src.startsWith("magnet:?") -> sm.download(src, saveDir, torrent_flags_t())
                src.startsWith("http://") || src.startsWith("https://") -> {
                    val bytes = okHttpClient.newCall(Request.Builder().url(src).build()).execute().use { r ->
                        if (!r.isSuccessful) throw IllegalStateException("Torrent file HTTP ${r.code}")
                        r.body?.bytes() ?: throw IllegalStateException("Empty torrent body")
                    }
                    info = TorrentInfo.bdecode(bytes)
                    sm.download(info, saveDir)
                }
                src.startsWith("file:") -> {
                    info = TorrentInfo(File(URI(src)))
                    sm.download(info, saveDir)
                }
                else -> throw IllegalArgumentException("Unsupported torrent source")
            }

            val hash: Sha1Hash = info?.infoHash()
                ?: Sha1Hash(item.torrentInfoHash ?: throw IllegalStateException("Missing info-hash"))

            // add is asynchronous: wait until the handle is registered
            var tries = 0
            while (handle == null && tries < 50 && coroutineContext.isActive) {
                handle = sm.find(hash)
                if (handle == null) { delay(200); tries++ }
            }
            val h = handle ?: throw IllegalStateException("Torrent could not be added to session")

            val startedAt = System.currentTimeMillis()
            while (coroutineContext.isActive) {
                if (!h.isValid()) throw IllegalStateException("Torrent was removed from session")
                val st = h.status()
                val ti = h.torrentFile()

                if (ti != null && realName == null) realName = ti.name()
                if (ti == null && System.currentTimeMillis() - startedAt > METADATA_TIMEOUT_MS) {
                    onStatusChange(DownloadStatus.FAILED, "No peers found: could not fetch torrent metadata")
                    return@withContext
                }

                val total = when {
                    st.totalWanted() > 0 -> st.totalWanted()
                    ti != null -> ti.totalSize()
                    else -> lastTotal
                }
                val done = st.totalDone()
                val speed = st.downloadPayloadRate().toLong()
                val seeds = st.numSeeds()
                val leechers = (st.numPeers() - st.numSeeds()).coerceAtLeast(0)
                val piecesDone = st.numPieces()
                val eta = if (speed > 0 && total > done) (total - done) / speed else -1L

                lastDone = done; lastTotal = total; lastPieces = piecesDone
                onProgress(done, total, speed, eta, piecesDone, seeds, leechers)

                if (st.isFinished() || st.isSeeding()) { finished = true; break }
                delay(1000)
            }

            if (!coroutineContext.isActive) {
                withContext(NonCancellable) {
                    onProgress(lastDone, lastTotal, 0L, -1L, lastPieces, 0, 0)
                    onStatusChange(DownloadStatus.PAUSED, null)
                }
                return@withContext
            }
        } catch (e: Exception) {
            if (!coroutineContext.isActive) {
                withContext(NonCancellable) { onStatusChange(DownloadStatus.PAUSED, null) }
            } else {
                Log.e(TAG, "Torrent error: ${e.message}", e)
                onStatusChange(DownloadStatus.FAILED, e.localizedMessage ?: "Torrent error")
            }
            return@withContext
        } finally {
            // Stop (and stop seeding). Files are kept on disk.
            try { handle?.let { sm.remove(it) } } catch (_: Exception) {}
        }

        if (finished) {
            try {
                val real = File(saveDir, realName ?: item.fileName)
                MediaScannerConnection.scanFile(context, arrayOf(real.absolutePath), arrayOf(null), null)
            } catch (_: Exception) {}
            onProgress(lastTotal, lastTotal, 0L, 0L, lastPieces, 0, 0)
            onStatusChange(DownloadStatus.COMPLETED, null)
        }
    }
}
