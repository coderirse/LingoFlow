package com.lingoflow.app.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.settings.AppLanguage
import com.lingoflow.app.domain.model.settings.ThemeMode
import com.lingoflow.app.ui.i18n.LocalStrings
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.model.translation.displayName
import com.lingoflow.app.ui.theme.LingoFlowTheme
import java.util.Locale
import kotlin.math.roundToInt

/** Stateless settings screen driven by [SettingsUiState]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onProviderChange: (LlmProviderId) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onDictionaryApiKeyChange: (String) -> Unit,
    onDefaultModeChange: (TranslationMode) -> Unit,
    onSaveClick: () -> Unit,
    onSaveSuccessConsumed: () -> Unit,
    onCheckUpdates: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(strings.settingsSaved)
            onSaveSuccessConsumed()
        }
    }

    val activeProviderId = uiState.settings.activeLlmProviderId
    val activeConfig = uiState.settings.llmProviders[activeProviderId]

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(strings.settings) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 20.dp,
                vertical = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(title = strings.llmProvider) {
                    ProviderDropdown(
                        selected = activeProviderId,
                        onSelected = onProviderChange
                    )
                    SecretTextField(
                        value = activeConfig?.apiKey ?: "",
                        onValueChange = onApiKeyChange,
                        label = strings.apiKey
                    )
                    OutlinedTextField(
                        value = activeConfig?.baseUrl ?: "",
                        onValueChange = onBaseUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.baseUrl) },
                        placeholder = { Text(activeProviderId.defaultBaseUrl) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                    OutlinedTextField(
                        value = activeConfig?.model ?: "",
                        onValueChange = onModelChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.model) },
                        placeholder = { Text(activeProviderId.defaultModel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                    TemperatureRow(
                        temperature = activeConfig?.temperature ?: 0.7f,
                        onTemperatureChange = onTemperatureChange
                    )
                }
            }

            item {
                SettingsSection(title = strings.dictionarySection) {
                    SecretTextField(
                        value = uiState.settings.dictionaryApiKey,
                        onValueChange = onDictionaryApiKeyChange,
                        label = strings.mwApiKey
                    )
                }
            }

            item {
                SettingsSection(title = strings.translationSection) {
                    TranslationModeSelector(
                        selected = uiState.settings.defaultTranslationMode,
                        onSelected = onDefaultModeChange
                    )
                }
            }

            item {
                SettingsSection(title = strings.appearance) {
                    ThemeModeDropdown(
                        selected = uiState.settings.themeMode,
                        onSelected = onThemeModeChange
                    )
                    AppLanguageDropdown(
                        selected = uiState.settings.appLanguage,
                        onSelected = onAppLanguageChange
                    )
                }
            }

            item {
                SettingsSection(title = strings.about) {
                    Text(
                        text = strings.currentVersion + com.lingoflow.app.BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    UpdateCheckRow(
                        updateCheck = uiState.updateCheck,
                        onCheckUpdates = onCheckUpdates
                    )
                }
            }

            item {
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = strings.saveSettings,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun UpdateCheckRow(
    updateCheck: UpdateCheckState,
    onCheckUpdates: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onCheckUpdates,
            enabled = updateCheck != UpdateCheckState.Checking,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (updateCheck == UpdateCheckState.Checking) {
                    strings.checking
                } else {
                    strings.checkForUpdates
                }
            )
        }

        when (updateCheck) {
            is UpdateCheckState.UpToDate -> Text(
                text = strings.upToDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            is UpdateCheckState.Available -> {
                Text(
                    text = strings.newVersionAvailable(updateCheck.release.tagName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = {
                        val url = updateCheck.release.apkDownloadUrl
                            ?: updateCheck.release.htmlUrl
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, url.toUri())
                        )
                    }
                ) {
                    Text(strings.downloadVersion(updateCheck.release.tagName))
                }
            }

            is UpdateCheckState.Failed -> Text(
                text = strings.updateFailed,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )

            else -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderDropdown(
    selected: LlmProviderId,
    onSelected: (LlmProviderId) -> Unit
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = providerDisplayName(selected),
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            label = { Text(strings.provider) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            LlmProviderId.entries.forEach { providerId ->
                DropdownMenuItem(
                    text = { Text(providerDisplayName(providerId)) },
                    onClick = {
                        onSelected(providerId)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SecretTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    val strings = LocalStrings.current
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                Text(if (passwordVisible) strings.hide else strings.show)
            }
        }
    )
}

@Composable
private fun TemperatureRow(
    temperature: Float,
    onTemperatureChange: (Float) -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = strings.temperature,
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = temperature,
            onValueChange = { raw ->
                // Snap to 0.1 increments for a predictable 0.0–2.0 scale.
                onTemperatureChange((raw * 10).roundToInt() / 10f)
            },
            valueRange = 0f..2f,
            steps = 20,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = String.format(Locale.US, "%.2f", temperature),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslationModeSelector(
    selected: TranslationMode,
    onSelected: (TranslationMode) -> Unit
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(strings.defaultMode) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TranslationMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.displayName) },
                    onClick = {
                        onSelected(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeDropdown(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val label = when (selected) {
        ThemeMode.SYSTEM -> strings.themeSystem
        ThemeMode.LIGHT -> strings.themeLight
        ThemeMode.DARK -> strings.themeDark
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(strings.theme) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ThemeMode.entries.forEach { mode ->
                val text = when (mode) {
                    ThemeMode.SYSTEM -> strings.themeSystem
                    ThemeMode.LIGHT -> strings.themeLight
                    ThemeMode.DARK -> strings.themeDark
                }
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelected(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppLanguageDropdown(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(strings.language) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        onSelected(language)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun providerDisplayName(providerId: LlmProviderId): String = when (providerId) {
    LlmProviderId.DEEPSEEK -> "DeepSeek"
    LlmProviderId.OPENAI -> "OpenAI"
    LlmProviderId.ANTHROPIC -> "Anthropic"
    LlmProviderId.GEMINI -> "Gemini"
    LlmProviderId.MOONSHOT -> "Moonshot"
    LlmProviderId.CUSTOM -> "Custom"
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    LingoFlowTheme {
        SettingsScreen(
            uiState = SettingsUiState(isLoading = false),
            onBack = {},
            onProviderChange = {},
            onApiKeyChange = {},
            onBaseUrlChange = {},
            onModelChange = {},
            onTemperatureChange = {},
            onDictionaryApiKeyChange = {},
            onDefaultModeChange = {},
            onSaveClick = {},
            onSaveSuccessConsumed = {},
            onCheckUpdates = {},
            onThemeModeChange = {},
            onAppLanguageChange = {}
        )
    }
}
