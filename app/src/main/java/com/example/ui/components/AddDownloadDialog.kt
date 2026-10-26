package com.example.ui.components

import android.content.ClipboardManager
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.engine.DownloadEngine
import com.example.model.FileType
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.VividEmerald

@Composable
fun AddDownloadDialog(
    initialUrl: String = "",
    isProbing: Boolean,
    probeResult: DownloadEngine.ProbeResult?,
    onProbeUrl: (String) -> Unit,
    onClearProbe: () -> Unit,
    onAddDownload: (url: String, fileName: String, fileType: FileType, totalBytes: Long, segments: Int, autoStart: Boolean) -> Unit,
    onAddBatch: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Single, 1: Batch
    var urlText by remember { mutableStateOf(initialUrl) }
    var fileNameText by remember { mutableStateOf("") }
    var batchText by remember { mutableStateOf("") }
    var selectedSegments by remember { mutableIntStateOf(4) }
    var selectedType by remember { mutableStateOf(FileType.OTHER) }
    var detectedSize by remember { mutableStateOf(-1L) }
    var isResumableDetected by remember { mutableStateOf(true) }

    LaunchedEffect(probeResult) {
        if (probeResult != null) {
            fileNameText = probeResult.fileName
            selectedType = probeResult.fileType
            detectedSize = probeResult.totalBytes
            isResumableDetected = probeResult.isResumable
        }
    }

    // Auto probe if initialUrl provided
    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotBlank() && initialUrl.startsWith("http")) {
            onProbeUrl(initialUrl)
        }
    }

    Dialog(onDismissRequest = {
        onClearProbe()
        onDismiss()
    }) {
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
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberCyan.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Add Download",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    IconButton(
                        onClick = {
                            onClearProbe()
                            onDismiss()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: Single vs Batch
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Single Link") },
                        icon = { Icon(Icons.Default.Link, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Batch Links") },
                        icon = { Icon(Icons.Default.ListAlt, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Single URL Input
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = {
                            urlText = it
                            if (it.isBlank()) {
                                fileNameText = ""
                                onClearProbe()
                            }
                        },
                        label = { Text("URL or Magnet Link") },
                        placeholder = { Text("https://... or magnet:?xt=...") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_download_url"),
                        trailingIcon = {
                            Row {
                                if (isProbing) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.CenterVertically)
                                    )
                                } else {
                                    // Paste button
                                    IconButton(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                            if (clip.isNotBlank()) {
                                                urlText = clip
                                                if (clip.startsWith("http")) onProbeUrl(clip)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Probe Button
                    if (!urlText.startsWith("magnet:?") && urlText.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onProbeUrl(urlText) },
                            enabled = !isProbing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_probe_url")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isProbing) "Checking link..." else "Probe Link Details")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // File Name field
                    OutlinedTextField(
                        value = fileNameText,
                        onValueChange = { fileNameText = it },
                        label = { Text("File Name") },
                        placeholder = { Text("my_file.ext") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_file_name")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Probe info badge
                    if (probeResult != null || detectedSize > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = VividEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "Detected Size: ${formatBytes(detectedSize)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (isResumableDetected) "✓ Resumable download supported" else "⚠️ Continuous Range resume not advertised",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isResumableDetected) VividEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Multi-thread / Segments Selector
                    Text(
                        text = "Connection Acceleration (Threads)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val segmentOptions = listOf(1, 4, 8, 16, 32)
                        segmentOptions.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = selectedSegments == option,
                                onClick = { selectedSegments = option },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = segmentOptions.size)
                            ) {
                                Text("${option}x")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons: Add Paused vs Download Now
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (urlText.isNotBlank()) {
                                    val finalName = fileNameText.ifBlank { "download_${System.currentTimeMillis()}" }
                                    onAddDownload(urlText, finalName, selectedType, detectedSize, selectedSegments, false)
                                    onClearProbe()
                                    onDismiss()
                                }
                            },
                            enabled = urlText.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_add_paused")
                        ) {
                            Text("Queue (Paused)")
                        }

                        Button(
                            onClick = {
                                if (urlText.isNotBlank()) {
                                    val finalName = fileNameText.ifBlank { "download_${System.currentTimeMillis()}" }
                                    onAddDownload(urlText, finalName, selectedType, detectedSize, selectedSegments, true)
                                    onClearProbe()
                                    onDismiss()
                                }
                            },
                            enabled = urlText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_download_now")
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download", fontWeight = FontWeight.Bold)
                        }
                    }

                } else {
                    // Batch Mode
                    Text(
                        text = "Paste multiple download links (one per line):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = batchText,
                        onValueChange = { batchText = it },
                        placeholder = { Text("https://example.com/video1.mp4\nhttps://example.com/doc.pdf\nmagnet:?xt=...") },
                        minLines = 6,
                        maxLines = 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_batch_urls")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val lines = batchText.lines().map { it.trim() }.filter { it.isNotBlank() }
                            if (lines.isNotEmpty()) {
                                onAddBatch(lines)
                                onDismiss()
                            }
                        },
                        enabled = batchText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_add_batch")
                    ) {
                        Text("Add Batch to Queue", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
