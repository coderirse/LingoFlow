package com.lingoflow.app.ui.home

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lingoflow.app.R
import com.lingoflow.app.data.tts.TtsPlaybackState
import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.TranslationStatus
import com.lingoflow.app.domain.model.history.TranslationHistoryItem
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.model.translation.TranslationNotices
import com.lingoflow.app.domain.model.translation.TranslationResponse
import com.lingoflow.app.domain.model.translation.displayName
import com.lingoflow.app.ui.components.BlinkingCursor
import com.lingoflow.app.ui.dictionary.DictionaryBottomSheet
import com.lingoflow.app.ui.dictionary.WordLookupInfoContent
import com.lingoflow.app.ui.dictionary.WordPreviewSheet
import com.lingoflow.app.ui.history.HistoryRoute
import com.lingoflow.app.ui.i18n.AppStrings
import com.lingoflow.app.ui.i18n.LocalStrings
import com.lingoflow.app.ui.learning.LearningRoute
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
        onSpeakClick = viewModel::onSpeakClick,
        onToggleFavoriteTranslation = viewModel::onToggleFavoriteTranslation,
        onCancelStreaming = viewModel::onCancelStreaming,
        onCancelTranslation = viewModel::onCancelTranslation,
        onReuseHistoryItem = viewModel::onReuseHistoryItem,
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
    onSpeakClick: () -> Unit,
    onToggleFavoriteTranslation: () -> Unit,
    onCancelStreaming: () -> Unit,
    onCancelTranslation: () -> Unit,
    onReuseHistoryItem: (TranslationHistoryItem) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val strings = LocalStrings.current
    val context = LocalContext.current

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { raw ->
            snackbarHostState.showSnackbar(localizeNotice(raw, strings))
            onSnackbarShown()
        }
    }

    // Tap-to-lookup, stage one: compact preview card. The two sheets are
    // mutually exclusive — stacking two modal sheets risks a stranded dim
    // window that blacks out the whole screen until the app is killed.
    var fullLookupWord by remember { mutableStateOf<String?>(null) }
    val previewWord = uiState.lookupWord
    if (previewWord != null) {
        WordPreviewSheet(
            word = previewWord,
            onDismiss = onLookupWordConsumed,
            onViewFullDefinition = {
                fullLookupWord = previewWord
                onLookupWordConsumed()
            },
            onGoToSettings = {
                onLookupWordConsumed()
                onSettingsClick()
            }
        )
    } else {
        // Tap-to-lookup, stage two: full dictionary sheet.
        fullLookupWord?.let { word ->
            DictionaryBottomSheet(
                initialWord = word,
                onDismiss = { fullLookupWord = null },
                onGoToSettings = {
                    fullLookupWord = null
                    onSettingsClick()
                }
            )
        }
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
                        snackbarHostState.showSnackbar(strings.copied)
                    }
                },
                onShare = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, uiState.translatedText)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }
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
                onSpeakClick = onSpeakClick,
                onToggleFavoriteTranslation = onToggleFavoriteTranslation,
                onCancelStreaming = onCancelStreaming,
                onCancelTranslation = onCancelTranslation,
                onGoToSettings = onSettingsClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            )

            1 -> HistoryRoute(
                onReuse = { item ->
                    onReuseHistoryItem(item)
                    selectedTab = 0
                },
                modifier = Modifier.padding(innerPadding)
            )

            else -> LearningRoute(
                onGoToSettings = onSettingsClick,
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
    val strings = LocalStrings.current
    val tabs = listOf(strings.tabTranslate, strings.tabHistory, strings.tabLearning)

    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
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
                    contentDescription = strings.settings,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Indicator fades/slides in for the selected tab instead of
        // popping in and out.
        val indicatorAlpha by animateFloatAsState(
            targetValue = if (selected) 1f else 0f,
            animationSpec = tween(durationMillis = 200),
            label = "tabIndicatorAlpha"
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(3.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .alpha(indicatorAlpha),
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.primary
            ) {}
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
    onSpeakClick: () -> Unit,
    onToggleFavoriteTranslation: () -> Unit,
    onCancelStreaming: () -> Unit,
    onCancelTranslation: () -> Unit,
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
                onPaste = onPaste,
                sourceLanguage = uiState.sourceLanguage,
                targetLanguage = uiState.targetLanguage,
                languagesEnabled = !uiState.isTranslating,
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
                onTranslateClick = onTranslateClick,
                onCancelStreaming = onCancelStreaming,
                onCancelTranslation = onCancelTranslation
            )
        }
        item {
            TranslationResultCard(
                uiState = uiState,
                onCopy = onCopy,
                onShare = onShare,
                onWordClick = onWordClick,
                onSpeakClick = onSpeakClick,
                onToggleFavorite = onToggleFavoriteTranslation
            )
        }
        item { LookupEntryBar(onGoToSettings = onGoToSettings) }
        item { Spacer(modifier = Modifier.height(20.dp)) }
        item { BrandingFooter() }
    }
}

