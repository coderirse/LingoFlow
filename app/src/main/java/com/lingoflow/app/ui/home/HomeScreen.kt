package com.lingoflow.app.ui.home

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lingoflow.app.R
import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.TranslationStatus
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.model.translation.TranslationResponse
import com.lingoflow.app.ui.dictionary.DictionaryBottomSheet
import com.lingoflow.app.ui.theme.LingoFlowOnSurface
import com.lingoflow.app.ui.theme.LingoFlowOnSurfaceVariant
import com.lingoflow.app.ui.theme.LingoFlowPrimary
import com.lingoflow.app.ui.theme.LingoFlowSecondary
import com.lingoflow.app.ui.theme.LingoFlowSurface
import com.lingoflow.app.ui.theme.LingoFlowSurfaceElevated
import com.lingoflow.app.ui.theme.LingoFlowTheme
import kotlinx.coroutines.launch

/** Entry point for the home destination: wires [HomeViewModel] to [HomeScreen]. */
@Composable
fun HomeRoute(
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onInputChange = viewModel::onInputChange,
        onSourceLanguageChange = viewModel::onSourceLanguageChange,
        onTargetLanguageChange = viewModel::onTargetLanguageChange,
        onSwapLanguages = viewModel::onSwapLanguages,
        onModeChange = viewModel::onModeChange,
        onTranslateClick = viewModel::onTranslateClick,
        onClearInput = viewModel::onClearInput,
        onSnackbarShown = viewModel::onSnackbarShown,
        onWordClick = viewModel::onWordClick,
        onLookupWordConsumed = viewModel::consumeLookupWord,
        onSettingsClick = onNavigateToSettings
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onInputChange: (String) -> Unit,
    onSourceLanguageChange: (Language) -> Unit,
    onTargetLanguageChange: (Language) -> Unit,
    onSwapLanguages: () -> Unit,
    onModeChange: (TranslationMode) -> Unit,
    onTranslateClick: () -> Unit,
    onClearInput: () -> Unit,
    onSnackbarShown: () -> Unit,
    onWordClick: (String) -> Unit,
    onLookupWordConsumed: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val context = LocalContext.current

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            onSnackbarShown()
        }
    }

    // Tap-to-lookup: a word was tapped in the translation result.
    uiState.lookupWord?.let { word ->
        DictionaryBottomSheet(
            initialWord = word,
            onDismiss = onLookupWordConsumed,
            onGoToSettings = {
                onLookupWordConsumed()
                onSettingsClick()
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HomeTopBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onSettingsClick = onSettingsClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> TranslateTab(
                uiState = uiState,
                onInputChange = onInputChange,
                onSourceLanguageChange = onSourceLanguageChange,
                onTargetLanguageChange = onTargetLanguageChange,
                onSwapLanguages = onSwapLanguages,
                onModeChange = onModeChange,
                onTranslateClick = onTranslateClick,
                onClearInput = onClearInput,
                onCopy = {
                    coroutineScope.launch {
                        clipboard.setClipEntry(
                            androidx.compose.ui.platform.ClipEntry(
                                ClipData.newPlainText(
                                    "LingoFlow translation",
                                    uiState.translatedText
                                )
                            )
                        )
                        snackbarHostState.showSnackbar("Copied")
                    }
                },
                onShare = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, uiState.translatedText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                },
                onPaste = {
                    coroutineScope.launch {
                        val text = clipboard.getClipEntry()
                            ?.clipData
                            ?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)
                            ?.text
                            ?.toString()
                        if (!text.isNullOrEmpty()) onInputChange(text)
                    }
                },
                onWordClick = onWordClick,
                onGoToSettings = onSettingsClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            )

            1 -> PlaceholderScreen(
                title = "History",
                icon = Icons.Default.DateRange,
                modifier = Modifier.padding(innerPadding)
            )

            else -> PlaceholderScreen(
                title = "Learning",
                icon = Icons.Default.Star,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Top bar with tab navigation
// ---------------------------------------------------------------------------

@Composable
private fun HomeTopBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onSettingsClick: () -> Unit
) {
    val tabs = listOf("Translate", "History", "Learning")

    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                HomeTab(
                    title = title,
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = LingoFlowOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HomeTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) LingoFlowOnSurface else LingoFlowOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(3.dp)
        ) {
            if (selected) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = LingoFlowPrimary
                ) {}
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Translate tab
// ---------------------------------------------------------------------------

@Composable
private fun TranslateTab(
    uiState: HomeUiState,
    onInputChange: (String) -> Unit,
    onSourceLanguageChange: (Language) -> Unit,
    onTargetLanguageChange: (Language) -> Unit,
    onSwapLanguages: () -> Unit,
    onModeChange: (TranslationMode) -> Unit,
    onTranslateClick: () -> Unit,
    onClearInput: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onPaste: () -> Unit,
    onWordClick: (String) -> Unit,
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InputCard(
                inputText = uiState.inputText,
                onInputChange = onInputChange,
                onClearInput = onClearInput,
                onPaste = onPaste
            )
        }
        item {
            LanguageBar(
                sourceLanguage = uiState.sourceLanguage,
                targetLanguage = uiState.targetLanguage,
                enabled = !uiState.isTranslating,
                onSourceLanguageChange = onSourceLanguageChange,
                onTargetLanguageChange = onTargetLanguageChange,
                onSwapLanguages = onSwapLanguages
            )
        }
        item {
            ModeSelector(
                selected = uiState.translationMode,
                onModeChange = onModeChange
            )
        }
        item {
            TranslateButton(
                uiState = uiState,
                onTranslateClick = onTranslateClick
            )
        }
        item {
            TranslationResultCard(
                uiState = uiState,
                onCopy = onCopy,
                onShare = onShare,
                onWordClick = onWordClick
            )
        }
        item {
            DictionaryLookupSection(onGoToSettings = onGoToSettings)
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
        item { BrandingFooter() }
    }
}

