package com.lingoflow.app.domain.repository

import kotlinx.coroutines.flow.Flow

/** Stores the user's favorite dictionary words. */
interface FavoritesRepository {

    suspend fun addFavorite(word: String)

    suspend fun removeFavorite(word: String)

    fun getFavorites(): Flow<Set<String>>

    fun isFavorite(word: String): Flow<Boolean>
}
