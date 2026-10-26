package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.DownloadItem
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.VividEmerald
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleDownloadDialog(
    item: DownloadItem,
    onSaveSchedule: (id: Long, startTime: Long?, pauseTime: Long?) -> Unit,
    onClearSchedule: (id: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val currentCalendar = remember { Calendar.getInstance() }

    // Start time toggle & controls
    var enableStartTime by remember {
        mutableStateOf(item.scheduledStartTime != null)
    }

    var startHour by remember {
        val cal = Calendar.getInstance()
        if (item.scheduledStartTime != null) cal.timeInMillis = item.scheduledStartTime
        else cal.add(Calendar.HOUR_OF_DAY, 1)
        mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY))
    }

    var startMinute by remember {
        val cal = Calendar.getInstance()
        if (item.scheduledStartTime != null) cal.timeInMillis = item.scheduledStartTime
        mutableIntStateOf((cal.get(Calendar.MINUTE) / 5) * 5)
    }

    // Pause time toggle & controls
    var enablePauseTime by remember {
        mutableStateOf(item.scheduledPauseTime != null)
    }

    var pauseHour by remember {
        val cal = Calendar.getInstance()
        if (item.scheduledPauseTime != null) cal.timeInMillis = item.scheduledPauseTime
        else cal.add(Calendar.HOUR_OF_DAY, 3)
        mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY))
    }

    var pauseMinute by remember {
        val cal = Calendar.getInstance()
        if (item.scheduledPauseTime != null) cal.timeInMillis = item.scheduledPauseTime
        mutableIntStateOf(0)
    }

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
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberCyan.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Schedule Download",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = item.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PresetChip(label = "+30m") {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.MINUTE, 30)
                        startHour = cal.get(Calendar.HOUR_OF_DAY)
                        startMinute = cal.get(Calendar.MINUTE)
                        enableStartTime = true
                    }
                    PresetChip(label = "+2 hrs") {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.HOUR_OF_DAY, 2)
                        startHour = cal.get(Calendar.HOUR_OF_DAY)
                        startMinute = 0
                        enableStartTime = true
                    }
                    PresetChip(label = "Tonight 2AM") {
                        startHour = 2
                        startMinute = 0
                        enableStartTime = true
                        pauseHour = 6
                        pauseMinute = 30
                        enablePauseTime = true
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Start Schedule Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (enableStartTime) CyberCyan.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                                Text("Auto-Start / Resume Time", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Switch(
                                checked = enableStartTime,
                                onCheckedChange = { enableStartTime = it },
                                modifier = Modifier.testTag("switch_enable_start_time")
                            )
                        }

                        if (enableStartTime) {
                            Spacer(modifier = Modifier.height(10.dp))
                            TimeSelectorRow(
                                hour = startHour,
                                minute = startMinute,
                                onHourChange = { startHour = it },
                                onMinuteChange = { startMinute = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pause Schedule Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (enablePauseTime) AmberWarning.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Pause, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
                                Text("Auto-Pause Time", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Switch(
                                checked = enablePauseTime,
                                onCheckedChange = { enablePauseTime = it },
                                modifier = Modifier.testTag("switch_enable_pause_time")
                            )
                        }

                        if (enablePauseTime) {
                            Spacer(modifier = Modifier.height(10.dp))
                            TimeSelectorRow(
                                hour = pauseHour,
                                minute = pauseMinute,
                                onHourChange = { pauseHour = it },
                                onMinuteChange = { pauseMinute = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (item.isScheduled || item.scheduledStartTime != null || item.scheduledPauseTime != null) {
                        OutlinedButton(
                            onClick = {
                                onClearSchedule(item.id)
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_clear_schedule")
                        ) {
                            Icon(Icons.Default.AlarmOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }

                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()

                            val calculatedStartTime = if (enableStartTime) {
                                val cal = Calendar.getInstance()
                                cal.set(Calendar.HOUR_OF_DAY, startHour)
                                cal.set(Calendar.MINUTE, startMinute)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                if (cal.timeInMillis <= now) {
                                    cal.add(Calendar.DAY_OF_YEAR, 1) // Next day
                                }
                                cal.timeInMillis
                            } else null

                            val calculatedPauseTime = if (enablePauseTime) {
                                val cal = Calendar.getInstance()
                                cal.set(Calendar.HOUR_OF_DAY, pauseHour)
                                cal.set(Calendar.MINUTE, pauseMinute)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                if (cal.timeInMillis <= now) {
                                    cal.add(Calendar.DAY_OF_YEAR, 1) // Next day
                                }
                                cal.timeInMillis
                            } else null

                            onSaveSchedule(item.id, calculatedStartTime, calculatedPauseTime)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_save_schedule")
                    ) {
                        Text("Save Schedule", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PresetChip(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
fun TimeSelectorRow(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberCyan.copy(alpha = 0.15f))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = String.format("%02d:%02d", hour, minute),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(text = "Hour: $hour", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = hour.toFloat(),
            onValueChange = { onHourChange(it.toInt()) },
            valueRange = 0f..23f,
            steps = 22,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Minute: $minute", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = minute.toFloat(),
            onValueChange = { onMinuteChange((it.toInt() / 5) * 5) },
            valueRange = 0f..55f,
            steps = 10,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
