package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan

/**
 * Modern floating pill chip inspired by Samsung One UI and Galaxy Store.
 * Features smooth spring animations, frosted glass translucent background,
 * subtle gradient borders, and active glow.
 */
@Composable
fun OneUiFloatingChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    count: Int? = null,
    icon: ImageVector? = null,
    accentColor: Color = CyberCyan,
    testTag: String? = null,
    modifier: Modifier = Modifier
) {
    val animatedBg by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip_bg"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip_border"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip_content"
    )

    val animatedElevation by animateDpAsState(
        targetValue = if (selected) 6.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip_elevation"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chip_scale"
    )

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .shadow(
                elevation = animatedElevation,
                shape = CircleShape,
                ambientColor = Color.Black,
                spotColor = if (selected) accentColor.copy(alpha = 0.35f) else Color.Transparent
            )
            .clip(CircleShape)
            .background(animatedBg)
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = if (selected) {
                        Brush.horizontalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.85f),
                                accentColor.copy(alpha = 0.45f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                animatedBorderColor,
                                animatedBorderColor.copy(alpha = 0.1f)
                            )
                        )
                    }
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = animatedContentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.1.sp
                ),
                color = animatedContentColor
            )

            if (count != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (selected) accentColor.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$count",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = animatedContentColor
                    )
                }
            }
        }
    }
}
