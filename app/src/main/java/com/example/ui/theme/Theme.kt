package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

fun getSchemeForPreset(preset: DarkThemePreset, customAccent: Color? = null): ColorScheme {
    val activeAccent = customAccent ?: preset.accent
    val isLight = preset == DarkThemePreset.LIGHT_EMERALD

    return if (isLight) {
        lightColorScheme(
            primary = activeAccent,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEBF2EE),
            onPrimaryContainer = Color(0xFF0A3929),
            secondary = activeAccent,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFEBF2EE),
            onSecondaryContainer = Color(0xFF0A3929),
            tertiary = Color(0xFF0A3929),
            background = preset.background,
            onBackground = preset.textMain,
            surface = preset.surface,
            onSurface = preset.textMain,
            surfaceVariant = preset.surfaceVariant,
            onSurfaceVariant = preset.textMuted,
            outline = preset.border,
            error = CoralError,
            onError = Color.White
        )
    } else {
        darkColorScheme(
            primary = activeAccent,
            onPrimary = Color.White,
            primaryContainer = activeAccent.copy(alpha = 0.22f),
            onPrimaryContainer = Color.White,
            secondary = activeAccent,
            onSecondary = Color.White,
            secondaryContainer = activeAccent.copy(alpha = 0.16f),
            onSecondaryContainer = Color.White,
            tertiary = activeAccent,
            background = preset.background,
            onBackground = preset.textMain,
            surface = preset.surface,
            onSurface = preset.textMain,
            surfaceVariant = preset.surfaceVariant,
            onSurfaceVariant = preset.textMuted,
            outline = preset.border,
            error = CoralError,
            onError = Color.White
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    preset: DarkThemePreset = DarkThemePreset.OLED,
    oledMode: Boolean = false,
    accentColor: Color? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val activePreset = if (oledMode) DarkThemePreset.OLED else preset

    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getSchemeForPreset(activePreset, accentColor)
    }

    MaterialTheme(
        colorScheme = baseScheme,
        typography = Typography,
        content = content
    )
}
