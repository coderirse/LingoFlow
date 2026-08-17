package com.lingoflow.app.domain.model.dictionary

/** A single sense of a word within one part of speech. */
data class Definition(
    val meaning: String,
    /** Sense identifier as printed by the dictionary, e.g. "1 a". */
    val senseNumber: String?,
    /** Usage labels, e.g. "formal", "chiefly US". */
    val labels: List<String>,
    val examples: List<Example>
)
