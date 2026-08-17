package com.lingoflow.app.domain.repository

import com.lingoflow.app.domain.model.history.TranslationHistoryItem
import kotlinx.coroutines.flow.Flow

/** Stores past translations for the History tab. */
interface HistoryRepository {

    suspend fun addHistory(item: TranslationHistoryItem)

    suspend fun deleteHistory(id: String)

    suspend fun toggleFavorite(id: String)

    suspend fun clearAllHistory()

    /** All records, newest first. */
    fun getAllHistory(): Flow<List<TranslationHistoryItem>>
}
