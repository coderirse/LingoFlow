package com.lingoflow.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingoflow.app.data.tts.TtsEngine
import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.TranslationException
import com.lingoflow.app.domain.model.TranslationStatus
import com.lingoflow.app.domain.model.history.TranslationHistoryItem
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.model.translation.TranslationRequest
import com.lingoflow.app.domain.model.translation.TranslationResponse
import com.lingoflow.app.domain.model.ttsTag
import com.lingoflow.app.domain.repository.HistoryRepository
import com.lingoflow.app.domain.repository.SettingsRepository
import com.lingoflow.app.domain.usecase.TranslateTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the home translation screen. */
data class HomeUiState(
    val sourceLanguage: Language = Language.AUTO,
    val targetLanguage: Language = Language.CHINESE,
    val inputText: String = "",
    val translationMode: TranslationMode = TranslationMode.STANDARD,
    val translationResponse: TranslationResponse? = null,
    val isTranslating: Boolean = false,
    val status: TranslationStatus = TranslationStatus.IDLE,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,
    /** Word tapped in the translation result; triggers dictionary lookup. */
    val lookupWord: String? = null,
    /** History record id of the current translation, for the favorite toggle. */
    val currentHistoryId: String? = null,
    /** Whether the current translation record is favorited. */
    val isCurrentFavorite: Boolean = false,
    /** Whether text-to-speech is ready on this device. */
    val ttsReady: Boolean = false
) {
    /** Convenience accessor for copy/share regardless of response type. */
    val translatedText: String
        get() = when (val response = translationResponse) {
            is TranslationResponse.Standard -> response.translatedText
            is TranslationResponse.Learning -> response.translatedText
            null -> ""
        }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val translateText: TranslateTextUseCase,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val ttsEngine: TtsEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(ttsReady = ttsEngine.isReady) }
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            _uiState.update { it.copy(translationMode = settings.defaultTranslationMode) }
        }
        viewModelScope.launch {
            translateText.status.collect { status ->
                _uiState.update { it.copy(status = status) }
            }
        }
        viewModelScope.launch {
            translateText.fallbackMessages.collect { message ->
                _uiState.update { it.copy(snackbarMessage = message) }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onSourceLanguageChange(language: Language) {
        _uiState.update { it.copy(sourceLanguage = language) }
    }

    fun onTargetLanguageChange(language: Language) {
        // Auto is never a valid target; the target picker already hides it.
        if (language == Language.AUTO) return
        _uiState.update { it.copy(targetLanguage = language) }
    }

    fun onModeChange(mode: TranslationMode) {
        _uiState.update { it.copy(translationMode = mode) }
    }

    /**
     * Swaps source and target. When the source is Auto there is no concrete
     * language to move into the target slot, so the target stays put and the
     * old target becomes the new source.
     */
    fun onSwapLanguages() {
        _uiState.update { state ->
            if (state.sourceLanguage == Language.AUTO) {
                state.copy(sourceLanguage = state.targetLanguage)
            } else {
                state.copy(
                    sourceLanguage = state.targetLanguage,
                    targetLanguage = state.sourceLanguage
                )
            }
        }
    }

    fun onTranslateClick() {
        val snapshot = _uiState.value
        if (snapshot.inputText.isBlank() || snapshot.isTranslating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isTranslating = true, errorMessage = null) }
            translateText(
                TranslationRequest(
                    text = snapshot.inputText,
                    sourceLanguage = snapshot.sourceLanguage,
                    targetLanguage = snapshot.targetLanguage,
                    mode = snapshot.translationMode
                )
            ).onSuccess { response ->
                val historyItem = TranslationHistoryItem(
                    sourceText = snapshot.inputText,
                    translatedText = when (response) {
                        is TranslationResponse.Standard -> response.translatedText
                        is TranslationResponse.Learning -> response.translatedText
                    },
                    sourceLanguage = snapshot.sourceLanguage,
                    targetLanguage = snapshot.targetLanguage,
                    mode = snapshot.translationMode
                )
                historyRepository.addHistory(historyItem)
                _uiState.update {
                    it.copy(
                        isTranslating = false,
                        translationResponse = response,
                        errorMessage = null,
                        currentHistoryId = historyItem.id,
                        isCurrentFavorite = false,
                        ttsReady = ttsEngine.isReady
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isTranslating = false,
                        translationResponse = null,
                        errorMessage = (error as? TranslationException)?.userMessage
                            ?: "Translation failed. Please try again."
                    )
                }
            }
        }
    }

    fun onClearInput() {
        _uiState.update {
            it.copy(
                inputText = "",
                translationResponse = null,
                errorMessage = null,
                currentHistoryId = null,
                isCurrentFavorite = false
            )
        }
    }

    /** Speaks the current translation in the target language. */
    fun onSpeakClick() {
        val state = _uiState.value
        val text = state.translatedText
        if (text.isBlank()) return
        ttsEngine.speak(text, state.targetLanguage.ttsTag ?: "en-US")
    }

    /** Toggles the favorite flag on the current translation record. */
    fun onToggleFavoriteTranslation() {
        val historyId = _uiState.value.currentHistoryId ?: return
        viewModelScope.launch {
            historyRepository.toggleFavorite(historyId)
            _uiState.update { it.copy(isCurrentFavorite = !it.isCurrentFavorite) }
        }
    }

    fun onClearResult() {
        _uiState.update {
            it.copy(
                translationResponse = null,
                errorMessage = null,
                currentHistoryId = null,
                isCurrentFavorite = false
            )
        }
    }

    /** Called by the UI after the transient snackbar has been shown. */
    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    /** A word was tapped in the translation result; open dictionary lookup. */
    fun onWordClick(word: String) {
        if (word.isBlank()) return
        _uiState.update { it.copy(lookupWord = word.trim().lowercase()) }
    }

    /** Called by the UI after the dictionary sheet for [HomeUiState.lookupWord] closed. */
    fun consumeLookupWord() {
        _uiState.update { it.copy(lookupWord = null) }
    }
}
