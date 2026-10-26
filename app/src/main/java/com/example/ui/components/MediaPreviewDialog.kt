package com.example.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.DownloadItem
import com.example.model.FileType
import com.example.ui.theme.AudioColor
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.VideoColor
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun MediaPreviewDialog(
    item: DownloadItem,
    onDismiss: () -> Unit
) {
    val file = File(item.savePath)
    val isMedia = item.fileType == FileType.VIDEO || item.fileType == FileType.AUDIO

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
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Media Preview",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isMedia && file.exists()) {
                    AudioVideoPlayer(file = file, isVideo = item.fileType == FileType.VIDEO)
                } else if (file.exists() && item.fileType == FileType.DOCUMENT) {
                    // Text preview for small text/json/doc files
                    val previewText = remember {
                        try {
                            if (file.length() < 100 * 1024) {
                                file.readText().take(1500)
                            } else {
                                "File size (${formatBytes(file.length())}) is too large for plain text preview. Use 'Open File' to view in dedicated app."
                            }
                        } catch (e: Exception) {
                            "Binary document. Use 'Open File' to open with viewer."
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        )
                    }
                } else {
                    Text(
                        text = "File is not available on disk.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AudioVideoPlayer(file: File, isVideo: Boolean) {
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableIntStateOf(0) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(file) {
        val player = MediaPlayer()
        try {
            player.setDataSource(file.absolutePath)
            player.prepare()
            duration = player.duration
            mediaPlayer = player
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            player.stop()
            player.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    currentPosition = it.currentPosition
                }
            }
            delay(500)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Player visual placeholder
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isVideo) VideoColor.copy(alpha = 0.15f)
                        else AudioColor.copy(alpha = 0.15f)
                    )
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = if (isVideo) VideoColor else AudioColor,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Slider
            val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
            Slider(
                value = progress,
                onValueChange = { newFrac ->
                    val targetMs = (newFrac * duration).toInt()
                    mediaPlayer?.seekTo(targetMs)
                    currentPosition = targetMs
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Time stamps
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatMs(currentPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatMs(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        val newPos = (currentPosition - 10000).coerceAtLeast(0)
                        mediaPlayer?.seekTo(newPos)
                        currentPosition = newPos
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.FastRewind, contentDescription = "-10s")
                }

                Spacer(modifier = Modifier.width(16.dp))

                FilledIconButton(
                    onClick = {
                        mediaPlayer?.let {
                            if (it.isPlaying) {
                                it.pause()
                                isPlaying = false
                            } else {
                                it.start()
                                isPlaying = true
                            }
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = CyberCyan,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("btn_media_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = {
                        val newPos = (currentPosition + 10000).coerceAtMost(duration)
                        mediaPlayer?.seekTo(newPos)
                        currentPosition = newPos
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = "+10s")
                }
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val sec = ms / 1000
    val m = sec / 60
    val s = sec % 60
    return String.format("%02d:%02d", m, s)
}
