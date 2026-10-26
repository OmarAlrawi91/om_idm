package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.example.ui.components.formatSpeed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.DownloadItem
import com.example.ui.components.AddDownloadDialog
import com.example.ui.components.DownloadFormatBottomSheet
import com.example.ui.components.ExitAppDialog
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.MediaPreviewDialog
import com.example.ui.components.OneUiFloatingNavigationBar
import com.example.ui.components.ScheduleDownloadDialog
import com.example.ui.theme.DarkThemePreset
import com.example.ui.components.SpeedIndicator
import com.example.ui.components.SpeedLimiterDialog
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TorrentScreen
import com.example.ui.theme.CoralError
import com.example.viewmodel.DownloadViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: DownloadViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableIntStateOf(0) } // 0: Downloads, 1: Torrents, 2: Browser, 3: Settings
    var browserTargetUrl by remember { mutableStateOf<String?>(null) }

    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val allDownloads by viewModel.allDownloads.collectAsStateWithLifecycle()
    val torrentDownloads by viewModel.torrentDownloads.collectAsStateWithLifecycle()
    val selectedFilterTab by viewModel.selectedFilterTab.collectAsStateWithLifecycle()
    val filteredDownloads by viewModel.filteredDownloads.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val overallSpeed by viewModel.overallSpeed.collectAsStateWithLifecycle()
    val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()
    val maxConcurrent by viewModel.maxConcurrentDownloads.collectAsStateWithLifecycle()
    val isOledMode by viewModel.isOledMode.collectAsStateWithLifecycle()
    val darkPreset by viewModel.darkThemePreset.collectAsStateWithLifecycle()
    val selectedAccent by viewModel.oneUiAccent.collectAsStateWithLifecycle()
    val globalSpeedLimit by viewModel.globalSpeedLimit.collectAsStateWithLifecycle()

    val globalScheduleEnabled by viewModel.globalScheduleEnabled.collectAsStateWithLifecycle()
    val globalStartHour by viewModel.globalStartHour.collectAsStateWithLifecycle()
    val globalStartMinute by viewModel.globalStartMinute.collectAsStateWithLifecycle()
    val globalPauseHour by viewModel.globalPauseHour.collectAsStateWithLifecycle()
    val globalPauseMinute by viewModel.globalPauseMinute.collectAsStateWithLifecycle()

    val isProbing by viewModel.isProbing.collectAsStateWithLifecycle()
    val probeResult by viewModel.probeResult.collectAsStateWithLifecycle()
    val formatModalUrl by viewModel.formatModalUrl.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSpeedLimiterDialog by remember { mutableStateOf(false) }
    var showExitAppDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var addDialogInitialUrl by remember { mutableStateOf("") }
    var selectedItemForDetails by remember { mutableStateOf<DownloadItem?>(null) }
    var selectedItemForPreview by remember { mutableStateOf<DownloadItem?>(null) }
    var selectedItemForSchedule by remember { mutableStateOf<DownloadItem?>(null) }

    // Handle toast events
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // BackHandler: return to Downloads tab if not on it
    BackHandler(enabled = currentTab != 0) {
        currentTab = 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )

                        // Top Right: Quick Theme Dropdown Pill + Idle/Active Indicator + Power Off Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Quick Theme Selector Dropdown Pill
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .clickable { showThemeMenu = true }
                                        .padding(horizontal = 7.dp, vertical = 4.dp)
                                        .testTag("theme_selector_dropdown")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(darkPreset.accent)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = darkPreset.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Theme",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showThemeMenu,
                                    onDismissRequest = { showThemeMenu = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    DarkThemePreset.values().forEach { presetItem ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(presetItem.accent)
                                                    )
                                                    Text(
                                                        text = presetItem.title,
                                                        fontWeight = if (darkPreset == presetItem) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (darkPreset == presetItem) presetItem.accent else MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.setDarkThemePreset(presetItem)
                                                showThemeMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Status / Idle Indicator (● Idle / ● Active (speed) / ● Offline)
                            val (statusText, statusColor) = when {
                                !isOnline -> Pair("● Offline", CoralError)
                                activeCount > 0 -> Pair("● Active (${formatSpeed(overallSpeed)})", MaterialTheme.colorScheme.primary)
                                else -> Pair("● Idle", Color(0xFF8E8E93))
                            }
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showSpeedLimiterDialog = true }
                                    .padding(horizontal = 5.dp, vertical = 4.dp)
                                    .testTag("top_status_indicator")
                            )

                            IconButton(
                                onClick = { showExitAppDialog = true },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("btn_topbar_power_off")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = "Turn off app and stop background service",
                                    tint = CoralError.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                OneUiFloatingNavigationBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    activeAccentColor = MaterialTheme.colorScheme.primary
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0 || currentTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = {
                        addDialogInitialUrl = ""
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Download") },
                    text = { Text(stringResource(R.string.add_download), fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("fab_add_download")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            when (currentTab) {
                0 -> DownloadsScreen(
                    downloads = filteredDownloads,
                    allDownloads = allDownloads,
                    selectedFilterTab = selectedFilterTab,
                    onSelectFilterTab = { viewModel.selectFilterTab(it) },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    onPause = { viewModel.pauseDownload(it) },
                    onResume = { viewModel.resumeDownload(it) },
                    onRestart = { viewModel.restartDownload(it) },
                    onDelete = { id, delFile -> viewModel.deleteDownload(id, delFile) },
                    onCopyLink = { viewModel.copyDownloadLink(context, it) },
                    onPauseAll = { viewModel.pauseAll() },
                    onResumeAll = { viewModel.resumeAll() },
                    onClearCompleted = { viewModel.clearCompleted() },
                    onOpen = { viewModel.openDownloadedFile(context, it) },
                    onShare = { viewModel.shareDownloadedFile(context, it) },
                    onShowDetails = { selectedItemForDetails = it },
                    onPreviewMedia = { selectedItemForPreview = it },
                    onSchedule = { selectedItemForSchedule = it },
                    onOpenAddDialog = {
                        addDialogInitialUrl = ""
                        showAddDialog = true
                    },
                    onSimulateSharedLink = {
                        viewModel.showFormatModal("https://sample-videos.com/video321/mp4/720/big_buck_bunny_720p_1mb.mp4")
                    },
                    onPlayWithMxPlayer = { viewModel.playWithMxPlayer(context, it) }
                )

                1 -> TorrentScreen(
                    torrentDownloads = torrentDownloads,
                    onAddTorrent = { viewModel.addTorrent(it) },
                    onPause = { viewModel.pauseDownload(it) },
                    onResume = { viewModel.resumeDownload(it) },
                    onRestart = { viewModel.restartDownload(it) },
                    onDelete = { id, delFile -> viewModel.deleteDownload(id, delFile) },
                    onCopyLink = { viewModel.copyDownloadLink(context, it) },
                    onOpen = { viewModel.openDownloadedFile(context, it) },
                    onShare = { viewModel.shareDownloadedFile(context, it) },
                    onShowDetails = { selectedItemForDetails = it },
                    onPreviewMedia = { selectedItemForPreview = it },
                    onSchedule = { selectedItemForSchedule = it },
                    onPlayWithMxPlayer = { viewModel.playWithMxPlayer(context, it) },
                    accentColor = MaterialTheme.colorScheme.primary
                )

                2 -> BrowserScreen(
                    initialUrl = browserTargetUrl,
                    onStartDownload = { url, fileName, fileType, totalBytes ->
                        viewModel.addDownload(url, fileName, fileType, totalBytes, 8, true)
                    },
                    onAddTorrent = { magnetOrUrl ->
                        viewModel.addTorrent(magnetOrUrl, true)
                    },
                    accentColor = MaterialTheme.colorScheme.primary
                )

                3 -> SettingsScreen(
                    maxConcurrentDownloads = maxConcurrent,
                    onMaxConcurrentChange = { viewModel.setMaxConcurrent(it) },
                    allDownloads = allDownloads,
                    darkThemePreset = darkPreset,
                    onSelectDarkPreset = { viewModel.setDarkThemePreset(it) },
                    isOledMode = isOledMode,
                    onOledModeChange = { viewModel.setOledMode(it) },
                    selectedAccent = selectedAccent,
                    onSelectAccent = { viewModel.setOneUiAccent(it) },
                    globalScheduleEnabled = globalScheduleEnabled,
                    globalStartHour = globalStartHour,
                    globalStartMinute = globalStartMinute,
                    globalPauseHour = globalPauseHour,
                    globalPauseMinute = globalPauseMinute,
                    onSetGlobalSchedule = { e, sh, sm, ph, pm -> viewModel.setGlobalSchedule(e, sh, sm, ph, pm) },
                    globalSpeedLimit = globalSpeedLimit,
                    onSetGlobalSpeedLimit = { viewModel.setGlobalSpeedLimit(it) },
                    onClearCompleted = { viewModel.clearCompleted() },
                    onTurnOffApp = { showExitAppDialog = true }
                )
            }
        }
    }

    // Add Download Dialog
    if (showAddDialog) {
        AddDownloadDialog(
            initialUrl = addDialogInitialUrl,
            isProbing = isProbing,
            probeResult = probeResult,
            onProbeUrl = { viewModel.probeUrl(it) },
            onClearProbe = { viewModel.clearProbeResult() },
            onAddDownload = { url, fileName, fileType, totalBytes, segments, autoStart ->
                viewModel.addDownload(url, fileName, fileType, totalBytes, segments, autoStart)
            },
            onAddBatch = { viewModel.addBatchDownloads(it) },
            onDismiss = { showAddDialog = false }
        )
    }

    // File Details Dialog
    selectedItemForDetails?.let { item ->
        FileDetailsDialog(
            item = item,
            onOpen = { viewModel.openDownloadedFile(context, it) },
            onPlayWithMxPlayer = { viewModel.playWithMxPlayer(context, it) },
            onShare = { viewModel.shareDownloadedFile(context, it) },
            onDismiss = { selectedItemForDetails = null }
        )
    }

    // Media Preview Dialog
    selectedItemForPreview?.let { item ->
        MediaPreviewDialog(
            item = item,
            onDismiss = { selectedItemForPreview = null }
        )
    }

    // Schedule Download Dialog
    selectedItemForSchedule?.let { item ->
        ScheduleDownloadDialog(
            item = item,
            onSaveSchedule = { id, start, pause ->
                viewModel.scheduleDownload(id, start, pause)
            },
            onClearSchedule = { id ->
                viewModel.clearSchedule(id)
            },
            onDismiss = { selectedItemForSchedule = null }
        )
    }

    // Speed Limiter Dialog
    if (showSpeedLimiterDialog) {
        SpeedLimiterDialog(
            currentLimitBytesPerSec = globalSpeedLimit,
            onSaveLimit = { viewModel.setGlobalSpeedLimit(it) },
            onDismiss = { showSpeedLimiterDialog = false }
        )
    }

    // Switch Off / Exit App Dialog
    if (showExitAppDialog) {
        ExitAppDialog(
            onConfirmExit = {
                showExitAppDialog = false
                viewModel.turnOffApp(context)
            },
            onDismiss = { showExitAppDialog = false }
        )
    }

    // Download Format Modal Bottom Sheet
    formatModalUrl?.let { url ->
        DownloadFormatBottomSheet(
            sourceUrl = url,
            onConfirmDownload = { finalUrl, fileName, fileType ->
                viewModel.addDownload(finalUrl, fileName, fileType, 0L, 8, true)
                viewModel.dismissFormatModal()
            },
            onDismiss = { viewModel.dismissFormatModal() }
        )
    }
}
