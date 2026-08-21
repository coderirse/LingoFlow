package com.lingoflow.app.ui.learning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lingoflow.app.domain.model.history.TranslationHistoryItem
import com.lingoflow.app.ui.dictionary.DictionaryBottomSheet
import com.lingoflow.app.ui.i18n.LocalStrings

/**
 * Learning tab: favorite translations and favorite words live in separate
 * sub-tabs instead of one mixed column. The word tab also hosts the manual
 * dictionary lookup entry (moved from the home screen).
 */
@Composable
fun LearningRoute(
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearningViewModel = hiltViewModel()
) {
    val words by viewModel.favoriteWords.collectAsStateWithLifecycle()
    val translations by viewModel.favoriteTranslations.collectAsStateWithLifecycle()
    LearningScreen(
        words = words,
        translations = translations,
        onRemove = viewModel::remove,
        onRemoveTranslation = viewModel::removeTranslation,
        onGoToSettings = onGoToSettings,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(
    words: List<String>,
    translations: List<TranslationHistoryItem>,
    onRemove: (String) -> Unit,
    onRemoveTranslation: (String) -> Unit,
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }
    var lookupWord by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SegmentedButton(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(strings.favoriteTranslationsSection, maxLines = 1)
            }
            SegmentedButton(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text(strings.favoriteWordsSection, maxLines = 1)
            }
        }

        when (selectedSection) {
            0 -> TranslationsTab(
                translations = translations,
                onRemoveTranslation = onRemoveTranslation,
                modifier = Modifier.fillMaxSize()
            )

            else -> WordsTab(
                words = words,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSearch = {
                    if (searchQuery.isNotBlank()) {
                        lookupWord = searchQuery.trim().lowercase()
                        searchQuery = ""
                    }
                },
                onWordClick = { lookupWord = it },
                onRemove = onRemove,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    lookupWord?.let { word ->
        DictionaryBottomSheet(
            initialWord = word,
            onDismiss = { lookupWord = null },
            onGoToSettings = {
                lookupWord = null
                onGoToSettings()
            }
        )
    }
}

@Composable
private fun TranslationsTab(
    translations: List<TranslationHistoryItem>,
    onRemoveTranslation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    if (translations.isEmpty()) {
        EmptyHint(text = strings.noFavoriteTranslations, modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(translations, key = { it.id }) { item ->
                FavoriteTranslationCard(
                    item = item,
                    onRemove = { onRemoveTranslation(item.id) }
                )
            }
        }
    }
}

@Composable
private fun WordsTab(
    words: List<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onWordClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Column(modifier = modifier) {
        // Manual dictionary lookup entry (moved from the home screen).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                placeholder = { Text(strings.englishWordHint) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            IconButton(onClick = onSearch, enabled = searchQuery.isNotBlank()) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = strings.lookUpWord,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (words.isEmpty()) {
            EmptyHint(
                text = strings.noFavoriteWords,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(words, key = { it }) { word ->
                    FavoriteWordCard(
                        word = word,
                        onClick = { onWordClick(word) },
                        onRemove = { onRemove(word) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FavoriteBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FavoriteTranslationCard(
    item: TranslationHistoryItem,
    onRemove: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.sourceText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.translatedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = strings.removeFromFavorites,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun FavoriteWordCard(
    word: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = strings.removeFromFavorites,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