@Composable
private fun InputCard(
    inputText: String,
    onInputChange: (String) -> Unit,
    onClearInput: () -> Unit,
    onPaste: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LingoFlowSurfaceElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BasicTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 150.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = LingoFlowOnSurface
                )
            ) { innerTextField ->
                Box {
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Enter text to translate...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LingoFlowOnSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPaste) {
                    Text("Paste", color = LingoFlowPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = onClearInput) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear input",
                                tint = LingoFlowOnSurfaceVariant
                            )
                        }
                    }
                    // Voice input arrives in a later phase.
                    IconButton(onClick = {}, enabled = false) {
                        Text("🎤")
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageBar(
    sourceLanguage: Language,
    targetLanguage: Language,
    enabled: Boolean,
    onSourceLanguageChange: (Language) -> Unit,
    onTargetLanguageChange: (Language) -> Unit,
    onSwapLanguages: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LingoFlowSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguagePicker(
                selected = sourceLanguage,
                options = Language.entries,
                enabled = enabled,
                onSelected = onSourceLanguageChange,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSwapLanguages, enabled = enabled) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Swap languages",
                    tint = LingoFlowPrimary
                )
            }
            LanguagePicker(
                selected = targetLanguage,
                options = Language.targetSelectable,
                enabled = enabled,
                onSelected = onTargetLanguageChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LanguagePicker(
    selected: Language,
    options: List<Language>,
    enabled: Boolean,
    onSelected: (Language) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        TextButton(onClick = { expanded = true }, enabled = enabled) {
            Text(
                text = selected.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = LingoFlowOnSurface,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = LingoFlowOnSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { language ->
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

@Composable
private fun ModeSelector(
    selected: TranslationMode,
    onModeChange: (TranslationMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TranslationMode.entries.forEach { mode ->
            ModeCard(
                mode = mode,
                isSelected = mode == selected,
                onClick = { onModeChange(mode) }
            )
        }
    }
}

@Composable
private fun ModeCard(
    mode: TranslationMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = 72.dp)
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) LingoFlowPrimary else LingoFlowSurface
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = mode.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) LingoFlowOnSurface else LingoFlowOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val TranslationMode.displayName: String
    get() = when (this) {
        TranslationMode.STANDARD -> "Standard"
        TranslationMode.NATURAL -> "Natural"
        TranslationMode.CONCISE -> "Concise"
        TranslationMode.FORMAL -> "Formal"
        TranslationMode.LEARNING -> "Learning"
    }

@Composable
private fun TranslateButton(
    uiState: HomeUiState,
    onTranslateClick: () -> Unit
) {
    Button(
        onClick = onTranslateClick,
        enabled = uiState.inputText.isNotBlank() && !uiState.isTranslating,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LingoFlowPrimary)
    ) {
        if (uiState.isTranslating) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = LingoFlowOnSurface
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                when (uiState.status) {
                    TranslationStatus.PREPARING_MODEL -> "Preparing translation model..."
                    else -> "Translating..."
                }
            )
        } else {
            Text("Translate", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TranslationResultCard(
    uiState: HomeUiState,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onWordClick: (String) -> Unit
) {
    var isFavorite by remember { mutableStateOf(false) }
    val response = uiState.translationResponse
    val hasResult = uiState.translatedText.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LingoFlowSurfaceElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Translation",
                    style = MaterialTheme.typography.titleMedium,
                    color = LingoFlowPrimary
                )
                // TTS playback arrives in a later phase.
                IconButton(onClick = {}, enabled = false) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Speak (coming soon)"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                uiState.errorMessage != null -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                response is TranslationResponse.Learning -> {
                    SelectionContainer {
                        Text(
                            text = response.translatedText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = LingoFlowOnSurface
                        )
                    }
                    response.contextExplanation?.let { explanation ->
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = LingoFlowSurface)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Analysis",
                            color = LingoFlowSecondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (explanation.meaningInContext.isNotBlank()) {
                            Text(
                                text = explanation.meaningInContext,
                                style = MaterialTheme.typography.bodySmall,
                                color = LingoFlowOnSurfaceVariant
                            )
                        }
                        explanation.grammarNote?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = LingoFlowOnSurfaceVariant
                            )
                        }
                        explanation.usageNote?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = LingoFlowOnSurfaceVariant
                            )
                        }
                        if (explanation.synonymsInContext.isNotEmpty()) {
                            Text(
                                text = "Synonyms: " + explanation.synonymsInContext
                                    .joinToString(", ") { it.word },
                                style = MaterialTheme.typography.bodySmall,
                                color = LingoFlowOnSurfaceVariant
                            )
                        }
                    }
                    if (response.dictionaryEntries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Key Words",
                            color = LingoFlowSecondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        response.dictionaryEntries.take(3).forEach { entry ->
                            val meaning = entry.entries.firstOrNull()
                                ?.definitions?.firstOrNull()?.meaning.orEmpty()
                            Text(
                                text = "• ${entry.word} — $meaning",
                                style = MaterialTheme.typography.bodySmall,
                                color = LingoFlowOnSurfaceVariant
                            )
                        }
                    }
                }

                response is TranslationResponse.Standard -> {
                    if (uiState.targetLanguage == Language.ENGLISH) {
                        // English output: tap any word to look it up in the dictionary.
                        ClickableWords(
                            text = response.translatedText,
                            onWordClick = onWordClick
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = response.translatedText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.5
                                ),
                                color = LingoFlowOnSurface
                            )
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Your translation will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LingoFlowOnSurfaceVariant
                        )
                    }
                }
            }

            if (hasResult) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCopy) {
                        Text("Copy", color = LingoFlowPrimary)
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share translation",
                            tint = LingoFlowOnSurfaceVariant
                        )
                    }
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            imageVector = if (isFavorite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = "Favorite",
                            tint = if (isFavorite) {
                                LingoFlowPrimary
                            } else {
                                LingoFlowOnSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Dictionary shortcut + branding
// ---------------------------------------------------------------------------

/** Renders [text] as individually tappable words for dictionary lookup. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClickableWords(
    text: String,
    onWordClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start)
    ) {
        text.split(Regex("\\s+")).forEach { rawWord ->
            val cleanWord = rawWord.trim { !it.isLetterOrDigit() }.lowercase()
            Text(
                text = rawWord,
                modifier = Modifier.clickable(enabled = cleanWord.isNotEmpty()) {
                    onWordClick(cleanWord)
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.5
                ),
                color = LingoFlowOnSurface
            )
        }
    }
}

@Composable
private fun DictionaryLookupSection(
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var word by remember { mutableStateOf("") }
    var showSheet by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LingoFlowSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Dictionary",
                style = MaterialTheme.typography.titleSmall,
                color = LingoFlowSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    placeholder = { Text("English word...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                IconButton(
                    onClick = { showSheet = true },
                    enabled = word.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Look up word",
                        tint = LingoFlowPrimary
                    )
                }
            }
        }
    }

    if (showSheet) {
        DictionaryBottomSheet(
            initialWord = word,
            onDismiss = { showSheet = false },
            onGoToSettings = {
                showSheet = false
                onGoToSettings()
            }
        )
    }
}

@Composable
private fun BrandingFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Copyright © 2026 Stafind. All rights reserved.",
            style = MaterialTheme.typography.bodySmall,
            color = LingoFlowOnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://github.com/coderirse/LingoFlow".toUri()
                )
                context.startActivity(intent)
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_github),
                contentDescription = "GitHub",
                modifier = Modifier.size(20.dp),
                tint = LingoFlowPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "View on GitHub",
                color = LingoFlowPrimary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Placeholder tabs
// ---------------------------------------------------------------------------

@Composable
private fun PlaceholderScreen(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LingoFlowOnSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "$title — Coming Soon",
            style = MaterialTheme.typography.titleMedium,
            color = LingoFlowOnSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    LingoFlowTheme {
        HomeScreen(
            uiState = HomeUiState(
                translationResponse = TranslationResponse.Standard("你好")
            ),
            onInputChange = {},
            onSourceLanguageChange = {},
            onTargetLanguageChange = {},
            onSwapLanguages = {},
            onModeChange = {},
            onTranslateClick = {},
            onClearInput = {},
            onSnackbarShown = {},
            onWordClick = {},
            onLookupWordConsumed = {},
            onSettingsClick = {}
        )
    }
}
