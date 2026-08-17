package com.lingoflow.app.ui.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingoflow.app.data.tts.TtsEngine
import com.lingoflow.app.domain.exception.DictionaryException
import com.lingoflow.app.domain.repository.DictionaryRepository
import com.lingoflow.app.domain.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DictionaryViewModel @Inject constructor(
    private val dictionaryRepository: DictionaryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val ttsEngine: TtsEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<DictionaryUiState>(DictionaryUiState.Idle)
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    /** Current set of favorited words, for the favorite toggle. */
    val favorites: StateFlow<Set<String>> = favoritesRepository.getFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )

    val ttsReady: Boolean get() = ttsEngine.isReady

    fun lookUp(word: String) {
        if (word.isBlank()) return

        viewModelScope.launch {
            _uiState.value = DictionaryUiState.Loading
            dictionaryRepository.lookup(word.trim())
                .onSuccess { entries ->
                    _uiState.value = DictionaryUiState.Success(entries)
                }
                .onFailure { error ->
                    _uiState.value = DictionaryUiState.Error(
                        error as? DictionaryException
                            ?: DictionaryException.Network(error)
                    )
                }
        }
    }

    fun toggleFavorite(word: String) {
        viewModelScope.launch {
            val normalized = word.trim().lowercase()
            if (normalized.isEmpty()) return@launch
            if (favoritesRepository.isFavorite(normalized).first()) {
                favoritesRepository.removeFavorite(normalized)
            } else {
                favoritesRepository.addFavorite(normalized)
            }
        }
    }

    fun speak(word: String) {
        ttsEngine.speak(word, "en-US")
    }
}
