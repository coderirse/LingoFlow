package com.lingoflow.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.lingoflow.app.domain.repository.FavoritesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore-backed [FavoritesRepository]; shares the settings DataStore file. */
@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : FavoritesRepository {

    override suspend fun addFavorite(word: String) {
        val normalized = word.trim().lowercase()
        if (normalized.isEmpty()) return
        dataStore.edit { prefs ->
            prefs[KEY_FAVORITES] = (prefs[KEY_FAVORITES] ?: emptySet()) + normalized
        }
    }

    override suspend fun removeFavorite(word: String) {
        val normalized = word.trim().lowercase()
        dataStore.edit { prefs ->
            prefs[KEY_FAVORITES] = (prefs[KEY_FAVORITES] ?: emptySet()) - normalized
        }
    }

    override fun getFavorites(): Flow<Set<String>> =
        dataStore.data.map { it[KEY_FAVORITES] ?: emptySet() }

    override fun isFavorite(word: String): Flow<Boolean> {
        val normalized = word.trim().lowercase()
        return getFavorites().map { normalized in it }
    }

    private companion object {
        val KEY_FAVORITES = stringSetPreferencesKey("favorites")
    }
}
