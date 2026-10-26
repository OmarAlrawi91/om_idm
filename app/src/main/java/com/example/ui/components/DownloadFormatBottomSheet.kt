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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileType

data class MediaFormatOption(
    val id: String,
    val title: String,
    val sizeText: String,
    val fileType: FileType,
    val extension: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadFormatBottomSheet(
    sourceUrl: String,
    onConfirmDownload: (String, String, FileType) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val musicOptions = remember {
        listOf(
            MediaFormatOption("mp3_classic", "Classic MP3", "315.2 KB", FileType.AUDIO, ".mp3"),
            MediaFormatOption("mp3_hq", "High Quality Audio (320kbps)", "1.8 MB", FileType.AUDIO, ".mp3")
        )
    }

    val videoOptions = remember {
        listOf(
            MediaFormatOption("mp4_720", "MP4 (720p HD)", "1.2 MB", FileType.VIDEO, ".mp4"),
            MediaFormatOption("mp4_1080", "MP4 (1080p FHD)", "4.8 MB", FileType.VIDEO, ".mp4")
        )
    }

    var selectedOption by remember { mutableStateOf(videoOptions[0]) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF3F3F46))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                text = "Download",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (sourceUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sourceUrl,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Music Section
            Text(
                text = "Music",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                color = Color(0xFFA1A1AA)
            )

            Spacer(modifier = Modifier.height(6.dp))

            musicOptions.forEach { opt ->
                val isSelected = selectedOption.id == opt.id
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { selectedOption = opt }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("option_${opt.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFA1A1AA),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = opt.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = opt.sizeText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = Color(0xFFA1A1AA)
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedOption = opt },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = Color(0xFF71717A)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Video Section
            Text(
                text = "Video",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                color = Color(0xFFA1A1AA)
            )

            Spacer(modifier = Modifier.height(6.dp))

            videoOptions.forEach { opt ->
                val isSelected = selectedOption.id == opt.id
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { selectedOption = opt }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("option_${opt.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFA1A1AA),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = opt.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = opt.sizeText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = Color(0xFFA1A1AA)
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedOption = opt },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = Color(0xFF71717A)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Start Download Button
            Button(
                onClick = {
                    val finalUrl = if (sourceUrl.isNotBlank()) sourceUrl else "https://sample-videos.com/video321/mp4/720/big_buck_bunny_720p_1mb.mp4"
                    val finalFileName = "OM_Media_${System.currentTimeMillis()}${selectedOption.extension}"
                    onConfirmDownload(finalUrl, finalFileName, selectedOption.fileType)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_start_download_modal")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Download",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
