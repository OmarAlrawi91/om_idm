package com.example.engine

import com.example.model.TorrentFileInfo
import com.example.model.TorrentMetadata
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.MessageDigest

class BencodeParser {

    sealed class BValue {
        data class BInt(val value: Long) : BValue()
        data class BString(val bytes: ByteArray) : BValue() {
            val text: String get() = String(bytes, Charsets.UTF_8)
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is BString) return false
                return bytes.contentEquals(other.bytes)
            }
            override fun hashCode(): Int = bytes.contentHashCode()
        }
        data class BList(val values: List<BValue>) : BValue()
        data class BDict(val map: Map<String, BValue>, val rawInfoBytes: ByteArray? = null) : BValue()
    }

    fun parse(bytes: ByteArray): TorrentMetadata {
        val stream = ByteArrayInputStream(bytes)
        val root = parseValue(stream, bytes) as? BValue.BDict
            ?: throw IllegalArgumentException("Root element is not a Bencode dictionary")

        val infoDict = root.map["info"] as? BValue.BDict
            ?: throw IllegalArgumentException("Torrent missing 'info' dictionary")

        // Compute info_hash (SHA-1 of the raw bencoded 'info' dictionary)
        val infoBytes = infoDict.rawInfoBytes ?: byteArrayOf()
        val infoHash = if (infoBytes.isNotEmpty()) {
            val sha1 = MessageDigest.getInstance("SHA-1")
            sha1.digest(infoBytes).joinToString("") { "%02x".format(it) }
        } else {
            "unknown"
        }

        val name = (infoDict.map["name"] as? BValue.BString)?.text ?: "Unnamed Torrent"
        val pieceLength = (infoDict.map["piece length"] as? BValue.BInt)?.value ?: 262144L
        val piecesBytes = (infoDict.map["pieces"] as? BValue.BString)?.bytes ?: byteArrayOf()
        val pieceCount = piecesBytes.size / 20

        val announce = (root.map["announce"] as? BValue.BString)?.text ?: ""
        val announceList = mutableListOf<String>()
        (root.map["announce-list"] as? BValue.BList)?.values?.forEach { item ->
            if (item is BValue.BList) {
                item.values.forEach { sub ->
                    if (sub is BValue.BString) announceList.add(sub.text)
                }
            } else if (item is BValue.BString) {
                announceList.add(item.text)
            }
        }
        if (announce.isNotEmpty() && !announceList.contains(announce)) {
            announceList.add(0, announce)
        }

        val files = mutableListOf<TorrentFileInfo>()
        var totalSize = 0L

        // Check if multi-file torrent
        val filesList = infoDict.map["files"] as? BValue.BList
        if (filesList != null) {
            for (f in filesList.values) {
                if (f is BValue.BDict) {
                    val len = (f.map["length"] as? BValue.BInt)?.value ?: 0L
                    val pathList = (f.map["path"] as? BValue.BList)?.values?.mapNotNull { (it as? BValue.BString)?.text }
                    val filePath = pathList?.joinToString("/") ?: "file"
                    files.add(TorrentFileInfo(path = filePath, length = len))
                    totalSize += len
                }
            }
        } else {
            // Single file
            val singleLen = (infoDict.map["length"] as? BValue.BInt)?.value ?: 0L
            files.add(TorrentFileInfo(path = name, length = singleLen))
            totalSize = singleLen
        }

        val comment = (root.map["comment"] as? BValue.BString)?.text
        val createdBy = (root.map["created by"] as? BValue.BString)?.text
        val creationDate = (root.map["creation date"] as? BValue.BInt)?.value

        return TorrentMetadata(
            name = name,
            infoHash = infoHash,
            totalSize = totalSize,
            pieceLength = pieceLength,
            pieceCount = pieceCount,
            announce = announce,
            announceList = announceList,
            files = files,
            comment = comment,
            createdBy = createdBy,
            creationDate = creationDate
        )
    }

    private fun parseValue(stream: InputStream, originalBytes: ByteArray): BValue {
        val b = stream.read()
        if (b == -1) throw IllegalArgumentException("Unexpected EOF")
        return when (b.toChar()) {
            'i' -> parseInteger(stream)
            'l' -> parseList(stream, originalBytes)
            'd' -> parseDict(stream, originalBytes)
            in '0'..'9' -> parseString(stream, b)
            else -> throw IllegalArgumentException("Invalid bencode character: ${b.toChar()}")
        }
    }

    private fun parseInteger(stream: InputStream): BValue.BInt {
        val sb = StringBuilder()
        while (true) {
            val c = stream.read()
            if (c == -1 || c.toChar() == 'e') break
            sb.append(c.toChar())
        }
        return BValue.BInt(sb.toString().toLong())
    }

    private fun parseString(stream: InputStream, firstDigit: Int): BValue.BString {
        val lenSb = StringBuilder()
        lenSb.append(firstDigit.toChar())
        while (true) {
            val c = stream.read()
            if (c == -1 || c.toChar() == ':') break
            lenSb.append(c.toChar())
        }
        val length = lenSb.toString().toInt()
        val buffer = ByteArray(length)
        var readTotal = 0
        while (readTotal < length) {
            val count = stream.read(buffer, readTotal, length - readTotal)
            if (count == -1) break
            readTotal += count
        }
        return BValue.BString(buffer)
    }

    private fun parseList(stream: InputStream, originalBytes: ByteArray): BValue.BList {
        val list = mutableListOf<BValue>()
        while (true) {
            stream.mark(1)
            val peek = stream.read()
            if (peek == -1 || peek.toChar() == 'e') break
            stream.reset()
            list.add(parseValue(stream, originalBytes))
        }
        return BValue.BList(list)
    }

    private fun parseDict(stream: InputStream, originalBytes: ByteArray): BValue.BDict {
        val map = mutableMapOf<String, BValue>()
        var rawInfoBytes: ByteArray? = null

        while (true) {
            stream.mark(1)
            val peek = stream.read()
            if (peek == -1 || peek.toChar() == 'e') break
            stream.reset()

            val key = (parseValue(stream, originalBytes) as BValue.BString).text

            if (key == "info") {
                // If next is 'd', capture raw bytes of this dictionary
                stream.mark(originalBytes.size)
                val start = getStreamPosition(stream, originalBytes)
                val value = parseValue(stream, originalBytes)
                val end = getStreamPosition(stream, originalBytes)
                if (start in originalBytes.indices && end <= originalBytes.size && end >= start) {
                    rawInfoBytes = originalBytes.copyOfRange(start, end)
                }
                map[key] = value
            } else {
                val value = parseValue(stream, originalBytes)
                map[key] = value
            }
        }
        return BValue.BDict(map, rawInfoBytes)
    }

    private fun getStreamPosition(stream: InputStream, originalBytes: ByteArray): Int {
        return if (stream is ByteArrayInputStream) {
            originalBytes.size - stream.available()
        } else {
            0
        }
    }
}
