package com.lingoflow.app.ui.home

import com.lingoflow.app.data.repository.FakeSettingsRepository
import com.lingoflow.app.data.translator.FakeTranslator
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        settings: FakeSettingsRepository = FakeSettingsRepository()
    ) = HomeViewModel(TranslateTextUseCase(engine), settings)

    @Test
    fun `initial state is idle with auto source and chinese target`() = runTest {
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
        viewModel.onInputChange("Hello")
        assertEquals("Hello", viewModel.uiState.value.inputText)
    }

    @Test
    fun `translate succeeds and exposes the result`() = runTest {
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
        viewModel.onTranslateClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isTranslating)
        assertNull(state.translationResponse)
    }

    @Test
    fun `translator failure surfaces a friendly error`() = runTest {
        val viewModel = createViewModel(engine = FailingEngine())
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
        val viewModel = createViewModel()
        viewModel.onSourceLanguageChange(Language.ENGLISH)
        viewModel.onTargetLanguageChange(Language.JAPANESE)
        viewModel.onSwapLanguages()

        val state = viewModel.uiState.value
        assertEquals(Language.JAPANESE, state.sourceLanguage)
        assertEquals(Language.ENGLISH, state.targetLanguage)
    }

    @Test
    fun `swap with auto source never makes target auto`() {
        val viewModel = createViewModel()
        viewModel.onSourceLanguageChange(Language.AUTO)
        viewModel.onTargetLanguageChange(Language.CHINESE)
        viewModel.onSwapLanguages()

        val state = viewModel.uiState.value
        assertEquals(Language.CHINESE, state.sourceLanguage)
        assertEquals(Language.CHINESE, state.targetLanguage)
    }

    @Test
    fun `clear input also clears result and error`() = runTest {
        val viewModel = createViewModel()
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
        val viewModel = createViewModel(settings = settings)
        advanceUntilIdle()

        assertEquals(TranslationMode.NATURAL, viewModel.uiState.value.translationMode)
    }

    @Test
    fun `mode change updates state`() = runTest {
        val viewModel = createViewModel()
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
        val viewModel = createViewModel(engine = engine)
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
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onWordClick("Hello,")

        assertEquals("hello,", viewModel.uiState.value.lookupWord)
    }

    @Test
    fun `consuming the lookup word clears it`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onWordClick("hello")
        assertEquals("hello", viewModel.uiState.value.lookupWord)

        viewModel.consumeLookupWord()
        assertNull(viewModel.uiState.value.lookupWord)
    }

    @Test
    fun `blank word click is ignored`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onWordClick("   ")

        assertNull(viewModel.uiState.value.lookupWord)
    }
}
