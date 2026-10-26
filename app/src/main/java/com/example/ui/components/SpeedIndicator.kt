package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun SpeedIndicator(
    overallSpeedBytesPerSec: Long,
    activeCount: Int,
    speedLimitBytesPerSec: Long = 0L,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isTransferring = overallSpeedBytesPerSec > 0 || activeCount > 0
    val speedText = formatSpeed(overallSpeedBytesPerSec)
    val hasLimit = speedLimitBytesPerSec > 0L

    val clickableMod = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else Modifier

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (hasLimit) AmberWarning.copy(alpha = 0.15f)
                else if (isTransferring) CyberCyan.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = 1.dp,
                color = if (hasLimit) AmberWarning.copy(alpha = 0.45f)
                else if (isTransferring) CyberCyan.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .then(clickableMod)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("speed_indicator_badge")
    ) {
        Icon(
            imageVector = if (hasLimit) Icons.Default.Speed else Icons.Default.Bolt,
            contentDescription = "Speed and Limiter",
            tint = if (hasLimit) AmberWarning else if (isTransferring) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = if (isTransferring) speedText else "Idle",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = if (hasLimit) AmberWarning else if (isTransferring) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hasLimit) {
            Text(
                text = "• CAP",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp
                ),
                color = AmberWarning
            )
        } else if (activeCount > 0) {
            Text(
                text = "($activeCount)",
                style = MaterialTheme.typography.labelSmall,
                color = VividEmerald
            )
        }
    }
}

fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec >= 1024 * 1024 -> String.format("%.2f MB/s", bytesPerSec / (1024f * 1024f))
        bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024f)
        else -> "$bytesPerSec B/s"
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "Unknown"
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024f * 1024f * 1024f))
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024f * 1024f))
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024f)
        else -> "$bytes B"
    }
}

fun formatEta(seconds: Long): String {
    if (seconds <= 0) return "--"
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}
