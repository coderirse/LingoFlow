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
        tts: FakeTtsEngine = FakeTtsEngine()
    ) = ViewModelBundle(
        viewModel = HomeViewModel(TranslateTextUseCase(engine), settings, history, tts),
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
    fun `speak reads the translation in the target language`() = runTest {
        val bundle = createViewModel()
        bundle.viewModel.onInputChange("Hello")
        bundle.viewModel.onTranslateClick()
        advanceUntilIdle()

        bundle.viewModel.onSpeakClick()

        assertEquals(listOf("你好"), bundle.tts.spoken)
    }
}
