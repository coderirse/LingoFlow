package com.lingoflow.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingoflow.app.data.update.UpdateChecker
import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.settings.AppLanguage
import com.lingoflow.app.domain.model.settings.AppSettings
import com.lingoflow.app.domain.model.settings.ProviderConfig
import com.lingoflow.app.domain.model.settings.ThemeMode
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val updateChecker: UpdateChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val settings = settingsRepository.getSettings()
                _uiState.update { it.copy(settings = settings, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to load settings.")
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

    /** Theme changes apply instantly, so persist them right away. */
    fun updateThemeMode(mode: ThemeMode) {
        mutateSettings { it.copy(themeMode = mode) }
        viewModelScope.launch {
            settingsRepository.saveSettings(_uiState.value.settings)
        }
    }

    /** Language changes apply instantly, so persist them right away. */
    fun updateAppLanguage(language: AppLanguage) {
        mutateSettings { it.copy(appLanguage = language) }
        viewModelScope.launch {
            settingsRepository.saveSettings(_uiState.value.settings)
        }
    }

    fun saveSettings() {
        val snapshot = _uiState.value.settings
        viewModelScope.launch {
            try {
                settingsRepository.saveSettings(snapshot)
                _uiState.update { it.copy(saveSuccess = true, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save settings.") }
            }
        }
    }

    /** Called by the UI after the "Settings saved" Snackbar has been shown. */
    fun consumeSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
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

    private fun mutateSettings(transform: (AppSettings) -> AppSettings) {
        _uiState.update { state -> state.copy(settings = transform(state.settings)) }
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
}
