package com.lingoflow.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lingoflow.app.domain.model.history.TranslationHistoryItem
import com.lingoflow.app.domain.repository.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * DataStore-backed [HistoryRepository]: the whole list is stored as one JSON
 * document. Capped at [MAX_ITEMS]; oldest entries are dropped first.
 */
@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : HistoryRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun addHistory(item: TranslationHistoryItem) {
        dataStore.edit { prefs ->
            val current = prefs.decode()
            val updated = (listOf(item) + current)
                .sortedByDescending { it.timestamp }
                .take(MAX_ITEMS)
            prefs[KEY_HISTORY] = json.encodeToString(updated)
        }
    }

    override suspend fun deleteHistory(id: String) {
        dataStore.edit { prefs ->
            prefs[KEY_HISTORY] = json.encodeToString(prefs.decode().filterNot { it.id == id })
        }
    }

    override suspend fun toggleFavorite(id: String) {
        dataStore.edit { prefs ->
            val updated = prefs.decode().map {
                if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
            }
            prefs[KEY_HISTORY] = json.encodeToString(updated)
        }
    }

    override suspend fun clearAllHistory() {
        dataStore.edit { prefs ->
            // Favorite records survive "Clear All": they are the user's
            // curated learning material (Learning tab), not transient
            // history. The confirm dialog in the UI says the same.
            val favorites = prefs.decode().filter { it.isFavorite }
            if (favorites.isEmpty()) {
                prefs.remove(KEY_HISTORY)
            } else {
                prefs[KEY_HISTORY] = json.encodeToString(favorites)
            }
        }
    }

    override fun getAllHistory(): Flow<List<TranslationHistoryItem>> =
        dataStore.data.map { it.decode().sortedByDescending { item -> item.timestamp } }

    private fun Preferences.decode(): List<TranslationHistoryItem> =
        this[KEY_HISTORY]?.let { raw ->
            runCatching {
                json.decodeFromString<List<TranslationHistoryItem>>(raw)
            }.getOrDefault(emptyList())
        } ?: emptyList()

    private companion object {
        const val MAX_ITEMS = 50
        val KEY_HISTORY = stringPreferencesKey("translation_history_v2")
    }
}
