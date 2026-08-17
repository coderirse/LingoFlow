package com.lingoflow.app.ui.settings

import com.lingoflow.app.data.update.ReleaseInfo
import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.settings.AppSettings

/** Progress of a "check for updates" request. */
sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class Available(val release: ReleaseInfo) : UpdateCheckState
    data object Failed : UpdateCheckState
}

/** UI state for the settings screen. */
data class SettingsUiState(
    val settings: AppSettings = AppSettings(
        activeLlmProviderId = LlmProviderId.DEEPSEEK,
        llmProviders = emptyMap(),
        dictionaryApiKey = ""
    ),
    val isLoading: Boolean = true,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val updateCheck: UpdateCheckState = UpdateCheckState.Idle
)
