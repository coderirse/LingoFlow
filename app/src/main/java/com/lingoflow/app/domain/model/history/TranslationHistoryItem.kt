package com.lingoflow.app.domain.model.history

import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.translation.TranslationMode
import java.util.UUID
import kotlinx.serialization.Serializable

/** One saved translation record, shown in the History tab. */
@Serializable
data class TranslationHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: Language,
    val targetLanguage: Language,
    val mode: TranslationMode,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
