package com.lingoflow.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Entry point for the settings destination: wires [SettingsViewModel] to [SettingsScreen]. */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onProviderChange = viewModel::updateProvider,
        onApiKeyChange = viewModel::updateApiKey,
        onBaseUrlChange = viewModel::updateBaseUrl,
        onModelChange = viewModel::updateModel,
        onTemperatureChange = viewModel::updateTemperature,
        onDictionaryApiKeyChange = viewModel::updateDictionaryApiKey,
        onDefaultModeChange = viewModel::updateDefaultMode,
        onSaveClick = viewModel::saveSettings,
        onSaveSuccessConsumed = viewModel::consumeSaveSuccess,
        onCheckUpdates = viewModel::checkForUpdates,
        onThemeModeChange = viewModel::updateThemeMode,
        onAppLanguageChange = viewModel::updateAppLanguage,
        onInterfaceStyleChange = viewModel::updateInterfaceStyle
    )
}
