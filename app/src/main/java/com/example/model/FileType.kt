package com.example.model

enum class FileType(val label: String) {
    VIDEO("Video"),
    AUDIO("Audio"),
    DOCUMENT("Document"),
    ARCHIVE("Archive"),
    TORRENT("Torrent"),
    OTHER("Other");

    companion object {
        fun fromFileNameOrMime(fileName: String, mimeType: String? = null): FileType {
            val lowerName = fileName.lowercase()
            val lowerMime = mimeType?.lowercase() ?: ""

            return when {
                lowerName.endsWith(".torrent") || lowerName.startsWith("magnet:") || lowerMime.contains("bittorrent") -> TORRENT
                lowerMime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") ||
                        lowerName.endsWith(".webm") || lowerName.endsWith(".avi") || lowerName.endsWith(".mov") ||
                        lowerName.endsWith(".flv") || lowerName.endsWith(".3gp") || lowerName.endsWith(".ts") -> VIDEO

                lowerMime.startsWith("audio/") || lowerName.endsWith(".mp3") || lowerName.endsWith(".m4a") ||
                        lowerName.endsWith(".flac") || lowerName.endsWith(".wav") || lowerName.endsWith(".aac") ||
                        lowerName.endsWith(".ogg") || lowerName.endsWith(".wma") -> AUDIO

                lowerMime.contains("pdf") || lowerMime.contains("word") || lowerMime.contains("excel") ||
                        lowerMime.contains("powerpoint") || lowerMime.startsWith("text/") ||
                        lowerName.endsWith(".pdf") || lowerName.endsWith(".doc") || lowerName.endsWith(".docx") ||
                        lowerName.endsWith(".txt") || lowerName.endsWith(".epub") || lowerName.endsWith(".xls") ||
                        lowerName.endsWith(".xlsx") || lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx") ||
                        lowerName.endsWith(".csv") || lowerName.endsWith(".json") || lowerName.endsWith(".md") -> DOCUMENT

                lowerMime.contains("zip") || lowerMime.contains("compressed") || lowerMime.contains("tar") ||
                        lowerName.endsWith(".zip") || lowerName.endsWith(".rar") || lowerName.endsWith(".7z") ||
                        lowerName.endsWith(".tar") || lowerName.endsWith(".gz") || lowerName.endsWith(".bz2") ||
                        lowerName.endsWith(".iso") || lowerName.endsWith(".apk") -> ARCHIVE

                else -> OTHER
            }
        }
    }
}
