package com.lingoflow.app.ui.dictionary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.lingoflow.app.ui.i18n.LocalStrings

/**
 * Full dictionary lookup presented as a bottom sheet. Owns its own
 * [DictionaryViewModel]; the host screen only supplies the initial word and
 * dismiss/navigation callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryBottomSheet(
    initialWord: String,
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit,
    viewModel: DictionaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    var query by remember { mutableStateOf(initialWord) }

    LaunchedEffect(initialWord) {
        if (initialWord.isNotBlank()) {
            viewModel.lookUp(initialWord)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(strings.englishWordHint) },
                    singleLine = true,
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = strings.clear
                                )
                            }
                        }
                    }
                )
                Button(
                    onClick = { viewModel.lookUp(query) },
                    enabled = query.isNotBlank() && uiState !is DictionaryUiState.Loading
                ) {
                    Text(strings.lookUp)
                }
            }

            when (val state = uiState) {
                DictionaryUiState.Idle -> {
                    Text(
                        text = strings.enterWordToLookup,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                DictionaryUiState.Loading -> {
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
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        items(state.entries) { entry ->
                            DictionaryEntryContent(
                                entry = entry,
                                isFavorite = entry.word.trim().lowercase() in favorites,
                                ttsReady = viewModel.ttsReady,
                                onToggleFavorite = { viewModel.toggleFavorite(entry.word) },
                                onSpeak = { viewModel.speak(entry.word) }
                            )
                        }
                    }
                }

                is DictionaryUiState.Error -> {
                    DictionaryErrorContent(
                        error = state.error,
                        onGoToSettings = onGoToSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun DictionaryEntryContent(
    entry: DictionaryEntry,
    isFavorite: Boolean,
    ttsReady: Boolean,
    onToggleFavorite: () -> Unit,
    onSpeak: () -> Unit
) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.word,
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

        entry.entries.forEach { posEntry ->
            if (posEntry.partOfSpeech.isNotBlank()) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = posEntry.partOfSpeech,
                            fontStyle = FontStyle.Italic
                        )
                    }
                )
            }
            posEntry.definitions.forEach { definition ->
                Column(
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        definition.senseNumber?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = definition.meaning,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (definition.labels.isNotEmpty()) {
                        Text(
                            text = definition.labels.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    definition.examples.forEach { example ->
                        Text(
                            text = example.sentence,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }

        if (entry.phrases.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = strings.phrasalVerbs,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            entry.phrases.forEach { phrase ->
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = phrase.phrase,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (phrase.meaning.isNotBlank()) {
                        Text(
                            text = phrase.meaning,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        entry.etymology?.let { etymology ->
            if (etymology.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = strings.etymology,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = etymology,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DictionaryErrorContent(
    error: DictionaryException,
    onGoToSettings: () -> Unit
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (error) {
            is DictionaryException.NoApiKey -> {
                Text(
                    text = strings.noApiKey,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = onGoToSettings) {
                    Text(strings.goToSettings)
                }
            }

            is DictionaryException.NotFound -> {
                val message = if (error.suggestions.isEmpty()) {
                    strings.noResults
                } else {
                    strings.wordNotFoundWithSuggestions(
                        error.suggestions.joinToString(", ")
                    )
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is DictionaryException.InvalidApiKey -> {
                Text(
                    text = strings.invalidApiKey,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is DictionaryException.Network -> {
                Text(
                    text = strings.networkError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is DictionaryException.ParseError -> {
                Text(
                    text = strings.parseError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
