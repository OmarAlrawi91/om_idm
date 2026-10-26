package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.TorrentColor

private data class NavItemData(
    val index: Int,
    val titleRes: Int,
    val fallbackTitle: String,
    val icon: ImageVector,
    val defaultAccent: Color,
    val testTag: String
)

/**
 * Modern floating pill-shaped bottom navigation bar inspired by Samsung One UI & Galaxy Store.
 * 4 Core Sections: Downloads, Torrents, Browser, Settings. (Grabber section deleted).
 */
@Composable
fun OneUiFloatingNavigationBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    activeAccentColor: Color = CyberCyan,
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            NavItemData(0, R.string.downloads_tab, "Downloads", Icons.Default.Download, CyberCyan, "nav_downloads"),
            NavItemData(1, R.string.torrents_tab, "Torrents", Icons.Default.Share, TorrentColor, "nav_torrents"),
            NavItemData(2, R.string.browser_tab, "Browser", Icons.Default.Language, CyberCyan, "nav_browser"),
            NavItemData(3, R.string.settings_tab, "Settings", Icons.Default.Settings, CyberCyan, "nav_settings")
        )
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 6.dp,
        shadowElevation = 18.dp,
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    activeAccentColor.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        ),
        modifier = modifier
            .testTag("bottom_nav_bar")
            .fillMaxWidth()
            .height(68.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                val isSelected = currentTab == item.index
                val itemAccent = if (isSelected) activeAccentColor else item.defaultAccent

                val animatedBg by animateColorAsState(
                    targetValue = if (isSelected) itemAccent.copy(alpha = 0.22f) else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "nav_item_bg"
                )

                val animatedBorder by animateColorAsState(
                    targetValue = if (isSelected) itemAccent.copy(alpha = 0.55f) else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "nav_item_border"
                )

                val animatedContentColor by animateColorAsState(
                    targetValue = if (isSelected) itemAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "nav_item_color"
                )

                val animatedScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1.0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "nav_item_scale"
                )

                val localizedTitle = stringResource(item.titleRes)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable { onTabSelected(item.index) }
                        .testTag(item.testTag)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Capsule-shaped indicator around active icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(animatedBg)
                                .border(
                                    width = 1.dp,
                                    color = animatedBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = localizedTitle,
                                tint = animatedContentColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = animatedScale
                                        scaleY = animatedScale
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = localizedTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 10.sp,
                                letterSpacing = 0.1.sp
                            ),
                            color = animatedContentColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
