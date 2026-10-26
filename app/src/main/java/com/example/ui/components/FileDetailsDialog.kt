package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.FileType
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.VividEmerald
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileDetailsDialog(
    item: DownloadItem,
    onOpen: (DownloadItem) -> Unit,
    onShare: (DownloadItem) -> Unit,
    onPlayWithMxPlayer: ((DownloadItem) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var sha256Hash by remember { mutableStateOf<String?>(null) }
    var isCalculatingHash by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Download Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // File Name
                DetailField("File Name", item.fileName, allowCopy = true) {
                    clipboardManager.setText(AnnotatedString(item.fileName))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status & Category
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        DetailField("Status", item.status.name)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DetailField("Category", item.fileType.label)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Size & Transferred
                val sizeStr = if (item.totalBytes > 0) "${formatBytes(item.totalBytes)} (${item.totalBytes} bytes)" else "Unknown"
                DetailField("File Size", sizeStr)

                Spacer(modifier = Modifier.height(10.dp))

                val downloadedStr = "${formatBytes(item.downloadedBytes)} (${item.downloadedBytes} bytes)"
                DetailField("Downloaded", "$downloadedStr (${item.progressPercent}%)")

                Spacer(modifier = Modifier.height(10.dp))

                // Resumable & Acceleration
                DetailField(
                    "Resume Capability",
                    if (item.isResumable) "Supported (Continuous Range Resume)" else "Restart on Interrupt"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Created date
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                DetailField("Added At", dateFormat.format(Date(item.createdAt)))

                if (item.completedAt != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailField("Completed At", dateFormat.format(Date(item.completedAt)))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Source URL
                DetailField("Source URL", item.url, allowCopy = true) {
                    clipboardManager.setText(AnnotatedString(item.url))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Save Path
                DetailField("Saved Path", item.savePath, allowCopy = true) {
                    clipboardManager.setText(AnnotatedString(item.savePath))
                }

                // Torrent Specifics
                if (item.isTorrent && !item.torrentInfoHash.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailField("Info Hash (SHA-1)", item.torrentInfoHash, allowCopy = true) {
                        clipboardManager.setText(AnnotatedString(item.torrentInfoHash))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // File Hash / Checksum Verification (if file exists)
                val targetFile = File(item.savePath)
                if (targetFile.exists()) {
                    Text(
                        text = "File Integrity (Checksum)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (sha256Hash == null) {
                        OutlinedButton(
                            onClick = {
                                isCalculatingHash = true
                                scope.launch {
                                    val hash = withContext(Dispatchers.IO) {
                                        try {
                                            val md = MessageDigest.getInstance("SHA-256")
                                            FileInputStream(targetFile).use { fis ->
                                                val buffer = ByteArray(8192)
                                                var bytesRead: Int
                                                while (fis.read(buffer).also { bytesRead = it } != -1) {
                                                    md.update(buffer, 0, bytesRead)
                                                }
                                            }
                                            md.digest().joinToString("") { "%02x".format(it) }
                                        } catch (e: Exception) {
                                            "Error computing checksum"
                                        }
                                    }
                                    sha256Hash = hash
                                    isCalculatingHash = false
                                }
                            },
                            enabled = !isCalculatingHash,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isCalculatingHash) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Calculating SHA-256...")
                            } else {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Calculate SHA-256 Hash")
                            }
                        }
                    } else {
                        DetailField("SHA-256 Checksum", sha256Hash!!, allowCopy = true) {
                            clipboardManager.setText(AnnotatedString(sha256Hash!!))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Play with MX Player (for video/audio items)
                    if (item.fileType == FileType.VIDEO || item.fileType == FileType.AUDIO) {
                        Button(
                            onClick = { onPlayWithMxPlayer?.invoke(item) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = androidx.compose.ui.graphics.Color.Black
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_details_mx_player")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play with MX Player", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onOpen(item) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VividEmerald,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_details_open")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open File")
                        }

                        OutlinedButton(
                            onClick = { onShare(item) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_details_share")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailField(
    title: String,
    value: String,
    allowCopy: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (allowCopy && onCopy != null) {
                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = if (allowCopy) FontFamily.Monospace else FontFamily.Default,
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}
