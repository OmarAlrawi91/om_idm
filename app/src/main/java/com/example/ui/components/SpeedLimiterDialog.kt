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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.VividEmerald

data class SpeedPreset(val label: String, val bytesPerSec: Long)

@Composable
fun SpeedLimiterDialog(
    currentLimitBytesPerSec: Long,
    onSaveLimit: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = remember {
        listOf(
            SpeedPreset("Unlimited", 0L),
            SpeedPreset("500 KB/s", 500L * 1024),
            SpeedPreset("1 MB/s", 1024L * 1024),
            SpeedPreset("2 MB/s", 2L * 1024 * 1024),
            SpeedPreset("5 MB/s", 5L * 1024 * 1024),
            SpeedPreset("10 MB/s", 10L * 1024 * 1024)
        )
    }

    var selectedLimit by remember { mutableFloatStateOf(currentLimitBytesPerSec.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberCyan.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = CyberCyan)
                }
                Text("Global Download Speed Limiter", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Throttle total download throughput smoothly so other apps and web browsing have dedicated bandwidth.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Current Target Display Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (selectedLimit > 0) AmberWarning.copy(alpha = 0.4f) else CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text("Active Cap:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (selectedLimit <= 0f) "UNLIMITED (Max Speed)"
                            else formatBytes(selectedLimit.toLong()) + "/s",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selectedLimit > 0f) AmberWarning else VividEmerald
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Quick Presets:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))

                // Preset Grid
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.take(3).forEach { p ->
                        val isSelected = selectedLimit.toLong() == p.bytesPerSec
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberCyan.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .border(1.dp, if (isSelected) CyberCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable { selectedLimit = p.bytesPerSec.toFloat() }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = p.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.drop(3).forEach { p ->
                        val isSelected = selectedLimit.toLong() == p.bytesPerSec
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberCyan.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .border(1.dp, if (isSelected) CyberCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable { selectedLimit = p.bytesPerSec.toFloat() }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = p.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Fine slider (0 to 15 MB/s)
                val sliderMb = (selectedLimit / (1024f * 1024f)).coerceIn(0f, 15f)
                Text("Fine adjustment: ${if (sliderMb == 0f) "Unlimited" else String.format("%.1f MB/s", sliderMb)}", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = sliderMb,
                    onValueChange = { mb ->
                        selectedLimit = if (mb <= 0.1f) 0f else mb * 1024f * 1024f
                    },
                    valueRange = 0f..15f,
                    steps = 29,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slider_speed_limiter")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveLimit(selectedLimit.toLong())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("btn_save_speed_limit")
            ) {
                Text("Apply Limit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_speed_limit")
            ) {
                Text("Cancel")
            }
        }
    )
}
