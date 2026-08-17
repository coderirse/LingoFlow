package com.lingoflow.app.ui.dictionary

import com.lingoflow.app.domain.exception.DictionaryException
import com.lingoflow.app.domain.model.dictionary.DictionaryEntry

/** UI state for dictionary lookups. */
sealed interface DictionaryUiState {
    data object Idle : DictionaryUiState
    data object Loading : DictionaryUiState
    data class Success(val entries: List<DictionaryEntry>) : DictionaryUiState
    data class Error(val error: DictionaryException) : DictionaryUiState
}
