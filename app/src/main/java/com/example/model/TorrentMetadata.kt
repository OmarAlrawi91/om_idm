package com.example.model

data class TorrentFileInfo(
    val path: String,
    val length: Long
)

data class TorrentMetadata(
    val name: String,
    val infoHash: String,
    val totalSize: Long,
    val pieceLength: Long,
    val pieceCount: Int,
    val announce: String,
    val announceList: List<String> = emptyList(),
    val files: List<TorrentFileInfo> = emptyList(),
    val comment: String? = null,
    val createdBy: String? = null,
    val creationDate: Long? = null
)
