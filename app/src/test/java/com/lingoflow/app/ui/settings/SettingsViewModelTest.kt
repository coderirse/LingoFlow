package com.lingoflow.app.ui.settings

import com.lingoflow.app.data.repository.FakeSettingsRepository
import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakeSettingsRepository = FakeSettingsRepository()
    ) = SettingsViewModel(repository) to repository

    @Test
    fun `loads default settings on init`() = runTest {
        val (viewModel, _) = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(LlmProviderId.DEEPSEEK, state.settings.activeLlmProviderId)
        assertEquals(TranslationMode.STANDARD, state.settings.defaultTranslationMode)
        assertEquals(0.7f, state.settings.llmProviders[LlmProviderId.DEEPSEEK]?.temperature)
    }

    @Test
    fun `switching provider updates active provider and fills defaults`() = runTest {
        val (viewModel, _) = createViewModel()
        advanceUntilIdle()

        viewModel.updateProvider(LlmProviderId.OPENAI)

        val settings = viewModel.uiState.value.settings
        assertEquals(LlmProviderId.OPENAI, settings.activeLlmProviderId)
        val config = settings.llmProviders[LlmProviderId.OPENAI]!!
        assertEquals(LlmProviderId.OPENAI.defaultModel, config.model)
        assertNull(config.baseUrl)
    }

    @Test
    fun `temperature update targets the active provider`() = runTest {
        val (viewModel, _) = createViewModel()
        advanceUntilIdle()

        viewModel.updateTemperature(1.2f)

        val config = viewModel.uiState.value.settings
            .llmProviders[LlmProviderId.DEEPSEEK]!!
        assertEquals(1.2f, config.temperature)
    }

    @Test
    fun `saveSettings persists through the repository and flags success`() = runTest {
        val (viewModel, repository) = createViewModel()
        advanceUntilIdle()

        viewModel.updateApiKey("sk-test")
        viewModel.updateDefaultMode(TranslationMode.LEARNING)
        viewModel.saveSettings()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveSuccess)
        val saved = repository.savedSettings!!
        assertEquals("sk-test", saved.llmProviders[LlmProviderId.DEEPSEEK]?.apiKey)
        assertEquals(TranslationMode.LEARNING, saved.defaultTranslationMode)
    }

    @Test
    fun `consuming save success clears the flag`() = runTest {
        val (viewModel, _) = createViewModel()
        advanceUntilIdle()

        viewModel.saveSettings()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.saveSuccess)

        viewModel.consumeSaveSuccess()
        assertFalse(viewModel.uiState.value.saveSuccess)
    }

    @Test
    fun `blank base url resets to null so the default applies`() = runTest {
        val (viewModel, _) = createViewModel()
        advanceUntilIdle()

        viewModel.updateBaseUrl("https://proxy.example.com/v1")
        assertEquals(
            "https://proxy.example.com/v1",
            viewModel.uiState.value.settings.llmProviders[LlmProviderId.DEEPSEEK]?.baseUrl
        )

        viewModel.updateBaseUrl("   ")
        assertNull(
            viewModel.uiState.value.settings.llmProviders[LlmProviderId.DEEPSEEK]?.baseUrl
        )
    }
}
