package com.inkleaf.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeDocumentTabTest {
    @Test
    fun `home document tabs default to recent and expose favourites`() {
        assertEquals(HomeDocumentTab.RECENT, defaultHomeDocumentTab())
        assertEquals(listOf(HomeDocumentTab.RECENT, HomeDocumentTab.FAVOURITES), homeDocumentTabs())
    }

    @Test
    fun `home tab empty states are specific to selected tab`() {
        assertEquals("No recently opened documents.", HomeDocumentTab.RECENT.emptyStateText)
        assertEquals("No favourite documents yet.", HomeDocumentTab.FAVOURITES.emptyStateText)
    }
}
