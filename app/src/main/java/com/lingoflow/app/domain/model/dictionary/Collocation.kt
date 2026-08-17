package com.lingoflow.app.domain.model.dictionary

/** A common word combination, e.g. "make a decision". */
data class Collocation(
    val phrase: String,
    val meaning: String?,
    val example: String?
)
