package com.inkleaf.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderThemeModeTest {
    @Test
    fun `light and dark resolve independently from system setting`() {
        assertEquals(ReaderThemePalette.LIGHT, ReaderThemeMode.LIGHT.resolvePalette(systemDark = true))
        assertEquals(ReaderThemePalette.LIGHT, ReaderThemeMode.LIGHT.resolvePalette(systemDark = false))
        assertEquals(ReaderThemePalette.DARK, ReaderThemeMode.DARK.resolvePalette(systemDark = true))
        assertEquals(ReaderThemePalette.DARK, ReaderThemeMode.DARK.resolvePalette(systemDark = false))
    }

    @Test
    fun `paper is explicit and system follows system setting`() {
        assertEquals(ReaderThemePalette.PAPER, ReaderThemeMode.SEPIA.resolvePalette(systemDark = true))
        assertEquals(ReaderThemePalette.PAPER, ReaderThemeMode.SEPIA.resolvePalette(systemDark = false))
        assertEquals(ReaderThemePalette.DARK, ReaderThemeMode.SYSTEM.resolvePalette(systemDark = true))
        assertEquals(ReaderThemePalette.LIGHT, ReaderThemeMode.SYSTEM.resolvePalette(systemDark = false))
    }

    @Test
    fun `stored theme names remain compatible`() {
        assertEquals(ReaderThemeMode.LIGHT, parseReaderThemeMode("LIGHT"))
        assertEquals(ReaderThemeMode.DARK, parseReaderThemeMode("DARK"))
        assertEquals(ReaderThemeMode.SEPIA, parseReaderThemeMode("SEPIA"))
        assertEquals(ReaderThemeMode.SEPIA, parseReaderThemeMode("PAPER"))
        assertEquals(ReaderThemeMode.SYSTEM, parseReaderThemeMode("SYSTEM"))
        assertEquals(ReaderThemeMode.SEPIA, parseReaderThemeMode("UNKNOWN"))
        assertEquals(ReaderThemeMode.SEPIA, parseReaderThemeMode(null))
    }

    @Test
    fun `home labels expose the four requested reading surfaces`() {
        assertEquals("Light", ReaderThemeMode.LIGHT.displayLabel)
        assertEquals("Dark", ReaderThemeMode.DARK.displayLabel)
        assertEquals("Paper", ReaderThemeMode.SEPIA.displayLabel)
        assertEquals("System", ReaderThemeMode.SYSTEM.displayLabel)
    }
}
