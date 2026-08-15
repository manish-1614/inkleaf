package com.inkleaf.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.inkleaf.app.ui.theme.parseReaderThemeMode
import com.inkleaf.app.ui.theme.ReaderThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reader_preferences")

data class RecentItem(
    val uriString: String,
    val displayName: String,
    val timestamp: Long
)

data class FavoriteItem(
    val uriString: String,
    val displayName: String,
    val timestamp: Long
)

class ReaderPreferencesRepository(private val context: Context) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("reader_theme")
        private val RECENTS_KEY = stringPreferencesKey("recent_documents")
        private val FAVORITES_KEY = stringPreferencesKey("favorite_documents")
        private fun scrollOffsetKey(fingerprint: String) = intPreferencesKey("scroll_offset_$fingerprint")
        private fun lastHeadingKey(fingerprint: String) = stringPreferencesKey("last_heading_$fingerprint")
    }

    val themeFlow: Flow<ReaderThemeMode> = context.dataStore.data.map { preferences ->
        val modeStr = preferences[THEME_KEY] ?: ReaderThemeMode.SEPIA.name
        parseReaderThemeMode(modeStr)
    }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ReaderThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = mode.name
        }
    }

    // Recents list functions. Serialized format: uriString|displayName|timestamp\n...
    val recentsFlow: Flow<List<RecentItem>> = context.dataStore.data.map { preferences ->
        val raw = preferences[RECENTS_KEY] ?: ""
        if (raw.isEmpty()) emptyList()
        else raw.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 3) {
                RecentItem(parts[0], parts[1], parts[2].toLongOrNull() ?: 0L)
            } else null
        }.sortedByDescending { it.timestamp }
    }.distinctUntilChanged()

    suspend fun addRecentDocument(uriString: String, displayName: String) {
        context.dataStore.edit { preferences ->
            val currentRaw = preferences[RECENTS_KEY] ?: ""
            val items = currentRaw.split("\n").mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size >= 3) {
                    RecentItem(parts[0], parts[1], parts[2].toLongOrNull() ?: 0L)
                } else null
            }.filter { it.uriString != uriString }.toMutableList()

            items.add(RecentItem(uriString, displayName, System.currentTimeMillis()))
            
            // Limit to 20 recents
            val limited = items.sortedByDescending { it.timestamp }.take(20)
            preferences[RECENTS_KEY] = limited.joinToString("\n") { "${it.uriString}|${it.displayName}|${it.timestamp}" }
        }
    }

    suspend fun removeRecentDocument(uriString: String) {
        context.dataStore.edit { preferences ->
            val currentRaw = preferences[RECENTS_KEY] ?: ""
            val items = currentRaw.split("\n").mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size >= 3) {
                    RecentItem(parts[0], parts[1], parts[2].toLongOrNull() ?: 0L)
                } else null
            }.filter { it.uriString != uriString }
            preferences[RECENTS_KEY] = items.joinToString("\n") { "${it.uriString}|${it.displayName}|${it.timestamp}" }
        }
    }

    // Favorites list functions. Serialized format: uriString|displayName|timestamp\n...
    val favoritesFlow: Flow<List<FavoriteItem>> = context.dataStore.data.map { preferences ->
        val raw = preferences[FAVORITES_KEY] ?: ""
        if (raw.isEmpty()) emptyList()
        else raw.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 3) {
                FavoriteItem(parts[0], parts[1], parts[2].toLongOrNull() ?: 0L)
            } else null
        }.sortedByDescending { it.timestamp }
    }.distinctUntilChanged()

    suspend fun toggleFavorite(uriString: String, displayName: String) {
        context.dataStore.edit { preferences ->
            val currentRaw = preferences[FAVORITES_KEY] ?: ""
            val items = currentRaw.split("\n").mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size >= 3) {
                    FavoriteItem(parts[0], parts[1], parts[2].toLongOrNull() ?: 0L)
                } else null
            }.toMutableList()

            val existingIndex = items.indexOfFirst { it.uriString == uriString }
            if (existingIndex != -1) {
                items.removeAt(existingIndex)
            } else {
                items.add(FavoriteItem(uriString, displayName, System.currentTimeMillis()))
            }
            preferences[FAVORITES_KEY] = items.joinToString("\n") { "${it.uriString}|${it.displayName}|${it.timestamp}" }
        }
    }

    // Reading Position Per Document
    fun getScrollPosition(fingerprint: String): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[scrollOffsetKey(fingerprint)] ?: 0
    }.distinctUntilChanged()

    suspend fun saveScrollPosition(fingerprint: String, offset: Int, lastHeadingId: String?) {
        try {
            context.dataStore.edit { preferences ->
                preferences[scrollOffsetKey(fingerprint)] = offset
                if (lastHeadingId != null) {
                    preferences[lastHeadingKey(fingerprint)] = lastHeadingId
                }
            }
        } catch (e: Exception) {
            // Silently swallow DataStore contention errors to avoid crashing the coroutine.
        }
    }
}
