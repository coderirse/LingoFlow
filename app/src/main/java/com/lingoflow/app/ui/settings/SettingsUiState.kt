package com.lingoflow.app.ui.settings

import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.settings.AppSettings

/** UI state for the settings screen. */
data class SettingsUiState(
    val settings: AppSettings = AppSettings(
        activeLlmProviderId = LlmProviderId.DEEPSEEK,
        llmProviders = emptyMap(),
        dictionaryApiKey = ""
    ),
    val isLoading: Boolean = true,
    val saveSuccess: Boolean = false,
    val error: String? = null
)
