package com.lingoflow.app.data.repository

import com.lingoflow.app.domain.model.history.TranslationHistoryItem
import com.lingoflow.app.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [HistoryRepository] for ViewModel tests. */
class FakeHistoryRepository : HistoryRepository {

    private val items = MutableStateFlow<List<TranslationHistoryItem>>(emptyList())

    override suspend fun addHistory(item: TranslationHistoryItem) {
        items.value = (listOf(item) + items.value)
            .sortedByDescending { it.timestamp }
            .take(50)
    }

    override suspend fun deleteHistory(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }

    override suspend fun toggleFavorite(id: String) {
        items.value = items.value.map {
            if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    override suspend fun clearAllHistory() {
        items.value = emptyList()
    }

    override fun getAllHistory(): Flow<List<TranslationHistoryItem>> = items

    fun favoriteOf(id: String): Flow<Boolean?> =
        items.map { list -> list.firstOrNull { it.id == id }?.isFavorite }
}
