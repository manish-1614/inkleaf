package com.inkleaf.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class ReaderThemeMode {
    LIGHT,
    DARK,
    SEPIA,
    SYSTEM
}

enum class ReaderThemePalette {
    LIGHT,
    DARK,
    PAPER
}

val ReaderThemeMode.displayLabel: String
    get() = when (this) {
        ReaderThemeMode.LIGHT -> "Light"
        ReaderThemeMode.DARK -> "Dark"
        ReaderThemeMode.SEPIA -> "Paper"
        ReaderThemeMode.SYSTEM -> "System"
    }

val ReaderThemeMode.subtitle: String
    get() = when (this) {
        ReaderThemeMode.LIGHT -> "Clean daylight"
        ReaderThemeMode.DARK -> "OLED midnight"
        ReaderThemeMode.SEPIA -> "Archival warm"
        ReaderThemeMode.SYSTEM -> "Auto adaptive"
    }

fun ReaderThemeMode.previewColors(isSystemDark: Boolean): Triple<Color, Color, Color> {
    return when (this) {
        ReaderThemeMode.SEPIA -> Triple(SepiaBackground, SepiaOnSurface, SepiaPrimary)
        ReaderThemeMode.LIGHT -> Triple(LightBackground, LightOnSurface, LightPrimary)
        ReaderThemeMode.DARK -> Triple(DarkBackground, DarkOnSurface, DarkPrimary)
        ReaderThemeMode.SYSTEM -> if (isSystemDark) {
            Triple(DarkBackground, DarkOnSurface, DarkPrimary)
        } else {
            Triple(LightBackground, LightOnSurface, LightPrimary)
        }
    }
}

fun parseReaderThemeMode(value: String?): ReaderThemeMode {
    return when (value?.uppercase()) {
        ReaderThemeMode.LIGHT.name -> ReaderThemeMode.LIGHT
        ReaderThemeMode.DARK.name -> ReaderThemeMode.DARK
        ReaderThemeMode.SEPIA.name, "PAPER" -> ReaderThemeMode.SEPIA
        ReaderThemeMode.SYSTEM.name -> ReaderThemeMode.SYSTEM
        else -> ReaderThemeMode.SEPIA
    }
}

fun ReaderThemeMode.resolvePalette(systemDark: Boolean): ReaderThemePalette {
    return when (this) {
        ReaderThemeMode.LIGHT -> ReaderThemePalette.LIGHT
        ReaderThemeMode.DARK -> ReaderThemePalette.DARK
        ReaderThemeMode.SEPIA -> ReaderThemePalette.PAPER
        ReaderThemeMode.SYSTEM -> if (systemDark) ReaderThemePalette.DARK else ReaderThemePalette.LIGHT
    }
}

// Light Reader Theme Colors ("Editorial Studio / Scandinavian Clean")
private val LightPrimary = Color(0xFF0F5B78)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFD6EFF8)
private val LightOnPrimaryContainer = Color(0xFF003548)
private val LightSecondary = Color(0xFF0F766E)
private val LightTertiary = Color(0xFF4338CA)
private val LightBackground = Color(0xFFFBFBFA)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF141923)
private val LightSurfaceVariant = Color(0xFFF1F4F6)
private val LightOnSurfaceVariant = Color(0xFF475569)
private val LightOutline = Color(0xFFCBD5E1)
private val LightOutlineVariant = Color(0xFFE2E8F0)

// Dark Reader Theme Colors ("OLED Midnight / Obsidian")
private val DarkPrimary = Color(0xFF38BDF8)
private val DarkOnPrimary = Color(0xFF082F49)
private val DarkPrimaryContainer = Color(0xFF0C4A6E)
private val DarkOnPrimaryContainer = Color(0xFFBAE6FD)
private val DarkSecondary = Color(0xFFA78BFA)
private val DarkTertiary = Color(0xFF34D399)
private val DarkBackground = Color(0xFF090D16)
private val DarkSurface = Color(0xFF131A26)
private val DarkOnSurface = Color(0xFFF1F5F9)
private val DarkSurfaceVariant = Color(0xFF1E2838)
private val DarkOnSurfaceVariant = Color(0xFF94A3B8)
private val DarkOutline = Color(0xFF334155)
private val DarkOutlineVariant = Color(0xFF1E293B)

// Sepia / Paper Reader Theme Colors ("Archival Book Paper / Warm Parchment")
private val SepiaPrimary = Color(0xFF8D3E1B)
private val SepiaOnPrimary = Color(0xFFFFFFFF)
private val SepiaPrimaryContainer = Color(0xFFEAD8C7)
private val SepiaOnPrimaryContainer = Color(0xFF431705)
private val SepiaSecondary = Color(0xFF6B4F3B)
private val SepiaTertiary = Color(0xFF825D2A)
private val SepiaBackground = Color(0xFFF7F3E9)
private val SepiaSurface = Color(0xFFEEE7DA)
private val SepiaOnSurface = Color(0xFF2C221A)
private val SepiaSurfaceVariant = Color(0xFFE4DAC9)
private val SepiaOnSurfaceVariant = Color(0xFF635243)
private val SepiaOutline = Color(0xFFD3C5B1)
private val SepiaOutlineVariant = Color(0xFFE2D6C4)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val SepiaColorScheme = lightColorScheme(
    primary = SepiaPrimary,
    onPrimary = SepiaOnPrimary,
    primaryContainer = SepiaPrimaryContainer,
    onPrimaryContainer = SepiaOnPrimaryContainer,
    secondary = SepiaSecondary,
    tertiary = SepiaTertiary,
    background = SepiaBackground,
    surface = SepiaSurface,
    onSurface = SepiaOnSurface,
    surfaceVariant = SepiaSurfaceVariant,
    onSurfaceVariant = SepiaOnSurfaceVariant,
    outline = SepiaOutline,
    outlineVariant = SepiaOutlineVariant
)

val InkleafTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.1).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
)

@Composable
fun InkleafTheme(
    readerThemeMode: ReaderThemeMode = ReaderThemeMode.SEPIA,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (readerThemeMode.resolvePalette(systemDark = darkTheme)) {
        ReaderThemePalette.PAPER -> SepiaColorScheme
        ReaderThemePalette.DARK -> DarkColorScheme
        ReaderThemePalette.LIGHT -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = InkleafTypography,
        content = content
    )
}
