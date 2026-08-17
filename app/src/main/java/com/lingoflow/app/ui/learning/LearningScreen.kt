package com.lingoflow.app.ui.learning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lingoflow.app.ui.dictionary.DictionaryBottomSheet
import com.lingoflow.app.ui.i18n.LocalStrings
import com.lingoflow.app.ui.theme.LingoFlowSecondary

/** Real Learning tab: the user's favorite dictionary words. */
@Composable
fun LearningRoute(
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearningViewModel = hiltViewModel()
) {
    val words by viewModel.favoriteWords.collectAsStateWithLifecycle()
    LearningScreen(
        words = words,
        onRemove = viewModel::remove,
        onGoToSettings = onGoToSettings,
        modifier = modifier
    )
}

@Composable
fun LearningScreen(
    words: List<String>,
    onRemove: (String) -> Unit,
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var lookupWord by remember { mutableStateOf<String?>(null) }

    if (words.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
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
                text = strings.noFavoriteWords,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(words, key = { it }) { word ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { lookupWord = word },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        IconButton(onClick = { onRemove(word) }) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = strings.removeFromFavorites,
                                tint = LingoFlowSecondary
                            )
                        }
                    }
                }
            }
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
