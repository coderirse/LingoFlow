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
    /** True when non-appearance edits are pending and not yet saved. */
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    /** Stable error key (see [com.lingoflow.app.domain.model.translation.TranslationErrors]). */
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val updateCheck: UpdateCheckState = UpdateCheckState.Idle
)
