package com.lingoflow.app.ui.dictionary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.lingoflow.app.ui.i18n.LocalStrings

/**
 * Headword with the TTS play button, shown whenever a word is on screen —
 * including error states: pronunciation never depends on lookup success
 * or on phonetic data being available.
 */
@Composable
internal fun WordPronunciationHeader(
    word: String,
    ttsReady: Boolean,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onSpeak, enabled = ttsReady) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = strings.playPronunciation
            )
        }
    }
}
