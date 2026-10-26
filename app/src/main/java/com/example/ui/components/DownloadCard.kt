package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.FileType
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ArchiveColor
import com.example.ui.theme.AudioColor
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DocumentColor
import com.example.ui.theme.OtherColor
import com.example.ui.theme.TorrentColor
import com.example.ui.theme.VideoColor
import com.example.ui.theme.VividEmerald

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadCard(
    item: DownloadItem,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onRestart: (Long) -> Unit,
    onDelete: (Long, Boolean) -> Unit,
    onOpen: (DownloadItem) -> Unit,
    onShare: (DownloadItem) -> Unit,
    onCopyLink: (String) -> Unit,
    onShowDetails: (DownloadItem) -> Unit,
    onPreviewMedia: (DownloadItem) -> Unit,
    onSchedule: (DownloadItem) -> Unit,
    onPlayWithMxPlayer: ((DownloadItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var deleteFileFromDisk by remember { mutableStateOf(true) }
    var showSegmentsExpanded by remember { mutableStateOf(false) }

    val categoryColor = when (item.fileType) {
        FileType.VIDEO -> VideoColor
        FileType.AUDIO -> AudioColor
        FileType.DOCUMENT -> DocumentColor
        FileType.ARCHIVE -> ArchiveColor
        FileType.TORRENT -> TorrentColor
        FileType.OTHER -> OtherColor
    }

    val categoryIcon: ImageVector = when (item.fileType) {
        FileType.VIDEO -> Icons.Default.Videocam
        FileType.AUDIO -> Icons.Default.Audiotrack
        FileType.DOCUMENT -> Icons.Default.Description
        FileType.ARCHIVE -> Icons.Default.Archive
        FileType.TORRENT -> Icons.Default.Share
        FileType.OTHER -> Icons.Default.Folder
    }

    val animatedProgress by animateFloatAsState(
        targetValue = item.progress,
        label = "progress_anim"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when (item.status) {
                    DownloadStatus.DOWNLOADING -> CyberCyan.copy(alpha = 0.5f)
                    DownloadStatus.COMPLETED -> VividEmerald.copy(alpha = 0.35f)
                    DownloadStatus.FAILED -> CoralError.copy(alpha = 0.4f)
                    DownloadStatus.PAUSED -> AmberWarning.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { onShowDetails(item) },
                onLongClick = { showContextMenu = true }
            )
            .testTag("download_card_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Category Badge + File Name + Status Indicator + 3-Dot Overflow Context Menu Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(categoryColor.copy(alpha = 0.15f))
                        .border(1.dp, categoryColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = item.fileType.label,
                        tint = categoryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // File Name & Category Tag
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.fileType.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = categoryColor
                        )
                        if (item.isTorrent) {
                            Text(
                                text = "• BITTORRENT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TorrentColor
                            )
                        } else if (item.numSegments > 1) {
                            Text(
                                text = "• ${item.numSegments}X THREADS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = CyberCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Status Indicator
                val isTorrentConnecting = item.isTorrent && item.status == DownloadStatus.DOWNLOADING && item.downloadedBytes == 0L
                DownloadStatusIndicator(
                    status = item.status,
                    customText = if (isTorrentConnecting) "CONNECTING P2P..." else null
                )

                Spacer(modifier = Modifier.width(2.dp))

                // Context Menu Overflow Button
                Box {
                    IconButton(
                        onClick = { showContextMenu = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_context_menu_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Context Menu Dropdown
                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false },
                        modifier = Modifier.testTag("dropdown_menu_${item.id}")
                    ) {
                        // Pause action
                        if (item.status == DownloadStatus.DOWNLOADING) {
                            DropdownMenuItem(
                                text = { Text("Pause Download") },
                                leadingIcon = { Icon(Icons.Default.Pause, contentDescription = null, tint = AmberWarning) },
                                onClick = {
                                    showContextMenu = false
                                    onPause(item.id)
                                },
                                modifier = Modifier.testTag("context_pause_${item.id}")
                            )
                        }

                        // Resume action
                        if (item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.QUEUED || item.status == DownloadStatus.FAILED) {
                            DropdownMenuItem(
                                text = { Text(if (item.status == DownloadStatus.FAILED) "Retry Download" else "Resume Download") },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyberCyan) },
                                onClick = {
                                    showContextMenu = false
                                    onResume(item.id)
                                },
                                modifier = Modifier.testTag("context_resume_${item.id}")
                            )
                        }

                        // Open File action
                        if (item.status == DownloadStatus.COMPLETED) {
                            DropdownMenuItem(
                                text = { Text("Open File") },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = VividEmerald) },
                                onClick = {
                                    showContextMenu = false
                                    onOpen(item)
                                },
                                modifier = Modifier.testTag("context_open_${item.id}")
                            )

                            // Media preview action
                            if (item.fileType == FileType.VIDEO || item.fileType == FileType.AUDIO || item.fileType == FileType.DOCUMENT) {
                                DropdownMenuItem(
                                    text = { Text("Preview In-App") },
                                    leadingIcon = { Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = categoryColor) },
                                    onClick = {
                                        showContextMenu = false
                                        onPreviewMedia(item)
                                    },
                                    modifier = Modifier.testTag("context_preview_${item.id}")
                                )

                                DropdownMenuItem(
                                    text = { Text("Play with MX Player") },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = VividEmerald) },
                                    onClick = {
                                        showContextMenu = false
                                        onPlayWithMxPlayer?.invoke(item)
                                    },
                                    modifier = Modifier.testTag("context_mx_player_${item.id}")
                                )
                            }

                            // Share File action
                            DropdownMenuItem(
                                text = { Text("Share File") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showContextMenu = false
                                    onShare(item)
                                },
                                modifier = Modifier.testTag("context_share_${item.id}")
                            )
                        }

                        // Copy link
                        DropdownMenuItem(
                            text = { Text("Copy Download Link") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showContextMenu = false
                                onCopyLink(item.url)
                            },
                            modifier = Modifier.testTag("context_copy_link_${item.id}")
                        )

                        // Restart from beginning
                        DropdownMenuItem(
                            text = { Text("Restart from Beginning") },
                            leadingIcon = { Icon(Icons.Default.Replay, contentDescription = null) },
                            onClick = {
                                showContextMenu = false
                                onRestart(item.id)
                            },
                            modifier = Modifier.testTag("context_restart_${item.id}")
                        )

                        // Schedule times
                        DropdownMenuItem(
                            text = { Text("Schedule Times...") },
                            leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null, tint = AmberWarning) },
                            onClick = {
                                showContextMenu = false
                                onSchedule(item)
                            },
                            modifier = Modifier.testTag("context_schedule_${item.id}")
                        )

                        // View details
                        DropdownMenuItem(
                            text = { Text("Details & Checksum") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                showContextMenu = false
                                onShowDetails(item)
                            },
                            modifier = Modifier.testTag("context_details_${item.id}")
                        )

                        HorizontalDivider()

                        // Delete action
                        DropdownMenuItem(
                            text = { Text("Delete...", color = CoralError) },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = CoralError) },
                            onClick = {
                                showContextMenu = false
                                showDeleteConfirmDialog = true
                            },
                            modifier = Modifier.testTag("context_delete_${item.id}")
                        )
                    }
                }
            }

            // Schedule Banner (if scheduled)
            if (item.isScheduled || item.scheduledStartTime != null || item.scheduledPauseTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmberWarning.copy(alpha = 0.12f))
                        .border(0.5.dp, AmberWarning.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .clickable { onSchedule(item) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("schedule_banner_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Schedule",
                        tint = AmberWarning,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = buildScheduleDescription(item.scheduledStartTime, item.scheduledPauseTime),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AmberWarning
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar Section
            if (item.status != DownloadStatus.COMPLETED) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("progress_bar_${item.id}"),
                        color = when (item.status) {
                            DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
                            DownloadStatus.PAUSED -> AmberWarning
                            DownloadStatus.FAILED -> CoralError
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Transfer Metrics Row: Transferred / Total (Percentage) and Real-time Speed / ETA
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val downloadedStr = formatBytes(item.downloadedBytes)
                val totalStr = if (item.totalBytes > 0) formatBytes(item.totalBytes) else "Unknown"

                Text(
                    text = if (item.status == DownloadStatus.COMPLETED) {
                        "Size: $totalStr • Complete"
                    } else if (item.isTorrent && item.status == DownloadStatus.DOWNLOADING && item.downloadedBytes == 0L) {
                        "Connecting P2P... ($totalStr)"
                    } else {
                        "$downloadedStr / $totalStr (${item.progressPercent}%)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Active speed & ETA
                if (item.status == DownloadStatus.DOWNLOADING && item.speedBytesPerSec > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Speed",
                            tint = CyberCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatSpeed(item.speedBytesPerSec),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        )
                        if (item.etaSeconds > 0) {
                            Text(
                                text = "• ETA ${formatEta(item.etaSeconds)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Multi-Thread Dynamic Segment Visualizer
            if (item.numSegments > 1 && item.status != DownloadStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(6.dp))
                val segmentsList = remember(item.segmentProgressJson) {
                    if (item.segmentProgressJson.isNotBlank()) {
                        item.segmentProgressJson.split(";").mapNotNull { part ->
                            val tokens = part.split(":")
                            if (tokens.size == 2) {
                                val idx = tokens[0].toIntOrNull() ?: 0
                                val percent = tokens[1].replace("%", "").toIntOrNull() ?: 0
                                Pair(idx, percent)
                            } else null
                        }
                    } else emptyList()
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberCyan.copy(alpha = 0.08f))
                        .clickable { showSegmentsExpanded = !showSegmentsExpanded }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(13.dp))
                        Text(
                            text = "${item.numSegments}X Multi-Thread Acceleration",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = CyberCyan
                        )
                    }
                    Text(
                        text = if (showSegmentsExpanded) "Hide details ▲" else "Show threads (${segmentsList.size.ifZero(item.numSegments)}) ▼",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (showSegmentsExpanded && segmentsList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            .padding(6.dp)
                    ) {
                        segmentsList.forEach { (idx, pct) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Thread #${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(58.dp)
                                )
                                LinearProgressIndicator(
                                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(2.5.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = CyberCyan,
                                    modifier = Modifier.width(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            // BitTorrent Swarm Details (if torrent)
            if (item.isTorrent && item.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Seeds: ${item.torrentSeeders} | Peers: ${item.torrentLeechers}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VividEmerald
                    )
                    Text(
                        text = "Pieces: ${item.torrentPiecesDone} / ${item.torrentPiecesTotal}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TorrentColor
                    )
                }
            }

            // Error notice banner
            if (item.status == DownloadStatus.FAILED && !item.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CoralError.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = CoralError,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = item.errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = CoralError,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Quick Action Buttons (Touch Target >= 48dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                when (item.status) {
                    DownloadStatus.DOWNLOADING -> {
                        FilledTonalIconButton(
                            onClick = { onPause(item.id) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = AmberWarning.copy(alpha = 0.2f),
                                contentColor = AmberWarning
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_quick_pause_${item.id}")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_quick_cancel_${item.id}")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DownloadStatus.PAUSED, DownloadStatus.QUEUED -> {
                        FilledTonalIconButton(
                            onClick = { onResume(item.id) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = CyberCyan.copy(alpha = 0.2f),
                                contentColor = CyberCyan
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_quick_resume_${item.id}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_quick_delete_${item.id}")
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DownloadStatus.FAILED -> {
                        FilledTonalIconButton(
                            onClick = { onResume(item.id) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = CyberCyan.copy(alpha = 0.2f),
                                contentColor = CyberCyan
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_quick_retry_${item.id}")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_quick_delete_${item.id}")
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DownloadStatus.COMPLETED -> {
                        if (item.fileType == FileType.VIDEO || item.fileType == FileType.AUDIO) {
                            FilledTonalIconButton(
                                onClick = { onPlayWithMxPlayer?.invoke(item) },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = CyberCyan.copy(alpha = 0.2f),
                                    contentColor = CyberCyan
                                ),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_quick_mx_player_${item.id}")
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play with MX Player",
                                    tint = CyberCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (item.fileType == FileType.VIDEO || item.fileType == FileType.AUDIO || item.fileType == FileType.DOCUMENT) {
                            IconButton(
                                onClick = { onPreviewMedia(item) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_quick_preview_${item.id}")
                            ) {
                                Icon(
                                    Icons.Default.PlayCircleFilled,
                                    contentDescription = "Preview",
                                    tint = categoryColor
                                )
                            }
                        }

                        FilledTonalIconButton(
                            onClick = { onOpen(item) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = VividEmerald.copy(alpha = 0.2f),
                                contentColor = VividEmerald
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_quick_open_${item.id}")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Open")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { onShare(item) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_quick_share_${item.id}")
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Download?") },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to remove \"${item.fileName}\"?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = deleteFileFromDisk,
                            onCheckedChange = { deleteFileFromDisk = it },
                            modifier = Modifier.testTag("checkbox_delete_disk_file")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Also delete file from device storage",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(item.id, deleteFileFromDisk)
                    },
                    modifier = Modifier.testTag("btn_confirm_delete")
                ) {
                    Text("Delete", color = CoralError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.testTag("btn_cancel_delete")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Status indicator badge with pulsing dot animation for active state
 */
@Composable
fun DownloadStatusIndicator(status: DownloadStatus, customText: String? = null) {
    val (defaultText, color, icon) = when (status) {
        DownloadStatus.DOWNLOADING -> Triple("DOWNLOADING", CyberCyan, Icons.Default.Bolt)
        DownloadStatus.PAUSED -> Triple("PAUSED", AmberWarning, Icons.Default.Pause)
        DownloadStatus.QUEUED -> Triple("QUEUED", MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.HourglassEmpty)
        DownloadStatus.COMPLETED -> Triple("COMPLETED", VividEmerald, Icons.Default.CheckCircle)
        DownloadStatus.FAILED -> Triple("FAILED", CoralError, Icons.Default.ErrorOutline)
        DownloadStatus.CANCELLED -> Triple("CANCELLED", MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Close)
    }
    val text = customText ?: defaultText

    // Pulsing dot for active downloading
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp)
            .testTag("status_indicator_${status.name.lowercase()}")
    ) {
        if (status == DownloadStatus.DOWNLOADING) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = pulseAlpha))
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(11.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
    }
}

fun buildScheduleDescription(startTime: Long?, pauseTime: Long?): String {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    val now = System.currentTimeMillis()

    return when {
        startTime != null && pauseTime != null -> {
            "⏰ Auto-Starts at ${timeFormat.format(Date(startTime))} • Auto-Pauses at ${timeFormat.format(Date(pauseTime))}"
        }
        startTime != null -> {
            val diffHours = (startTime - now) / (1000 * 60 * 60)
            if (diffHours in 0..23) {
                "⏰ Auto-Starts today at ${timeFormat.format(Date(startTime))}"
            } else {
                "⏰ Auto-Starts on ${dateFormat.format(Date(startTime))}"
            }
        }
        pauseTime != null -> {
            "⏰ Auto-Pauses at ${timeFormat.format(Date(pauseTime))}"
        }
        else -> "⏰ Scheduled"
    }
}

fun Int.ifZero(defaultVal: Int): Int = if (this == 0) defaultVal else this

