package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Modern Eye-Comfort Palette (Soft, non-glare, gentle on the eyes in low light & night reading)
val OneUiPastelBlue = Color(0xFF3B82F6)       // Electric Blue (Pure OLED accent)
val OneUiSoftViolet = Color(0xFF8B5CF6)       // Deep Violet accent
val OneUiLavender = Color(0xFFA78BFA)         // Soft lavender pastel
val OneUiSereneIndigo = Color(0xFF6366F1)     // Calm serene indigo
val OneUiVelvetMint = Color(0xFF10B981)       // Emerald accent
val OneUiMutedRose = Color(0xFFF43F5E)        // Rose accent

val ComfortBlue = OneUiPastelBlue
val ComfortMint = OneUiVelvetMint
val ComfortAmber = Color(0xFFF59E0B)          // Warm amber
val ComfortCoral = Color(0xFFEF4444)          // Soft coral error
val ComfortTeal = Color(0xFF06B6D4)           // Cyber Cyan accent
val ComfortLavender = OneUiSoftViolet

// Primary brand colors
val CyberCyan = ComfortTeal
val ElectricBlue = OneUiPastelBlue
val NeonPurple = OneUiSoftViolet
val VividEmerald = OneUiVelvetMint
val AmberWarning = ComfortAmber
val CoralError = ComfortCoral

// Theme Constants
val MidnightVelvetBackground = Color(0xFF0B0813)
val MidnightVelvetSurface = Color(0xFF151022)
val MidnightVelvetSurfaceVariant = Color(0xFF211936)
val MidnightVelvetBorder = Color(0xFF312550)
val MidnightVelvetCard = Color(0xFF211936)
val MidnightVelvetTextPrimary = Color(0xFFF5F3FF)
val MidnightVelvetTextSecondary = Color(0xFFA78BFA)

val OledBlack = Color(0xFF000000)
val OledSurface = Color(0xFF121214)
val OledSurfaceVariant = Color(0xFF1C1C1E)
val OledBorder = Color(0xFF27272A)
val OledCardBorder = Color(0xFF27272A)

val DarkBackground = Color(0xFF090D16)
val DarkSurface = Color(0xFF111827)
val DarkSurfaceVariant = Color(0xFF1F2937)
val DarkBorder = Color(0xFF374151)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF71717A)

// Category Colors
val VideoColor = Color(0xFFF43F5E)
val AudioColor = Color(0xFFF59E0B)
val DocumentColor = Color(0xFF3B82F6)
val ArchiveColor = Color(0xFF10B981)
val TorrentColor = Color(0xFF06B6D4)
val OtherColor = Color(0xFF8B5CF6)

enum class OneUiAccent(val label: String, val color: Color, val lightVariant: Color) {
    CLEAN_BLUE("Pure Blue", Color(0xFF3B82F6), Color(0xFF60A5FA)),
    VELVET_VIOLET("Deep Violet", Color(0xFF8B5CF6), Color(0xFFA78BFA)),
    LAVENDER_PURPLE("Lavender", Color(0xFFA78BFA), Color(0xFFC4B5FD)),
    SERENE_INDIGO("Indigo", Color(0xFF6366F1), Color(0xFF818CF8)),
    SOFT_MINT("Emerald", Color(0xFF10B981), Color(0xFF34D399)),
    MUTED_ROSE("Cyber Cyan", Color(0xFF06B6D4), Color(0xFF22D3EE))
}

/**
 * 4 Themes & Palettes (Design System):
 * 1. Pure OLED: bg #000000, surface #121214, card #1C1C1E, accent #3B82F6, border #27272A
 * 2. Cyber Cyan: bg #090D16, surface #111827, card #1F2937, accent #06B6D4, border #374151
 * 3. Emerald Dark: bg #05130E, surface #0D211A, card #163329, accent #10B981, border #1F4D3E
 * 4. Deep Violet: bg #0B0813, surface #151022, card #211936, accent #8B5CF6, border #312550
 */
enum class DarkThemePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val card: Color,
    val accent: Color,
    val textMain: Color,
    val textMuted: Color,
    val border: Color
) {
    OLED(
        id = "oled",
        title = "Pure OLED",
        subtitle = "True pitch-black (#000000) with Electric Blue",
        background = Color(0xFF000000),
        surface = Color(0xFF121214),
        surfaceVariant = Color(0xFF1C1C1E),
        card = Color(0xFF1C1C1E),
        accent = Color(0xFF3B82F6),
        textMain = Color(0xFFFFFFFF),
        textMuted = Color(0xFF71717A),
        border = Color(0xFF27272A)
    ),
    CYBER(
        id = "cyber",
        title = "Cyber Cyan",
        subtitle = "Deep space navy with Cyan accent",
        background = Color(0xFF090D16),
        surface = Color(0xFF111827),
        surfaceVariant = Color(0xFF1F2937),
        card = Color(0xFF1F2937),
        accent = Color(0xFF06B6D4),
        textMain = Color(0xFFF9FAFB),
        textMuted = Color(0xFF9CA3AF),
        border = Color(0xFF374151)
    ),
    EMERALD(
        id = "emerald",
        title = "Emerald Dark",
        subtitle = "Deep forest night with Emerald accent",
        background = Color(0xFF05130E),
        surface = Color(0xFF0D211A),
        surfaceVariant = Color(0xFF163329),
        card = Color(0xFF163329),
        accent = Color(0xFF10B981),
        textMain = Color(0xFFECFDF5),
        textMuted = Color(0xFF6EE7B7),
        border = Color(0xFF1F4D3E)
    ),
    VIOLET(
        id = "violet",
        title = "Deep Violet",
        subtitle = "Midnight velvet with Vivid Purple",
        background = Color(0xFF0B0813),
        surface = Color(0xFF151022),
        surfaceVariant = Color(0xFF211936),
        card = Color(0xFF211936),
        accent = Color(0xFF8B5CF6),
        textMain = Color(0xFFF5F3FF),
        textMuted = Color(0xFFA78BFA),
        border = Color(0xFF312550)
    ),
    LIGHT_EMERALD(
        id = "light-emerald",
        title = "Light Emerald",
        subtitle = "Clean light surface with Forest Green accent",
        background = Color(0xFFF8F9FA),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFEBF2EE),
        card = Color(0xFFEBF2EE),
        accent = Color(0xFF0A3929),
        textMain = Color(0xFF111827),
        textMuted = Color(0xFF6B7280),
        border = Color(0xFFD1D5DB)
    );

    companion object {
        val OLED_BLACK = OLED
        val MIDNIGHT_VELVET = VIOLET
        val DEEP_NAVY = CYBER
    }
}
