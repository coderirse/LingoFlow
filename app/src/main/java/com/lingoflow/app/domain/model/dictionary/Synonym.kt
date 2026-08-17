package com.lingoflow.app.domain.model.dictionary

/** A synonym, optionally scoped to a context where it fits. */
data class Synonym(
    val word: String,
    val context: String?
)
