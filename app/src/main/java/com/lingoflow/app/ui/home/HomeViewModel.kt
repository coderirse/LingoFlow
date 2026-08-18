package com.lingoflow.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingoflow.app.data.tts.TtsEngine
import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.TranslationException
import com.lingoflow.app.domain.model.TranslationStatus
import com.lingoflow.app.domain.model.history.TranslationHistoryItem
import com.lingoflow.app.domain.model.dictionary.WordLookupInfo
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.model.translation.TranslationRequest
import com.lingoflow.app.domain.model.translation.TranslationResponse
import com.lingoflow.app.domain.model.ttsTag
import com.lingoflow.app.domain.repository.HistoryRepository
import com.lingoflow.app.domain.repository.SettingsRepository
import com.lingoflow.app.domain.usecase.LookupWordUseCase
import com.lingoflow.app.domain.usecase.TranslateTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
    val ttsReady: Boolean = false,
    /** True while an LLM streaming translation is in progress. */
    val isStreaming: Boolean = false,
    /** Text received so far during a streaming translation. */
    val streamingText: String = "",
    /** Chinese dictionary info for single-word English→Chinese translations. */
    val wordLookup: WordLookupInfo? = null,
    /** True while the LLM dictionary block is being fetched. */
    val wordLookupLoading: Boolean = false
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
    private val ttsEngine: TtsEngine,
    private val lookupWordUseCase: LookupWordUseCase
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
        // Single source of truth for the favorite heart: the stored history
        // record. Unfavoriting from the Learning tab updates the heart here.
        // catch {} guards the process: a DataStore hiccup must never crash.
        viewModelScope.launch {
            historyRepository.getAllHistory()
                .catch { }
                .collect { items ->
                    _uiState.update { state ->
                        val favorite = state.currentHistoryId
                            ?.let { id -> items.firstOrNull { it.id == id }?.isFavorite }
                            ?: false
                        if (favorite == state.isCurrentFavorite) {
                            state
                        } else {
                            state.copy(isCurrentFavorite = favorite)
                        }
                    }
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

        if (snapshot.translationMode in STREAMING_MODES) {
            startStreamingTranslation(snapshot)
        } else {
            startOneShotTranslation(snapshot)
        }
    }

    /** Cancels an in-flight streaming translation, keeping the partial text. */
    fun onCancelStreaming() {
        streamJob?.cancel()
    }

    private var streamJob: kotlinx.coroutines.Job? = null

    private fun translatedTextOf(response: TranslationResponse): String =
        when (response) {
            is TranslationResponse.Standard -> response.translatedText
            is TranslationResponse.Learning -> response.translatedText
        }

    private fun startOneShotTranslation(snapshot: HomeUiState) {
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
                onTranslationFinished(snapshot, translatedTextOf(response), response)
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

    private fun startStreamingTranslation(snapshot: HomeUiState) {
        streamJob?.cancel()
        val request = TranslationRequest(
            text = snapshot.inputText,
            sourceLanguage = snapshot.sourceLanguage,
            targetLanguage = snapshot.targetLanguage,
            mode = snapshot.translationMode
        )
        val stream = translateText.translateStream(request)
        if (stream == null) {
            startOneShotTranslation(snapshot)
            return
        }

        streamJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTranslating = true,
                    isStreaming = true,
                    streamingText = "",
                    errorMessage = null
                )
            }
            val accumulated = StringBuilder()
            try {
                stream.collect { delta ->
                    accumulated.append(delta)
                    _uiState.update { it.copy(streamingText = accumulated.toString()) }
                }
                onTranslationFinished(
                    snapshot,
                    accumulated.toString(),
                    TranslationResponse.Standard(accumulated.toString())
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                // User cancelled: keep the partial text on screen, don't save.
                _uiState.update {
                    it.copy(isTranslating = false, isStreaming = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTranslating = false,
                        isStreaming = false,
                        streamingText = "",
                        errorMessage = (e as? TranslationException)?.userMessage
                            ?: "Translation failed. Please try again."
                    )
                }
            }
        }
    }

    private suspend fun onTranslationFinished(
        snapshot: HomeUiState,
        translatedText: String,
        response: TranslationResponse
    ) {
        val historyItem = TranslationHistoryItem(
            sourceText = snapshot.inputText,
            translatedText = translatedText,
            sourceLanguage = snapshot.sourceLanguage,
            targetLanguage = snapshot.targetLanguage,
            mode = snapshot.translationMode
        )
        historyRepository.addHistory(historyItem)
        _uiState.update {
            it.copy(
                isTranslating = false,
                isStreaming = false,
                streamingText = "",
                translationResponse = response,
                errorMessage = null,
                currentHistoryId = historyItem.id,
                isCurrentFavorite = false,
                ttsReady = ttsEngine.isReady,
                wordLookup = null,
                wordLookupLoading = false
            )
        }
        maybeLoadWordLookup(snapshot)
    }

    /**
     * Youdao-style enrichment: a lone English word translated to Chinese via
     * ML Kit gets an additional Chinese dictionary block below the direct
     * translation. Silent no-op on any failure.
     */
    private fun maybeLoadWordLookup(snapshot: HomeUiState) {
        val word = snapshot.inputText.trim()
        val eligible = snapshot.translationMode == TranslationMode.STANDARD &&
            snapshot.targetLanguage == Language.CHINESE &&
            word.matches(SINGLE_ENGLISH_WORD)
        if (!eligible) return

        viewModelScope.launch {
            _uiState.update { it.copy(wordLookupLoading = true) }
            // Hard cap: a hung LLM call must never leave the loading cursor
            // on screen forever. Timeout settles silently, same as failure.
            val result = withTimeoutOrNull(WORD_LOOKUP_TIMEOUT_MS) {
                lookupWordUseCase(word.lowercase())
            }
            result?.onSuccess { info ->
                _uiState.update { it.copy(wordLookup = info) }
            }
            // Failure/timeout: silently keep the plain ML Kit translation only.
            _uiState.update { it.copy(wordLookupLoading = false) }
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

    /**
     * Toggles the favorite flag on the current translation record. The heart
     * state itself is driven by the history flow collector, so the stored
     * record is the single source of truth.
     */
    fun onToggleFavoriteTranslation() {
        val historyId = _uiState.value.currentHistoryId ?: return
        viewModelScope.launch {
            historyRepository.toggleFavorite(historyId)
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

    private companion object {
        val STREAMING_MODES = setOf(
            TranslationMode.NATURAL,
            TranslationMode.CONCISE,
            TranslationMode.FORMAL
        )
        val SINGLE_ENGLISH_WORD = Regex("^[a-zA-Z-]{1,30}$")
        const val WORD_LOOKUP_TIMEOUT_MS = 20_000L
    }
}
