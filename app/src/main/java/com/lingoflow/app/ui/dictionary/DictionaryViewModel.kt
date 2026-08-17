package com.lingoflow.app.ui.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingoflow.app.data.dictionary.InflectionStemmer
import com.lingoflow.app.data.tts.TtsEngine
import com.lingoflow.app.domain.exception.DictionaryException
import com.lingoflow.app.domain.model.dictionary.DictionaryEntry
import com.lingoflow.app.domain.model.dictionary.WordLookupInfo
import com.lingoflow.app.domain.repository.DictionaryRepository
import com.lingoflow.app.domain.repository.FavoritesRepository
import com.lingoflow.app.domain.usecase.LookupWordUseCase
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
    private val ttsEngine: TtsEngine,
    private val lookupWordUseCase: LookupWordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DictionaryUiState>(DictionaryUiState.Idle)
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    /** Chinese dictionary info from the LLM; null until loaded or on failure. */
    private val _lookupInfo = MutableStateFlow<WordLookupInfo?>(null)
    val lookupInfo: StateFlow<WordLookupInfo?> = _lookupInfo.asStateFlow()

    /** True when the LLM lookup finished without usable Chinese info. */
    private val _lookupInfoUnavailable = MutableStateFlow(false)
    val lookupInfoUnavailable: StateFlow<Boolean> = _lookupInfoUnavailable.asStateFlow()

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
            _uiState.value = lookUpWithInflectionFallback(word.trim().lowercase())
        }
        // Chinese glosses come from the LLM in parallel; failures are silent
        // and the UI falls back to the Merriam-Webster English entry.
        _lookupInfo.value = null
        _lookupInfoUnavailable.value = false
        viewModelScope.launch {
            lookupWordUseCase(word.trim().lowercase())
                .onSuccess { _lookupInfo.value = it }
                .onFailure { _lookupInfoUnavailable.value = true }
        }
    }

    /**
     * MW indexes headwords, not inflections: "served" comes back as a
     * NotFound suggestion list. When that happens, retry with morphological
     * stems (serve, …) and MW's own first suggestion before giving up.
     */
    private suspend fun lookUpWithInflectionFallback(word: String): DictionaryUiState {
        val firstResult = dictionaryRepository.lookup(word)
        firstResult.onSuccess { return DictionaryUiState.Success(it) }

        val firstError = firstResult.exceptionOrNull()
        if (firstError !is DictionaryException.NotFound) {
            return errorState(firstError)
        }

        val retryCandidates = buildList {
            addAll(InflectionStemmer.candidates(word))
            firstError.suggestions.firstOrNull()?.let { add(it.lowercase()) }
        }.distinct().filter { it != word }

        for (candidate in retryCandidates) {
            val retry = dictionaryRepository.lookup(candidate)
            retry.onSuccess { return DictionaryUiState.Success(it) }
        }
        return errorState(firstError)
    }

    private fun errorState(error: Throwable?): DictionaryUiState.Error =
        DictionaryUiState.Error(
            error as? DictionaryException
                ?: DictionaryException.Network(error)
        )

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
