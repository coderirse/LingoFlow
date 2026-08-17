package com.lingoflow.app.ui.dictionary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lingoflow.app.domain.exception.DictionaryException
import com.lingoflow.app.domain.model.dictionary.DictionaryEntry
import com.lingoflow.app.domain.model.dictionary.WordLookupInfo
import com.lingoflow.app.ui.components.BlinkingCursor
import com.lingoflow.app.ui.i18n.LocalStrings

/**
 * Stage one of tap-to-lookup: a compact preview with the headword, phonetic,
 * first definition and favorite toggle. "View Full Definition" escalates to
 * the full [DictionaryBottomSheet]. Data comes from the same
 * [DictionaryViewModel] pipeline (cached), so repeat lookups are instant.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPreviewSheet(
    word: String,
    onDismiss: () -> Unit,
    onViewFullDefinition: () -> Unit,
    onGoToSettings: () -> Unit,
    viewModel: DictionaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val lookupInfo by viewModel.lookupInfo.collectAsStateWithLifecycle()
    val lookupInfoUnavailable by viewModel.lookupInfoUnavailable.collectAsStateWithLifecycle()
    val lookupInfoLoading by viewModel.lookupInfoLoading.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    LaunchedEffect(word) {
        viewModel.lookUp(word)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val state = uiState) {
                DictionaryUiState.Idle, DictionaryUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is DictionaryUiState.Success -> {
                    val entry = state.entries.first()
                    WordPreviewContent(
                        word = entry.word,
                        entry = entry,
                        lookupInfo = lookupInfo,
                        lookupInfoUnavailable = lookupInfoUnavailable,
                        lookupInfoLoading = lookupInfoLoading,
                        isFavorite = entry.word.trim().lowercase() in favorites,
                        ttsReady = viewModel.ttsReady,
                        onToggleFavorite = { viewModel.toggleFavorite(entry.word) },
                        onSpeak = { viewModel.speak(entry.word) },
                        onViewFullDefinition = onViewFullDefinition
                    )
                }

                is DictionaryUiState.Error -> {
                    WordPreviewError(
                        error = state.error,
                        onGoToSettings = {
                            onDismiss()
                            onGoToSettings()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WordPreviewContent(
    word: String,
    entry: DictionaryEntry,
    lookupInfo: WordLookupInfo?,
    lookupInfoUnavailable: Boolean,
    lookupInfoLoading: Boolean,
    isFavorite: Boolean,
    ttsReady: Boolean,
    onToggleFavorite: () -> Unit,
    onSpeak: () -> Unit,
    onViewFullDefinition: () -> Unit
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) {
                    Icons.Default.Favorite
                } else {
                    Icons.Default.FavoriteBorder
                },
                contentDescription = if (isFavorite) {
                    strings.removeFromFavorites
                } else {
                    strings.addToFavorites
                },
                tint = if (isFavorite) {
                    Color(0xFF00BCD4)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }

    entry.phonetics.firstOrNull()?.let { phonetic ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = phonetic.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onSpeak, enabled = ttsReady) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = strings.playPronunciation
                )
            }
        }
    }

    if (lookupInfo != null) {
        // LLM-produced Chinese glosses, Youdao-style.
        WordLookupInfoContent(info = lookupInfo)
    } else if (lookupInfoLoading) {
        // MW result is already on screen; the Chinese glosses are on the way.
        BlinkingCursor()
    } else {
        // Fallback: Merriam-Webster English entry.
        val firstPos = entry.entries.firstOrNull()
        firstPos?.let { posEntry ->
            if (posEntry.partOfSpeech.isNotBlank()) {
                Text(
                    text = posEntry.partOfSpeech,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            posEntry.definitions.firstOrNull()?.let { definition ->
                Text(
                    text = definition.meaning,
                    style = MaterialTheme.typography.bodyMedium
                )
                definition.examples.firstOrNull()?.let { example ->
                    Text(
                        text = example.sentence,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (lookupInfoUnavailable) {
            Text(
                text = strings.chineseMeaningUnavailable,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Button(
        onClick = onViewFullDefinition,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(strings.viewFullDefinition)
    }
}

@Composable
private fun WordPreviewError(
    error: DictionaryException,
    onGoToSettings: () -> Unit
) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val message = when (error) {
            is DictionaryException.NoApiKey -> strings.noApiKey
            is DictionaryException.NotFound ->
                if (error.suggestions.isEmpty()) {
                    strings.noResults
                } else {
                    strings.wordNotFoundWithSuggestions(
                        error.suggestions.joinToString(", ")
                    )
                }
            is DictionaryException.InvalidApiKey -> strings.invalidApiKey
            else -> strings.networkError
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        if (error is DictionaryException.NoApiKey) {
            TextButton(onClick = onGoToSettings) {
                Text(strings.goToSettings)
            }
        }
    }
}
