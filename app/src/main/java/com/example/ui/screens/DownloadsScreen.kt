package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.FileType
import com.example.ui.components.DownloadCard
import com.example.viewmodel.DownloadFilterTab

@Composable
fun DownloadsScreen(
    downloads: List<DownloadItem>,
    allDownloads: List<DownloadItem>,
    selectedFilterTab: DownloadFilterTab = DownloadFilterTab.ACTIVE,
    onSelectFilterTab: (DownloadFilterTab) -> Unit = {},
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onRestart: (Long) -> Unit,
    onDelete: (Long, Boolean) -> Unit,
    onCopyLink: (String) -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onClearCompleted: () -> Unit,
    onOpen: (DownloadItem) -> Unit,
    onShare: (DownloadItem) -> Unit,
    onShowDetails: (DownloadItem) -> Unit,
    onPreviewMedia: (DownloadItem) -> Unit,
    onSchedule: (DownloadItem) -> Unit,
    onOpenAddDialog: () -> Unit,
    onSimulateSharedLink: () -> Unit = {},
    onPlayWithMxPlayer: ((DownloadItem) -> Unit)? = null,
    selectedCategory: FileType? = null,
    onSelectCategory: (FileType?) -> Unit = {},
    selectedStatus: DownloadStatus? = null,
    onSelectStatus: (DownloadStatus?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeCount = remember(allDownloads) {
        allDownloads.count {
            it.status == DownloadStatus.DOWNLOADING ||
            it.status == DownloadStatus.PAUSED ||
            it.status == DownloadStatus.QUEUED
        }
    }

    val completedFailedCount = remember(allDownloads) {
        allDownloads.count {
            it.status == DownloadStatus.COMPLETED ||
            it.status == DownloadStatus.FAILED ||
            it.status == DownloadStatus.CANCELLED
        }
    }

    val downloadingCount = remember(allDownloads) {
        allDownloads.count { it.status == DownloadStatus.DOWNLOADING }
    }
    val pausedCount = remember(allDownloads) {
        allDownloads.count { it.status == DownloadStatus.PAUSED }
    }
    val completedCount = remember(allDownloads) {
        allDownloads.count { it.status == DownloadStatus.COMPLETED }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Search Bar (48dp height, #1C1C1E background, #6C6C70 hint, #FFFFFF text, start search icon)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1C1C1E))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF6C6C70),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_downloads_input"),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search downloads, URLs, or files...",
                                color = Color(0xFF6C6C70),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simplified Filters: Only Active & Done/Failed (40dp height, 20dp cornerRadius)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val isActiveSelected = selectedFilterTab == DownloadFilterTab.ACTIVE
            val isCompletedSelected = selectedFilterTab == DownloadFilterTab.COMPLETED_FAILED

            // Active Tab Button
            Button(
                onClick = { onSelectFilterTab(DownloadFilterTab.ACTIVE) },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActiveSelected) MaterialTheme.colorScheme.primary else Color(0xFF1C1C1E),
                    contentColor = if (isActiveSelected) Color.White else Color(0xFF8E8E93)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("filter_tab_active")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Active",
                        fontSize = 14.sp,
                        fontWeight = if (isActiveSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActiveSelected) Color.White else Color(0xFF8E8E93)
                    )
                    if (activeCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isActiveSelected) Color.White.copy(alpha = 0.25f) else Color(0xFF2C2C2E))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$activeCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActiveSelected) Color.White else Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }

            // Completed / Failed Tab Button
            Button(
                onClick = { onSelectFilterTab(DownloadFilterTab.COMPLETED_FAILED) },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompletedSelected) MaterialTheme.colorScheme.primary else Color(0xFF1C1C1E),
                    contentColor = if (isCompletedSelected) Color.White else Color(0xFF8E8E93)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("filter_tab_completed")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Completed / Failed",
                        fontSize = 14.sp,
                        fontWeight = if (isCompletedSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCompletedSelected) Color.White else Color(0xFF8E8E93)
                    )
                    if (completedFailedCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isCompletedSelected) Color.White.copy(alpha = 0.25f) else Color(0xFF2C2C2E))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$completedFailedCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompletedSelected) Color.White else Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }
        }

        // Minimal Bulk Quick Actions Toolbar
        if (selectedFilterTab == DownloadFilterTab.ACTIVE && (downloadingCount > 0 || pausedCount > 0)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (downloadingCount > 0) {
                    OutlinedButton(
                        onClick = onPauseAll,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_pause_all")
                    ) {
                        Icon(Icons.Default.PauseCircleOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause All", fontSize = 12.sp)
                    }
                }
                if (pausedCount > 0) {
                    OutlinedButton(
                        onClick = onResumeAll,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_resume_all")
                    ) {
                        Icon(Icons.Default.PlayCircleOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume All", fontSize = 12.sp)
                    }
                }
            }
        } else if (selectedFilterTab == DownloadFilterTab.COMPLETED_FAILED && completedCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onClearCompleted,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("btn_clear_completed")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Completed", fontSize = 12.sp)
                }
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Content Area / Empty State Placeholder
        if (downloads.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C1E))
                    ) {
                        Icon(
                            imageVector = if (selectedFilterTab == DownloadFilterTab.ACTIVE) Icons.Default.Download else Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching downloads"
                        else if (selectedFilterTab == DownloadFilterTab.ACTIVE) "No Active Downloads"
                        else "No Completed Downloads",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try a different search term"
                        else if (selectedFilterTab == DownloadFilterTab.ACTIVE) "Share any link from TikTok or Instagram to download automatically."
                        else "Finished and failed downloads will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93),
                        textAlign = TextAlign.Center
                    )

                    if (selectedFilterTab == DownloadFilterTab.ACTIVE && searchQuery.isEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onOpenAddDialog,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1C1C1E),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("btn_empty_add_download")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Download", fontSize = 13.sp)
                            }

                            Button(
                                onClick = onSimulateSharedLink,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("btn_simulate_share")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simulate Shared Link", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(downloads, key = { it.id }) { item ->
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
}
