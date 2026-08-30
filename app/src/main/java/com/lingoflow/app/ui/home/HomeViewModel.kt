package com.lingoflow.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingoflow.app.data.engine.LongTextFormatter
import com.lingoflow.app.data.engine.TranslationRouter
import com.lingoflow.app.data.tts.TtsPlaybackState
import com.lingoflow.app.data.tts.TtsEngine
import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.TranslationException
import com.lingoflow.app.domain.model.TranslationStatus
import com.lingoflow.app.domain.model.history.TranslationHistoryItem
import com.lingoflow.app.domain.model.dictionary.WordLookupInfo
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.model.translation.TranslationNotices
import com.lingoflow.app.domain.model.translation.TranslationErrors
import com.lingoflow.app.domain.model.translation.TranslationMemory
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
    /** Stable error key from [TranslationErrors]; the UI localizes it. */
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
    /** Current text-to-speech playback state. */
    val ttsPlaybackState: TtsPlaybackState = TtsPlaybackState.IDLE,
    /** True while an LLM streaming translation is in progress. */
    val isStreaming: Boolean = false,
    /** Text received so far during a streaming translation. */
    val streamingText: String = "",
    /** Chinese dictionary info for single-word English→Chinese translations. */
    val wordLookup: WordLookupInfo? = null,
    /** True while the LLM dictionary block is being fetched. */
    val wordLookupLoading: Boolean = false,
    /** The mode that produced the current [translationResponse]; drives the badge. */
    val resultMode: TranslationMode? = null
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
        _uiState.update { it.copy(ttsReady = ttsEngine.isReady.value) }
        viewModelScope.launch {
            ttsEngine.isReady.collect { ready ->
                _uiState.update { it.copy(ttsReady = ready) }
            }
        }
        viewModelScope.launch {
            ttsEngine.playbackState.collect { playbackState ->
                _uiState.update { it.copy(ttsPlaybackState = playbackState) }
            }
        }
        // Resume the last-used setup: e.g. zh->en + Natural last time means
        // the same next launch. Without memory (fresh install) fall back to
        // the settings' default mode and the plain AUTO/Chinese pair.
        viewModelScope.launch {
            val memory = settingsRepository.translationMemory()
            val settings = settingsRepository.getSettings()
            _uiState.update {
                if (memory != null) {
                    it.copy(
                        sourceLanguage = memory.source,
                        targetLanguage = memory.target,
                        translationMode = memory.mode
                    )
                } else {
                    it.copy(translationMode = settings.defaultTranslationMode)
                }
            }
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
        rememberCurrentSetup()
    }

    fun onTargetLanguageChange(language: Language) {
        // Auto is never a valid target; the target picker already hides it.
        if (language == Language.AUTO) return
        _uiState.update { it.copy(targetLanguage = language) }
        rememberCurrentSetup()
    }

    fun onModeChange(mode: TranslationMode) {
        ttsEngine.stop()
        _uiState.update { it.copy(translationMode = mode) }
        rememberCurrentSetup()
    }

    /**
     * Persists the current language pair + mode so the next launch resumes
     * here. Scoped to its own three DataStore keys - never touches the
     * settings document (API keys etc.). Fire-and-forget; a failed save is
     * invisible and self-corrects on the next change.
     */
    private fun rememberCurrentSetup() {
        val snapshot = _uiState.value
        val memory = TranslationMemory(
            source = snapshot.sourceLanguage,
            target = snapshot.targetLanguage,
            mode = snapshot.translationMode
        )
        viewModelScope.launch { runCatching { settingsRepository.saveTranslationMemory(memory) } }
    }

    /**
     * Swaps source and target. When the source is Auto there is no concrete
     * language to move into the target slot, so the target stays put and the
     * old target becomes the new source.
     */
    fun onSwapLanguages() {
        ttsEngine.stop()
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
        rememberCurrentSetup()
    }

    fun onTranslateClick() {
        ttsEngine.stop()
        val snapshot = _uiState.value
        if (snapshot.inputText.isBlank() || snapshot.isTranslating) return

        if (shouldStream(snapshot.translationMode, snapshot.inputText)) {
            startStreamingTranslation(snapshot)
        } else {
            startOneShotTranslation(snapshot)
        }
    }

    /**
     * NATURAL/CONCISE/FORMAL always stream. Long STANDARD text also streams:
     * the router feeds it through the LLM so the layout gets organized and
     * the result appears progressively. Short STANDARD (and LEARNING, whose
     * JSON analysis only exists as a whole) stay one-shot.
     */
    private fun shouldStream(mode: TranslationMode, text: String): Boolean =
        mode in STREAMING_MODES ||
            (mode == TranslationMode.STANDARD &&
                text.trim().length >= TranslationRouter.LONG_TEXT_MIN_LENGTH)

    /** Cancels an in-flight streaming translation, keeping the partial text. */
    fun onCancelStreaming() {
        streamJob?.cancel()
    }

    /** Cancels an in-flight one-shot translation (e.g. LEARNING mode). */
    fun onCancelTranslation() {
        oneShotJob?.cancel()
    }

    private var streamJob: kotlinx.coroutines.Job? = null
    private var oneShotJob: kotlinx.coroutines.Job? = null
    private var wordLookupJob: kotlinx.coroutines.Job? = null

    private fun translatedTextOf(response: TranslationResponse): String =
        when (response) {
            is TranslationResponse.Standard -> response.translatedText
            is TranslationResponse.Learning -> response.translatedText
        }

    private fun startOneShotTranslation(snapshot: HomeUiState) {
        // Rapid re-taps (or a mode switch mid-flight) must not leave two
        // one-shot translations racing over the shared status flow.
        oneShotJob?.cancel()
        oneShotJob = viewModelScope.launch {
            _uiState.update { it.copy(isTranslating = true, errorMessage = null) }
            try {
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
                    ttsEngine.stop()
                    _uiState.update {
                        it.copy(
                            isTranslating = false,
                            translationResponse = null,
                            errorMessage = (error as? TranslationException)?.code
                                ?: TranslationErrors.GENERIC
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // User cancelled: restore idle state, keep the previous result.
                _uiState.update { it.copy(isTranslating = false) }
                throw e
            }
        }
    }

    private fun startStreamingTranslation(snapshot: HomeUiState) {
        streamJob?.cancel()
        val resultMode = snapshot.translationMode
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
                // Long STANDARD runs through the formatter at the end: the
                // model already emits structure while streaming, and this
                // pass normalizes the last mile (literal "\n" escapes, list
                // markers that still share a line).
                val finalText = if (snapshot.translationMode == TranslationMode.STANDARD) {
                    LongTextFormatter.format(accumulated.toString())
                } else {
                    accumulated.toString()
                }
                onTranslationFinished(
                    snapshot,
                    finalText,
                    TranslationResponse.Standard(finalText)
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                ttsEngine.stop()
                // User cancelled: keep what already streamed on screen as a
                // plain result (no history entry, no word lookup).
                _uiState.update { state ->
                    val partial = state.streamingText
                    if (partial.isBlank()) {
                        state.copy(isTranslating = false, isStreaming = false)
                    } else {
                        state.copy(
                            isTranslating = false,
                            isStreaming = false,
                            translationResponse = TranslationResponse.Standard(partial),
                            resultMode = resultMode
                        )
                    }
                }
            } catch (e: Exception) {
                ttsEngine.stop()
                val partial = _uiState.value.streamingText
                if (partial.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isTranslating = false,
                            isStreaming = false,
                            streamingText = "",
                            errorMessage = (e as? TranslationException)?.code
                                ?: TranslationErrors.GENERIC
                        )
                    }
                } else {
                    // Broke mid-stream: keep the partial text on screen and
                    // flag it with a transient notice instead of wiping it.
                    _uiState.update {
                        it.copy(
                            isTranslating = false,
                            isStreaming = false,
                            translationResponse = TranslationResponse.Standard(partial),
                            resultMode = resultMode,
                            snackbarMessage = TranslationNotices.STREAM_INTERRUPTED
                        )
                    }
                }
            }
        }
    }

    private suspend fun onTranslationFinished(
        snapshot: HomeUiState,
        translatedText: String,
        response: TranslationResponse
    ) {
        // A finished translation replaces the screen content; any speech of
        // the previous result must not keep talking over it.
        ttsEngine.stop()
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
                resultMode = snapshot.translationMode,
                errorMessage = null,
                currentHistoryId = historyItem.id,
                isCurrentFavorite = false,
                ttsReady = ttsEngine.isReady.value,
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

        // Tracked so clearing the result cancels a still-running lookup —
        // otherwise a slow LLM answer could land under the next translation.
        wordLookupJob?.cancel()
        wordLookupJob = viewModelScope.launch {
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
        ttsEngine.stop()
        wordLookupJob?.cancel()
        _uiState.update {
            it.copy(
                inputText = "",
                translationResponse = null,
                resultMode = null,
                errorMessage = null,
                currentHistoryId = null,
                isCurrentFavorite = false,
                wordLookup = null,
                wordLookupLoading = false
            )
        }
    }

    /**
     * Play / pause / resume for the current translation. The platform TTS
     * has no pause API; the engine emulates it at sentence granularity.
     */
    fun onSpeakClick() {
        val state = _uiState.value
        val text = state.translatedText
        if (text.isBlank()) return
        when (state.ttsPlaybackState) {
            TtsPlaybackState.IDLE ->
                ttsEngine.speak(text, state.targetLanguage.ttsTag ?: "en-US")

            TtsPlaybackState.SPEAKING -> ttsEngine.pause()
            TtsPlaybackState.PAUSED -> ttsEngine.resume()
        }
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
        ttsEngine.stop()
        wordLookupJob?.cancel()
        _uiState.update {
            it.copy(
                translationResponse = null,
                resultMode = null,
                errorMessage = null,
                currentHistoryId = null,
                isCurrentFavorite = false,
                wordLookup = null,
                wordLookupLoading = false
            )
        }
    }

    /**
     * Reuses a history record: restores the source text together with the
     * language pair and mode it was translated with, not just the text.
     */
    fun onReuseHistoryItem(item: TranslationHistoryItem) {
        ttsEngine.stop()
        _uiState.update {
            it.copy(
                inputText = item.sourceText,
                sourceLanguage = item.sourceLanguage,
                targetLanguage = item.targetLanguage,
                translationMode = item.mode,
                translationResponse = null,
                resultMode = null,
                errorMessage = null,
                currentHistoryId = null,
                isCurrentFavorite = false
            )
        }
        rememberCurrentSetup()
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
