package com.lingoflow.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingoflow.app.data.update.UpdateChecker
import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.settings.AppLanguage
import com.lingoflow.app.domain.model.settings.AppSettings
import com.lingoflow.app.domain.model.settings.InterfaceStyle
import com.lingoflow.app.domain.model.settings.ProviderConfig
import com.lingoflow.app.domain.model.settings.ThemeMode
import com.lingoflow.app.domain.model.translation.TranslationErrors
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val updateChecker: UpdateChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * Baseline of what is actually persisted. Appearance fields are applied
     * and persisted instantly; every other edit lives only in the working
     * copy until [saveSettings] is pressed. [isDirty] compares the working
     * copy against this snapshot (appearance fields excluded, since they can
     * never be "pending").
     */
    private var saved: AppSettings? = null

    init {
        viewModelScope.launch {
            try {
                val settings = settingsRepository.getSettings()
                saved = settings
                _uiState.update { it.copy(settings = settings, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = ErrorKeys.LOAD_FAILED)
                }
            }
        }
    }

    /** Switches the active provider, creating a default config if never edited. */
    fun updateProvider(providerId: LlmProviderId) {
        mutateSettings { settings ->
            val config = settings.llmProviders[providerId] ?: ProviderConfig(
                providerId = providerId,
                apiKey = "",
                baseUrl = null,
                model = providerId.defaultModel
            )
            settings.copy(
                activeLlmProviderId = providerId,
                llmProviders = settings.llmProviders + (providerId to config)
            )
        }
    }

    fun updateApiKey(key: String) = mutateActiveProvider { it.copy(apiKey = key) }

    fun updateBaseUrl(url: String) =
        mutateActiveProvider { it.copy(baseUrl = url.ifBlank { null }) }

    fun updateModel(model: String) = mutateActiveProvider { it.copy(model = model) }

    fun updateTemperature(temp: Float) =
        mutateActiveProvider { it.copy(temperature = temp) }

    fun updateDictionaryApiKey(key: String) =
        mutateSettings { it.copy(dictionaryApiKey = key) }

    fun updateDefaultMode(mode: TranslationMode) =
        mutateSettings { it.copy(defaultTranslationMode = mode) }

    /**
     * Appearance changes preview instantly AND persist instantly — but they
     * are persisted onto the last SAVED snapshot, never onto the working
     * copy. Otherwise a half-typed API key would silently ride along with a
     * theme switch.
     */
    fun updateThemeMode(mode: ThemeMode) =
        applyAppearance { it.copy(themeMode = mode) }

    fun updateInterfaceStyle(style: InterfaceStyle) =
        applyAppearance { it.copy(interfaceStyle = style) }

    fun updateAppLanguage(language: AppLanguage) =
        applyAppearance { it.copy(appLanguage = language) }

    fun saveSettings() {
        val snapshot = _uiState.value.settings
        val error = validate(snapshot)
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                settingsRepository.saveSettings(snapshot)
                saved = snapshot
                _uiState.update {
                    it.copy(saveSuccess = true, isDirty = false, isSaving = false, error = null)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = ErrorKeys.SAVE_FAILED)
                }
            }
        }
    }

    /** Queries GitHub for the newest release and compares it to this build. */
    fun checkForUpdates() {
        if (_uiState.value.updateCheck == UpdateCheckState.Checking) return
        viewModelScope.launch {
            _uiState.update { it.copy(updateCheck = UpdateCheckState.Checking) }
            updateChecker.checkLatestRelease()
                .onSuccess { release ->
                    _uiState.update {
                        it.copy(
                            updateCheck = if (release.isNewerThanInstalled) {
                                UpdateCheckState.Available(release)
                            } else {
                                UpdateCheckState.UpToDate
                            }
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(updateCheck = UpdateCheckState.Failed) }
                }
        }
    }

    /** Called by the UI after the "Settings saved" Snackbar has been shown. */
    fun consumeSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    /** Called by the UI after the localized error message has been shown. */
    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * The active provider's resolved base URL must be a valid http(s) URL —
     * blank CUSTOM URLs or typo'd hosts would only fail deep inside OkHttp
     * with a generic "Translation failed".
     */
    private fun validate(settings: AppSettings): String? {
        val config = settings.llmProviders[settings.activeLlmProviderId]
            ?: return TranslationErrors.INVALID_BASE_URL
        val raw = config.baseUrl?.ifBlank { null } ?: config.providerId.defaultBaseUrl
        val url = raw.trim().toHttpUrlOrNull() ?: return TranslationErrors.INVALID_BASE_URL
        if (url.scheme !in setOf("http", "https")) {
            return TranslationErrors.INVALID_BASE_URL
        }
        return null
    }

    private fun mutateSettings(transform: (AppSettings) -> AppSettings) {
        _uiState.update { state ->
            val updated = transform(state.settings)
            state.copy(settings = updated, isDirty = isDirty(updated))
        }
    }

    private fun mutateActiveProvider(transform: (ProviderConfig) -> ProviderConfig) {
        mutateSettings { settings ->
            val id = settings.activeLlmProviderId
            val current = settings.llmProviders[id] ?: ProviderConfig(
                providerId = id,
                apiKey = "",
                baseUrl = null,
                model = id.defaultModel
            )
            settings.copy(
                llmProviders = settings.llmProviders + (id to transform(current))
            )
        }
    }

    /**
     * Applies an appearance change to the working copy (instant preview) and
     * persists the same change on top of the last SAVED snapshot, so pending
     * edits stay pending instead of riding along.
     */
    private fun applyAppearance(transform: (AppSettings) -> AppSettings) {
        mutateSettings(transform)
        val base = saved ?: return
        val toPersist = transform(base)
        saved = toPersist
        viewModelScope.launch {
            try {
                settingsRepository.saveSettings(toPersist)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = ErrorKeys.SAVE_FAILED) }
            }
        }
    }

    private fun isDirty(working: AppSettings): Boolean {
        val baseline = saved ?: return false
        return working.copy(
            themeMode = baseline.themeMode,
            appLanguage = baseline.appLanguage,
            interfaceStyle = baseline.interfaceStyle
        ) != baseline
    }

    /** Error keys that are settings-specific (localized by the screen). */
    object ErrorKeys {
        const val LOAD_FAILED = "err_settings_load_failed"
        const val SAVE_FAILED = "err_settings_save_failed"
    }
}
