package com.example.engine

import java.net.URLDecoder

data class MagnetInfo(
    val infoHash: String,
    val displayName: String,
    val trackers: List<String>,
    val exactSizeBytes: Long = -1L,
    val webseedUrl: String? = null,
    val torrentFileUrl: String? = null
)

object MagnetParser {
    fun parse(uriString: String): MagnetInfo? {
        if (!uriString.startsWith("magnet:?")) return null

        var infoHash = ""
        var displayName = ""
        val trackers = mutableListOf<String>()
        var exactSize = -1L
        var webseedUrl: String? = null
        var torrentFileUrl: String? = null

        val query = uriString.substring("magnet:?".length)
        val pairs = query.split("&")

        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx == -1) continue
            val key = pair.substring(0, idx)
            val rawValue = pair.substring(idx + 1)
            val value = try {
                URLDecoder.decode(rawValue, "UTF-8")
            } catch (_: Exception) {
                rawValue
            }

            when (key.lowercase()) {
                "xt" -> {
                    // urn:btih:<40-hex-or-32-base32>
                    if (value.startsWith("urn:btih:", ignoreCase = true)) {
                        infoHash = value.substring("urn:btih:".length).lowercase()
                    }
                }
                "dn" -> displayName = value
                "tr" -> trackers.add(value)
                "xl" -> exactSize = value.toLongOrNull() ?: -1L
                "ws" -> webseedUrl = value
                "xs" -> torrentFileUrl = value
                "as" -> if (webseedUrl == null) webseedUrl = value
            }
        }

        if (infoHash.isEmpty()) {
            return null
        }

        if (displayName.isEmpty()) {
            displayName = "Torrent_${infoHash.take(8)}"
        }

        return MagnetInfo(
            infoHash = infoHash,
            displayName = displayName,
            trackers = trackers,
            exactSizeBytes = exactSize,
            webseedUrl = webseedUrl,
            torrentFileUrl = torrentFileUrl
        )
    }
}
