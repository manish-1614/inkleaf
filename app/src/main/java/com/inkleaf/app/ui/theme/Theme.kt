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

// Light Reader Theme Colors ("Editorial Studio")
private val LightPrimary = Color(0xFF1E40AF)
private val LightSecondary = Color(0xFF0D9488)
private val LightTertiary = Color(0xFF4338CA)
private val LightBackground = Color(0xFFF8FAF9)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF0F172A)
private val LightSurfaceVariant = Color(0xFFF1F5F9)
private val LightOnSurfaceVariant = Color(0xFF334155)
private val LightOutlineVariant = Color(0xFFE2E8F0)

// Dark Reader Theme Colors ("OLED Midnight")
private val DarkPrimary = Color(0xFF38BDF8)
private val DarkSecondary = Color(0xFFA78BFA)
private val DarkTertiary = Color(0xFF34D399)
private val DarkBackground = Color(0xFF0B0F19)
private val DarkSurface = Color(0xFF151C2C)
private val DarkOnSurface = Color(0xFFF1F5F9)
private val DarkSurfaceVariant = Color(0xFF1E293B)
private val DarkOnSurfaceVariant = Color(0xFFCBD5E1)
private val DarkOutlineVariant = Color(0xFF334155)

// Sepia / Paper Reader Theme Colors ("Authentic Parchment")
private val SepiaPrimary = Color(0xFF9A3412)
private val SepiaSecondary = Color(0xFFB45309)
private val SepiaTertiary = Color(0xFF78350F)
private val SepiaBackground = Color(0xFFF5EFE6)
private val SepiaSurface = Color(0xFFEAE3D2)
private val SepiaOnSurface = Color(0xFF2C1E14)
private val SepiaSurfaceVariant = Color(0xFFDFD7C6)
private val SepiaOnSurfaceVariant = Color(0xFF4A3728)
private val SepiaOutlineVariant = Color(0xFFD6C7B2)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outlineVariant = LightOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outlineVariant = DarkOutlineVariant
)

private val SepiaColorScheme = lightColorScheme(
    primary = SepiaPrimary,
    secondary = SepiaSecondary,
    tertiary = SepiaTertiary,
    background = SepiaBackground,
    surface = SepiaSurface,
    onSurface = SepiaOnSurface,
    surfaceVariant = SepiaSurfaceVariant,
    onSurfaceVariant = SepiaOnSurfaceVariant,
    outlineVariant = SepiaOutlineVariant
)

val InkleafTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun InkleafTheme(
    readerThemeMode: ReaderThemeMode = ReaderThemeMode.LIGHT,
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
