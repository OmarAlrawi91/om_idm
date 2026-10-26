package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.model.FileType
import com.example.ui.theme.ArchiveColor
import com.example.ui.theme.AudioColor
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DocumentColor
import com.example.ui.theme.OtherColor
import com.example.ui.theme.TorrentColor
import com.example.ui.theme.VideoColor

/**
 * Modern floating scrollable category filter chips inspired by Samsung One UI and Galaxy Store.
 */
@Composable
fun CategoryFilterChips(
    selectedCategory: FileType?,
    onSelectCategory: (FileType?) -> Unit,
    countsByType: Map<FileType, Int>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" Floating Chip
        val totalCount = countsByType.values.sum()
        OneUiFloatingChip(
            selected = selectedCategory == null,
            onClick = { onSelectCategory(null) },
            label = "All",
            count = totalCount,
            icon = Icons.Default.AllInclusive,
            accentColor = CyberCyan,
            testTag = "filter_chip_all"
        )

        // Type chips
        FileType.values().forEach { type ->
            val count = countsByType[type] ?: 0
            val chipColor = when (type) {
                FileType.VIDEO -> VideoColor
                FileType.AUDIO -> AudioColor
                FileType.DOCUMENT -> DocumentColor
                FileType.ARCHIVE -> ArchiveColor
                FileType.TORRENT -> TorrentColor
                FileType.OTHER -> OtherColor
            }
            val icon: ImageVector = when (type) {
                FileType.VIDEO -> Icons.Default.Videocam
                FileType.AUDIO -> Icons.Default.Audiotrack
                FileType.DOCUMENT -> Icons.Default.Description
                FileType.ARCHIVE -> Icons.Default.Archive
                FileType.TORRENT -> Icons.Default.Share
                FileType.OTHER -> Icons.Default.Folder
            }

            OneUiFloatingChip(
                selected = selectedCategory == type,
                onClick = {
                    if (selectedCategory == type) onSelectCategory(null) else onSelectCategory(type)
                },
                label = type.label,
                count = count,
                icon = icon,
                accentColor = chipColor,
                testTag = "filter_chip_${type.name.lowercase()}"
            )
        }
    }
}
