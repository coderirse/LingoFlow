package com.lingoflow.app.domain.model.learning

import com.lingoflow.app.domain.model.dictionary.Synonym

/** AI explanation of a word as used in a specific sentence. */
data class ContextExplanation(
    val originalSentence: String,
    val selectedWord: String,
    val meaningInContext: String,
    val grammarNote: String?,
    val usageNote: String?,
    val synonymsInContext: List<Synonym>
)
