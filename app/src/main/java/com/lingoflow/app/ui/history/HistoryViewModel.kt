package com.lingoflow.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingoflow.app.domain.model.history.TranslationHistoryItem
import com.lingoflow.app.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val history: StateFlow<List<TranslationHistoryItem>> =
        historyRepository.getAllHistory()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun delete(id: String) {
        viewModelScope.launch { historyRepository.deleteHistory(id) }
    }

    /** Undo for a deletion: re-inserts the record (sorted back into place). */
    fun restore(item: TranslationHistoryItem) {
        viewModelScope.launch { historyRepository.addHistory(item) }
    }

    fun clearAll() {
        viewModelScope.launch { historyRepository.clearAllHistory() }
    }
}
