package com.example.ui.screens

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.ui.components.OneUiFloatingChip
import com.example.ui.components.formatBytes
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.DarkThemePreset
import com.example.ui.theme.OneUiAccent
import com.example.ui.theme.OneUiVelvetMint
import java.io.File

@Composable
fun SettingsScreen(
    maxConcurrentDownloads: Int,
    onMaxConcurrentChange: (Int) -> Unit,
    allDownloads: List<DownloadItem>,
    darkThemePreset: DarkThemePreset = DarkThemePreset.MIDNIGHT_VELVET,
    onSelectDarkPreset: (DarkThemePreset) -> Unit = {},
    isOledMode: Boolean = false,
    onOledModeChange: (Boolean) -> Unit = {},
    selectedAccent: OneUiAccent,
    onSelectAccent: (OneUiAccent) -> Unit,
    globalScheduleEnabled: Boolean,
    globalStartHour: Int,
    globalStartMinute: Int,
    globalPauseHour: Int,
    globalPauseMinute: Int,
    onSetGlobalSchedule: (enabled: Boolean, startH: Int, startM: Int, pauseH: Int, pauseM: Int) -> Unit,
    globalSpeedLimit: Long = 0L,
    onSetGlobalSpeedLimit: (Long) -> Unit = {},
    onClearCompleted: () -> Unit,
    onTurnOffApp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var autoResumeOnReconnect by remember { mutableStateOf(true) }

    val downloadDir = remember {
        val ext = context.getExternalFilesDir(null)
        if (ext != null) File(ext, "OM_IDM").absolutePath else File(context.filesDir, "OM_IDM").absolutePath
    }

    val totalDownloadedBytes = remember(allDownloads) {
        allDownloads.filter { it.status == DownloadStatus.COMPLETED }.sumOf { it.totalBytes.coerceAtLeast(0) }
    }

    val (freeSpaceBytes, totalSpaceBytes) = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val free = stat.availableBlocksLong * stat.blockSizeLong
            val total = stat.blockCountLong * stat.blockSizeLong
            Pair(free, total)
        } catch (_: Exception) {
            Pair(0L, 0L)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Concurrent Active Downloads Card (Cleaned Top Card)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
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
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text(
                                text = "Concurrent Active Downloads",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Maximum simultaneous active download streams",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Concurrency Limit: $maxConcurrentDownloads tasks",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Slider(
                        value = maxConcurrentDownloads.toFloat(),
                        onValueChange = { onMaxConcurrentChange(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        ),
                        modifier = Modifier.testTag("slider_concurrent_downloads")
                    )
                }
            }
        }

        // Auto-Sorted Folders & Storage Stats Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
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
                                .background(OneUiVelvetMint.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = OneUiVelvetMint)
                        }
                        Column {
                            Text(
                                text = "Auto-Folder Sorting & Storage",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Files are organized by type into Videos, Music, Documents, Torrents",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Base Directory: $downloadDir",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (totalSpaceBytes > 0) {
                        val usedSpace = totalSpaceBytes - freeSpaceBytes
                        val usedPercent = (usedSpace.toDouble() / totalSpaceBytes.toDouble()).toFloat().coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { usedPercent },
                            color = selectedAccent.color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Free Space: ${formatBytes(freeSpaceBytes)} / ${formatBytes(totalSpaceBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onClearCompleted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_clear_completed")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.clear_completed))
                    }
                }
            }
        }

        // Developer Credits & About Card (Clean, single English line)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, selectedAccent.color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(selectedAccent.color.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = selectedAccent.color)
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.developer_credit),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = selectedAccent.color
                            )
                            Text(
                                text = "OM IDM Lead Architect & Developer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.about_app_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Switch Off App & Daemon Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CoralError.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
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
                                .background(CoralError.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = CoralError)
                        }
                        Column {
                            Text(
                                text = "App Power & Background Service",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Turn off OM IDM and stop persistent background service",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onTurnOffApp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralError,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_settings_power_off")
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Switch Off OM IDM", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
