package com.lingoflow.app.data.repository

import com.lingoflow.app.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [FavoritesRepository] for ViewModel tests. */
class FakeFavoritesRepository(
    initial: Set<String> = emptySet()
) : FavoritesRepository {

    private val favorites = MutableStateFlow(initial)

    override suspend fun addFavorite(word: String) {
        favorites.value = favorites.value + word.trim().lowercase()
    }

    override suspend fun removeFavorite(word: String) {
        favorites.value = favorites.value - word.trim().lowercase()
    }

    override fun getFavorites(): Flow<Set<String>> = favorites

    override fun isFavorite(word: String): Flow<Boolean> =
        favorites.map { word.trim().lowercase() in it }
}