@Composable
private fun InputCard(
    inputText: String,
    onInputChange: (String) -> Unit,
    onClearInput: () -> Unit,
    onPaste: () -> Unit,
    sourceLanguage: Language,
    targetLanguage: Language,
    languagesEnabled: Boolean,
    onSourceLanguageChange: (Language) -> Unit,
    onTargetLanguageChange: (Language) -> Unit,
    onSwapLanguages: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Language row lives inside the input card: one less card on screen.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguagePicker(
                    selected = sourceLanguage,
                    options = Language.entries,
                    enabled = languagesEnabled,
                    onSelected = onSourceLanguageChange,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onSwapLanguages,
                    enabled = languagesEnabled,
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Swap languages",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                LanguagePicker(
                    selected = targetLanguage,
                    options = Language.targetSelectable,
                    enabled = languagesEnabled,
                    onSelected = onTargetLanguageChange,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline
            )

            BasicTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 150.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            ) { innerTextField ->
                Box {
                    if (inputText.isEmpty()) {
                        Text(
                            text = strings.enterText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Text(strings.paste, color = MaterialTheme.colorScheme.primary)
                }
                if (inputText.isNotEmpty()) {
                    IconButton(onClick = onClearInput) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = strings.clearInput,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
    // Horizontally scrollable chip row: labels never truncate on narrow
    // screens, and new modes can be added without squeezing the row.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (isSelected) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text(
                text = mode.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TranslateButton(
    uiState: HomeUiState,
    onTranslateClick: () -> Unit,
    onCancelStreaming: () -> Unit,
    onCancelTranslation: () -> Unit
) {
    val strings = LocalStrings.current
    val keyboardController = LocalSoftwareKeyboardController.current
    // Every in-flight translation is cancellable, STANDARD included: short
    // texts finish on-device almost instantly, but long texts go through
    // the LLM and need the same way out as the other modes. Streaming
    // modes cancel the stream; one-shot modes (STANDARD/LEARNING) cancel
    // the request.
    val cancellable = uiState.isStreaming || uiState.isTranslating
    val enabled = cancellable ||
        (uiState.inputText.isNotBlank() && !uiState.isTranslating)
    val gradient = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    )

    Button(
        onClick = {
            when {
                uiState.isStreaming -> onCancelStreaming()
                cancellable -> onCancelTranslation()
                else -> {
                    keyboardController?.hide()
                    onTranslateClick()
                }
            }
        },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (enabled) Modifier.background(gradient) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            when {
                cancellable -> {
                    Text(strings.cancel, style = MaterialTheme.typography.titleMedium)
                }

                uiState.isTranslating -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        when (uiState.status) {
                            TranslationStatus.PREPARING_MODEL -> strings.preparingModel
                            else -> strings.translating
                        }
                    )
                }

                else -> {
                    Text(strings.translate, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun TranslationResultCard(
    uiState: HomeUiState,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onWordClick: (String) -> Unit,
    onSpeakClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val strings = LocalStrings.current
    val response = uiState.translationResponse
    val hasResult = uiState.translatedText.isNotEmpty()
    val isSpeaking = uiState.ttsPlaybackState == TtsPlaybackState.SPEAKING

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        // The one brand moment: a very subtle blue→cyan gradient hairline.
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.translationTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Small tag naming the mode that produced this result —
                    // mirrors the badge the History tab shows per record.
                    uiState.resultMode?.let { mode ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onSpeakClick,
                    enabled = uiState.ttsReady && hasResult
                ) {
                    if (isSpeaking) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pause),
                            contentDescription = strings.pauseTranslation,
                            tint = if (uiState.ttsReady && hasResult) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = strings.speakTranslation,
                            tint = if (uiState.ttsReady && hasResult) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
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
                            text = strings.localizedError(uiState.errorMessage),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                uiState.isStreaming -> {
                    StreamingText(text = uiState.streamingText)
                }

                uiState.isTranslating -> {
                    // One-shot translation in progress (ML Kit STANDARD or
                    // LLM LEARNING): same blinking typewriter cursor as the
                    // streaming modes.
                    Spacer(modifier = Modifier.height(8.dp))
                    BlinkingCursor()
                }

                response is TranslationResponse.Learning -> {
                    if (uiState.targetLanguage == Language.ENGLISH) {
                        // Same tap-to-lookup words as the Standard English result.
                        ClickableWords(
                            text = response.translatedText,
                            onWordClick = onWordClick
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = response.translatedText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 27.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    response.contextExplanation?.let { explanation ->
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.analysis,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (explanation.meaningInContext.isNotBlank()) {
                            Text(
                                text = explanation.meaningInContext,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        explanation.grammarNote?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        explanation.usageNote?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (explanation.synonymsInContext.isNotEmpty()) {
                            Text(
                                text = strings.synonymsPrefix + explanation.synonymsInContext
                                    .joinToString(", ") { it.word },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (response.dictionaryEntries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.keyWords,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        response.dictionaryEntries.take(3).forEach { entry ->
                            val meaning = entry.entries.firstOrNull()
                                ?.definitions?.firstOrNull()?.meaning.orEmpty()
                            Text(
                                text = "• ${entry.word} — $meaning",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    fontSize = 18.sp,
                                    lineHeight = 27.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (uiState.wordLookup == null && uiState.wordLookupLoading) {
                            Spacer(modifier = Modifier.height(12.dp))
                            BlinkingCursor()
                        }
                        // Youdao-style Chinese dictionary block for single words.
                        uiState.wordLookup?.let { info ->
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = info.word,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            WordLookupInfoContent(info = info)
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.emptyResult,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Text(strings.copy, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = strings.shareTranslation,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        enabled = uiState.currentHistoryId != null
                    ) {
                        Icon(
                            imageVector = if (uiState.isCurrentFavorite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = strings.favoriteTranslation,
                            tint = if (uiState.isCurrentFavorite) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
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

/**
 * Renders [text] with individually tappable words for dictionary lookup, as
 * ONE [Text] instead of one composable per word: long translations stay a
 * single layout node (hundreds of word nodes used to slow composition and
 * break text selection), paragraph gaps come from the real newline
 * characters, and TalkBack reads the text naturally.
 */
@Composable
private fun ClickableWords(
    text: String,
    onWordClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Last tapped word stays highlighted (underline + brand color) as feedback.
    var tappedWord by remember(text) { mutableStateOf<String?>(null) }
    val highlightColor = MaterialTheme.colorScheme.primary

    // Word ranges in annotated-string coordinates, resolved at tap time.
    val (annotated, wordRanges) = remember(text, tappedWord, highlightColor) {
        buildAnnotated(text, tappedWord, highlightColor)
    }
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotated,
        onTextLayout = { layoutResult.value = it },
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 18.sp,
            lineHeight = 27.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(text) {
                detectTapGestures { position ->
                    val layout = layoutResult.value ?: return@detectTapGestures
                    val offset = layout.getOffsetForPosition(position)
                    wordRanges.firstOrNull { (range, _) ->
                        offset >= range.first && offset <= range.last + 1
                    }?.let { (_, cleanWord) ->
                        tappedWord = cleanWord
                        onWordClick(cleanWord)
                    }
                }
            }
    )
}

/** The word characters considered part of a lookable-up token. */
private val wordRegex = Regex("[A-Za-z\u00C0-\u024F'-]+")

private data class AnnotatedWords(
    val annotated: AnnotatedString,
    val ranges: List<Pair<IntRange, String>>
)

/**
 * Builds the annotated string, recording every word's character range and
 * cleaned form. The tapped word (if any) gets an underline + brand color
 * span; rebuilding only happens on tap, never per animation frame.
 */
private fun buildAnnotated(
    text: String,
    tappedWord: String?,
    highlightColor: Color
): AnnotatedWords {
    val ranges = mutableListOf<Pair<IntRange, String>>()
    val annotated = buildAnnotatedString {
        var cursor = 0
        for (match in wordRegex.findAll(text)) {
            append(text.substring(cursor, match.range.first))
            val raw = match.value
            val clean = raw.trim { !it.isLetterOrDigit() }.lowercase()
            val spanStart = length
            if (clean.isNotEmpty() && clean == tappedWord) {
                withStyle(
                    SpanStyle(
                        color = highlightColor,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(raw)
                }
            } else {
                append(raw)
            }
            if (clean.isNotEmpty()) {
                ranges += IntRange(spanStart, length - 1) to clean
            }
            cursor = match.range.last + 1
        }
        append(text.substring(cursor))
    }
    return AnnotatedWords(annotated, ranges)
}

/**
 * Streaming translation text with the typewriter cursor rendered inline via
 * an [InlineTextContent]. The annotated string is rebuilt only when [text]
 * changes; the blink animation runs INSIDE the cursor's inline composable,
 * so no per-frame string rebuilds — that used to cost a full
 * buildAnnotatedString of the whole accumulated text on every animation
 * frame, which stalled long streaming translations.
 */
private const val STREAM_CURSOR_ID = "streamCursor"

@Composable
private fun StreamingText(
    text: String,
    modifier: Modifier = Modifier
) {
    val display = remember(text) {
        buildAnnotatedString {
            append(text)
            appendInlineContent(STREAM_CURSOR_ID, " ")
        }
    }
    Text(
        text = display,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 18.sp,
            lineHeight = 27.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        inlineContent = mapOf(
            STREAM_CURSOR_ID to InlineTextContent(
                Placeholder(0.45.em, 1.1.em, PlaceholderVerticalAlign.TextCenter)
            ) {
                BlinkingCursorBlock()
            }
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** The pulsing block used as the streaming cursor's inline content. */
@Composable
private fun BlinkingCursorBlock() {
    val transition = rememberInfiniteTransition(label = "streamCursor")
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "streamCursorAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(cursorAlpha)
            .background(MaterialTheme.colorScheme.primary)
    )
}

/**
 * Lightweight dictionary lookup entry: a field-styled row that opens the full
 * dictionary sheet on tap. The full search input lives in Learning → Words.
 */
@Composable
private fun LookupEntryBar(onGoToSettings: () -> Unit) {
    val strings = LocalStrings.current
    var showSheet by remember { mutableStateOf(false) }

    Surface(
        onClick = { showSheet = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = strings.lookUpWord,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = strings.englishWordHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showSheet) {
        DictionaryBottomSheet(
            initialWord = "",
            onDismiss = { showSheet = false },
            onGoToSettings = {
                showSheet = false
                onGoToSettings()
            }
        )
    }
}

@Composable
private fun BrandingFooter(modifier: Modifier = Modifier) {    val strings = LocalStrings.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = strings.copyright,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.viewOnGitHub,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
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
            onSpeakClick = {},
            onToggleFavoriteTranslation = {},
            onCancelStreaming = {},
            onCancelTranslation = {},
            onReuseHistoryItem = {},
            onSettingsClick = {}
        )
    }
}

/**
 * The data layer emits stable notice keys; only the UI knows the active
 * language. Unknown values (raw messages) pass through unchanged.
 */
private fun localizeNotice(message: String, strings: AppStrings): String =
    when (message) {
        TranslationNotices.LLM_KEY_MISSING -> strings.noticeLlmKeyMissing
        TranslationNotices.LLM_FAILED -> strings.noticeLlmFailed
        TranslationNotices.STREAM_INTERRUPTED -> strings.noticeStreamInterrupted
        else -> message
    }
