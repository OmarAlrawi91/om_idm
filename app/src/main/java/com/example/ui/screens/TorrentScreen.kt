package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DownloadItem
import com.example.ui.components.DownloadCard
import com.example.ui.theme.TorrentColor
import java.io.File
import java.io.FileOutputStream

@Composable
fun TorrentScreen(
    torrentDownloads: List<DownloadItem>,
    onAddTorrent: (String) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onRestart: (Long) -> Unit,
    onDelete: (Long, Boolean) -> Unit,
    onCopyLink: (String) -> Unit,
    onOpen: (DownloadItem) -> Unit,
    onShare: (DownloadItem) -> Unit,
    onShowDetails: (DownloadItem) -> Unit,
    onPreviewMedia: (DownloadItem) -> Unit,
    onSchedule: (DownloadItem) -> Unit,
    onPlayWithMxPlayer: ((DownloadItem) -> Unit)? = null,
    accentColor: Color = TorrentColor,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var magnetInput by remember { mutableStateOf("") }

    // System document picker launcher for local .torrent files
    val torrentFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val tempFile = File(context.cacheDir, "picked_${System.currentTimeMillis()}.torrent")
                    FileOutputStream(tempFile).use { output ->
                        inputStream.copyTo(output)
                    }
                    inputStream.close()
                    onAddTorrent(tempFile.absolutePath)
                }
            } catch (_: Exception) {}
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Core Torrent File & Magnet Downloader Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = accentColor)
                        }
                        Column {
                            Text(
                                text = "Add Magnet or .Torrent",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "High-speed P2P torrent download manager",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = magnetInput,
                        onValueChange = { magnetInput = it },
                        placeholder = { Text("Paste magnet:?xt=... or .torrent URL") },
                        maxLines = 2,
                        trailingIcon = {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                if (clip.isNotBlank()) magnetInput = clip
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_torrent_magnet")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                if (magnetInput.isBlank()) {
                                    Toast.makeText(context, "Please paste a valid magnet link or .torrent URL", Toast.LENGTH_SHORT).show()
                                } else {
                                    onAddTorrent(magnetInput)
                                    magnetInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(46.dp)
                                .testTag("btn_add_torrent_magnet")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Torrent Download", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                torrentFilePicker.launch("*/*")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_pick_local_torrent")
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open .torrent")
                        }
                    }
                }
            }
        }

        // Active Torrents List Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${stringResource(R.string.torrent_my_tasks)} (${torrentDownloads.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }
        }

        if (torrentDownloads.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = accentColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.torrent_no_tasks),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Paste a magnet link or choose a .torrent file above to start downloading.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(torrentDownloads, key = { it.id }) { item ->
                DownloadCard(
                    item = item,
                    onPause = onPause,
                    onResume = onResume,
                    onRestart = onRestart,
                    onDelete = onDelete,
                    onOpen = onOpen,
                    onShare = onShare,
                    onCopyLink = onCopyLink,
                    onShowDetails = onShowDetails,
                    onPreviewMedia = onPreviewMedia,
                    onSchedule = onSchedule,
                    onPlayWithMxPlayer = onPlayWithMxPlayer
                )
            }
        }
    }
}
