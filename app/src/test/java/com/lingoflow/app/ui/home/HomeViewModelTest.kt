package com.lingoflow.app.ui.home

import com.lingoflow.app.data.repository.FakeHistoryRepository
import com.lingoflow.app.data.repository.FakeSettingsRepository
import com.lingoflow.app.data.translator.FakeTranslator
import com.lingoflow.app.data.tts.FakeTtsEngine
import com.lingoflow.app.domain.engine.MlKitTranslationEngine
import com.lingoflow.app.domain.engine.TranslationEngine
import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.TranslationStatus
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.model.translation.TranslationRequest
import com.lingoflow.app.domain.model.translation.TranslationResponse
import com.lingoflow.app.domain.usecase.TranslateTextUseCase
import com.lingoflow.app.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Engine whose every translation fails, for the error path. */
    private class FailingEngine : TranslationEngine {
        override val status: StateFlow<TranslationStatus> =
            MutableStateFlow(TranslationStatus.IDLE)

        override suspend fun translate(
            request: TranslationRequest
        ): Result<TranslationResponse> = Result.failure(
            com.lingoflow.app.domain.model.TranslationException(
                "Translation failed. Please try again."
            )
        )
    }

    private fun createViewModel(
        engine: TranslationEngine = MlKitTranslationEngine(FakeTranslator()),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        history: FakeHistoryRepository = FakeHistoryRepository(),
        tts: FakeTtsEngine = FakeTtsEngine(),
        lookup: com.lingoflow.app.domain.usecase.LookupWordUseCase =
            com.lingoflow.app.domain.usecase.LookupWordUseCase(settings) {
                com.lingoflow.app.data.llm.FakeLlmProvider()
            }
    ) = ViewModelBundle(
        viewModel = HomeViewModel(
            TranslateTextUseCase(engine), settings, history, tts, lookup
        ),
        history = history,
        tts = tts
    )

    private data class ViewModelBundle(
        val viewModel: HomeViewModel,
        val history: FakeHistoryRepository,
        val tts: FakeTtsEngine
    )

    @Test
    fun `initial state is idle with auto source and chinese target`() = runTest {
        val viewModel = createViewModel().viewModel
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(Language.AUTO, state.sourceLanguage)
        assertEquals(Language.CHINESE, state.targetLanguage)
        assertFalse(state.isTranslating)
        assertNull(state.errorMessage)
        assertNull(state.translationResponse)
    }

    @Test
    fun `input change updates state`() {
        val viewModel = createViewModel().viewModel
        viewModel.onInputChange("Hello")
        assertEquals("Hello", viewModel.uiState.value.inputText)
    }

    @Test
    fun `translate succeeds and exposes the result`() = runTest {
        val viewModel = createViewModel().viewModel
        viewModel.onInputChange("Hello")
        viewModel.onTranslateClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isTranslating)
        assertNull(state.errorMessage)
        assertTrue(state.translationResponse is TranslationResponse.Standard)
        assertEquals("你好", state.translatedText)
    }

    @Test
    fun `translate with blank input is a no-op`() = runTest {
        val viewModel = createViewModel().viewModel
        viewModel.onTranslateClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isTranslating)
        assertNull(state.translationResponse)
    }

    @Test
    fun `translator failure surfaces a friendly error`() = runTest {
        val viewModel = createViewModel(engine = FailingEngine()).viewModel
        viewModel.onInputChange("Hello")
        viewModel.onTranslateClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isTranslating)
        assertNull(state.translationResponse)
        assertEquals("Translation failed. Please try again.", state.errorMessage)
    }

    @Test
    fun `swap exchanges source and target`() {
        val viewModel = createViewModel().viewModel
        viewModel.onSourceLanguageChange(Language.ENGLISH)
        viewModel.onTargetLanguageChange(Language.JAPANESE)
        viewModel.onSwapLanguages()

        val state = viewModel.uiState.value
        assertEquals(Language.JAPANESE, state.sourceLanguage)
        assertEquals(Language.ENGLISH, state.targetLanguage)
    }

    @Test
    fun `swap with auto source never makes target auto`() {
        val viewModel = createViewModel().viewModel
        viewModel.onSourceLanguageChange(Language.AUTO)
        viewModel.onTargetLanguageChange(Language.CHINESE)
        viewModel.onSwapLanguages()

        val state = viewModel.uiState.value
        assertEquals(Language.CHINESE, state.sourceLanguage)
        assertEquals(Language.CHINESE, state.targetLanguage)
    }

    @Test
    fun `clear input also clears result and error`() = runTest {
        val viewModel = createViewModel().viewModel
        viewModel.onInputChange("Hello")
        viewModel.onTranslateClick()
        advanceUntilIdle()
        viewModel.onClearInput()

        val state = viewModel.uiState.value
        assertTrue(state.inputText.isEmpty())
        assertNull(state.translationResponse)
        assertNull(state.errorMessage)
    }

    @Test
    fun `default mode loads from settings`() = runTest {
        val settings = FakeSettingsRepository().also {
            it.saveSettings(
                it.getSettings().copy(defaultTranslationMode = TranslationMode.NATURAL)
            )
        }
        val viewModel = createViewModel(settings = settings).viewModel
        advanceUntilIdle()

        assertEquals(TranslationMode.NATURAL, viewModel.uiState.value.translationMode)
    }

    @Test
    fun `mode change updates state`() = runTest {
        val viewModel = createViewModel().viewModel
        advanceUntilIdle()

        viewModel.onModeChange(TranslationMode.FORMAL)
        assertEquals(TranslationMode.FORMAL, viewModel.uiState.value.translationMode)
    }

    @Test
    fun `learning mode exposes learning response`() = runTest {
        val learning = TranslationResponse.Learning(
            translatedText = "你好",
            dictionaryEntries = emptyList(),
            contextExplanation = null
        )
        val engine = object : TranslationEngine {
            override val status: StateFlow<TranslationStatus> =
                MutableStateFlow(TranslationStatus.IDLE)

            override suspend fun translate(
                request: TranslationRequest
            ): Result<TranslationResponse> = Result.success(learning)
        }
        val viewModel = createViewModel(engine = engine).viewModel
        advanceUntilIdle()

        viewModel.onInputChange("Hello")
        viewModel.onModeChange(TranslationMode.LEARNING)
        viewModel.onTranslateClick()
        advanceUntilIdle()

        assertTrue(
            viewModel.uiState.value.translationResponse is TranslationResponse.Learning
        )
    }

    @Test
    fun `word click stores a normalized lookup word`() = runTest {
        val viewModel = createViewModel().viewModel
        advanceUntilIdle()

        viewModel.onWordClick("Hello,")

        assertEquals("hello,", viewModel.uiState.value.lookupWord)
    }

    @Test
    fun `consuming the lookup word clears it`() = runTest {
        val viewModel = createViewModel().viewModel
        advanceUntilIdle()

        viewModel.onWordClick("hello")
        assertEquals("hello", viewModel.uiState.value.lookupWord)

        viewModel.consumeLookupWord()
        assertNull(viewModel.uiState.value.lookupWord)
    }

    @Test
    fun `blank word click is ignored`() = runTest {
        val viewModel = createViewModel().viewModel
        advanceUntilIdle()

        viewModel.onWordClick("   ")

        assertNull(viewModel.uiState.value.lookupWord)
    }

    @Test
    fun `successful translation is saved to history`() = runTest {
        val bundle = createViewModel()
        bundle.viewModel.onInputChange("Hello")
        bundle.viewModel.onTranslateClick()
        advanceUntilIdle()

        val history = bundle.history.getAllHistory().first()
        assertEquals(1, history.size)
        assertEquals("Hello", history.single().sourceText)
        assertEquals("你好", history.single().translatedText)
        assertEquals(history.single().id, bundle.viewModel.uiState.value.currentHistoryId)
        assertFalse(bundle.viewModel.uiState.value.isCurrentFavorite)
    }

    @Test
    fun `favorite toggle flips the history record and ui state`() = runTest {
        val bundle = createViewModel()
        bundle.viewModel.onInputChange("Hello")
        bundle.viewModel.onTranslateClick()
        advanceUntilIdle()

        val historyId = bundle.viewModel.uiState.value.currentHistoryId
        assertNotNull(historyId)

        bundle.viewModel.onToggleFavoriteTranslation()
        advanceUntilIdle()

        assertTrue(bundle.viewModel.uiState.value.isCurrentFavorite)
        assertTrue(bundle.history.favoriteOf(historyId!!).first() == true)

        bundle.viewModel.onToggleFavoriteTranslation()
        advanceUntilIdle()

        assertFalse(bundle.viewModel.uiState.value.isCurrentFavorite)
        assertFalse(bundle.history.favoriteOf(historyId).first() == true)
    }

    @Test
    fun `failed translation does not save history and favorite is disabled`() = runTest {
        val bundle = createViewModel(engine = FailingEngine())
        bundle.viewModel.onInputChange("Hello")
        bundle.viewModel.onTranslateClick()
        advanceUntilIdle()

        assertTrue(bundle.history.getAllHistory().first().isEmpty())
        assertNull(bundle.viewModel.uiState.value.currentHistoryId)
    }

    @Test
    fun `favorite state follows the stored history record`() = runTest {
        val bundle = createViewModel()
        bundle.viewModel.onInputChange("Hello")
        bundle.viewModel.onTranslateClick()
        advanceUntilIdle()

        val historyId = bundle.viewModel.uiState.value.currentHistoryId
        assertNotNull(historyId)
        assertFalse(bundle.viewModel.uiState.value.isCurrentFavorite)

        // Toggled from outside (e.g. the Learning tab): the heart follows.
        bundle.history.toggleFavorite(historyId!!)
        advanceUntilIdle()
        assertTrue(bundle.viewModel.uiState.value.isCurrentFavorite)

        bundle.history.toggleFavorite(historyId)
        advanceUntilIdle()
        assertFalse(bundle.viewModel.uiState.value.isCurrentFavorite)
    }

    @Test
    fun `speak reads the translation in the target language`() = runTest {
        val bundle = createViewModel()
        bundle.viewModel.onInputChange("Hello")
        bundle.viewModel.onTranslateClick()
        advanceUntilIdle()

        bundle.viewModel.onSpeakClick()

        assertEquals(listOf("你好"), bundle.tts.spoken)
    }

    /** Engine that streams scripted deltas, optionally stalling mid-stream. */
    private class FakeStreamingEngine(
        private val deltas: List<String>,
        private val stallAfterFirst: Boolean = false
    ) : TranslationEngine,
        com.lingoflow.app.domain.engine.StreamingTranslationEngine {
        override val status: StateFlow<TranslationStatus> =
            MutableStateFlow(TranslationStatus.IDLE)

        override suspend fun translate(
            request: TranslationRequest
        ): Result<TranslationResponse> =
            Result.success(TranslationResponse.Standard(deltas.joinToString("")))

        override fun translateStream(
            request: TranslationRequest
        ): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.flow {
            deltas.forEachIndexed { index, delta ->
                emit(delta)
                if (stallAfterFirst && index == 0) {
                    kotlinx.coroutines.delay(60_000)
                }
            }
        }
    }

    @Test
    fun `llm mode streams deltas then finalizes with history`() = runTest {
        val bundle = createViewModel(engine = FakeStreamingEngine(listOf("你", "好")))
        advanceUntilIdle()

        bundle.viewModel.onInputChange("Hello")
        bundle.viewModel.onModeChange(TranslationMode.NATURAL)
        bundle.viewModel.onTranslateClick()
        advanceUntilIdle()

        val state = bundle.viewModel.uiState.value
        assertFalse(state.isStreaming)
        assertTrue(state.translationResponse is TranslationResponse.Standard)
        assertEquals("你好", state.translatedText)
        assertEquals("你好", bundle.history.getAllHistory().first().single().translatedText)
    }

    @Test
    fun `cancelling a stream keeps partial text and skips history`() = runTest {
        val bundle = createViewModel(
            engine = FakeStreamingEngine(listOf("你", "好"), stallAfterFirst = true)
        )
        advanceUntilIdle()

        bundle.viewModel.onInputChange("Hello")
        bundle.viewModel.onModeChange(TranslationMode.NATURAL)
        bundle.viewModel.onTranslateClick()
        // Run only tasks scheduled at the current virtual time: the first
        // delta arrives, then the stream stalls on its delay.
        runCurrent()

        assertTrue(bundle.viewModel.uiState.value.isStreaming)
        assertEquals("你", bundle.viewModel.uiState.value.streamingText)

        bundle.viewModel.onCancelStreaming()
        advanceUntilIdle()

        val state = bundle.viewModel.uiState.value
        assertFalse(state.isStreaming)
        assertFalse(state.isTranslating)
        assertTrue(bundle.history.getAllHistory().first().isEmpty())
    }

    @Test
    fun `stream failure surfaces a friendly error`() = runTest {
        val engine = object : TranslationEngine,
            com.lingoflow.app.domain.engine.StreamingTranslationEngine {
            override val status: StateFlow<TranslationStatus> =
                MutableStateFlow(TranslationStatus.IDLE)

            override suspend fun translate(
                request: TranslationRequest
            ): Result<TranslationResponse> = Result.failure(
                com.lingoflow.app.domain.model.TranslationException("boom")
            )

            override fun translateStream(
                request: TranslationRequest
            ): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.flow {
                throw com.lingoflow.app.domain.model.TranslationException(
                    "Network error. Please check your connection."
                )
            }
        }
        val bundle = createViewModel(engine = engine)
        advanceUntilIdle()

        bundle.viewModel.onInputChange("Hello")
        bundle.viewModel.onModeChange(TranslationMode.FORMAL)
        bundle.viewModel.onTranslateClick()
        advanceUntilIdle()

        val state = bundle.viewModel.uiState.value
        assertFalse(state.isStreaming)
        assertEquals("Network error. Please check your connection.", state.errorMessage)
    }
    @Test
    fun `single english word to chinese loads dictionary block`() = runTest {
        val provider = com.lingoflow.app.data.llm.FakeLlmProvider(
            chatResult = com.lingoflow.app.domain.model.llm.ChatResponse(
                content = """{"word": "consider", "entries": [{"partOfSpeech": "vt.", "meanings": ["考虑", "认为"]}], "example": "I will consider it.", "exampleTranslation": "我会考虑的。"}""",
                finishReason = "stop",
                usage = null
            )
        )
        val settings = FakeSettingsRepository(
            com.lingoflow.app.domain.model.settings.AppSettings(
                activeLlmProviderId = com.lingoflow.app.domain.model.llm.LlmProviderId.DEEPSEEK,
                llmProviders = mapOf(
                    com.lingoflow.app.domain.model.llm.LlmProviderId.DEEPSEEK to
                        com.lingoflow.app.domain.model.settings.ProviderConfig(
                            providerId = com.lingoflow.app.domain.model.llm.LlmProviderId.DEEPSEEK,
                            apiKey = "sk-test",
                            baseUrl = null,
                            model = "deepseek-chat"
                        )
                ),
                dictionaryApiKey = ""
            )
        )
        val lookup = com.lingoflow.app.domain.usecase.LookupWordUseCase(settings) { provider }
        val bundle = createViewModel(settings = settings, lookup = lookup)
        advanceUntilIdle()

        bundle.viewModel.onInputChange("consider")
        bundle.viewModel.onTranslateClick()
        advanceUntilIdle()

        val info = bundle.viewModel.uiState.value.wordLookup
        assertNotNull(info)
        assertEquals("consider", info!!.word)
        assertEquals(listOf("考虑", "认为"), info.entries[0].meanings)
        // ML Kit direct translation is still shown above.
        assertTrue(bundle.viewModel.uiState.value.translationResponse is TranslationResponse.Standard)
    }

    @Test
    fun `sentence translation does not trigger word lookup`() = runTest {
        val bundle = createViewModel()
        advanceUntilIdle()

        bundle.viewModel.onInputChange("Hello world")
        bundle.viewModel.onTranslateClick()
        advanceUntilIdle()

        assertNull(bundle.viewModel.uiState.value.wordLookup)
    }
    @Test
    fun `word lookup loading flag settles after completion`() = runTest {
        val bundle = createViewModel()
        advanceUntilIdle()

        bundle.viewModel.onInputChange("consider")
        bundle.viewModel.onTranslateClick()
        advanceUntilIdle()

        assertFalse(bundle.viewModel.uiState.value.wordLookupLoading)
        assertNull(bundle.viewModel.uiState.value.wordLookup)
    }

    @Test
    fun `cancelling a one-shot translation restores idle state`() = runTest {
        // LLM one-shot (LEARNING) that never answers: cancel must restore idle.
        val stallingEngine = object : TranslationEngine {
            override val status: StateFlow<TranslationStatus> =
                MutableStateFlow(TranslationStatus.IDLE)

            override suspend fun translate(
                request: TranslationRequest
            ): Result<TranslationResponse> {
                kotlinx.coroutines.delay(120_000)
                error("unreachable")
            }
        }
        val bundle = createViewModel(engine = stallingEngine)
        advanceUntilIdle()

        bundle.viewModel.onInputChange("天若有情天亦老")
        bundle.viewModel.onModeChange(TranslationMode.LEARNING)
        bundle.viewModel.onTranslateClick()
        runCurrent()

        assertTrue(bundle.viewModel.uiState.value.isTranslating)

        bundle.viewModel.onCancelTranslation()
        advanceUntilIdle()

        val state = bundle.viewModel.uiState.value
        assertFalse(state.isTranslating)
        assertNull(state.translationResponse)
        assertTrue(bundle.history.getAllHistory().first().isEmpty())
    }

    @Test
    fun `hung word lookup is capped by timeout and loading settles`() = runTest {
        // Lookup that never answers: the 20s guard must settle the UI.
        val hangingLookup = object : com.lingoflow.app.domain.usecase.LookupWordUseCase(
            FakeSettingsRepository(),
            { com.lingoflow.app.data.llm.FakeLlmProvider() }
        ) {
            override suspend fun invoke(
                word: String
            ): Result<com.lingoflow.app.domain.model.dictionary.WordLookupInfo> {
                kotlinx.coroutines.delay(120_000)
                error("unreachable")
            }
        }
        val bundle = createViewModel(lookup = hangingLookup)
        advanceUntilIdle()

        bundle.viewModel.onInputChange("consider")
        bundle.viewModel.onTranslateClick()
        advanceUntilIdle()

        val state = bundle.viewModel.uiState.value
        assertFalse(state.wordLookupLoading)
        assertNull(state.wordLookup)
        // ML Kit direct translation is unaffected.
        assertTrue(state.translationResponse is TranslationResponse.Standard)
    }
}
