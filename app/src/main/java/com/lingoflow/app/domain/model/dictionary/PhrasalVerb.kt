package com.lingoflow.app.domain.model.dictionary

/** A phrasal verb derived from a headword, e.g. "abide by". */
data class PhrasalVerb(
    val phrase: String,
    val meaning: String,
    val examples: List<Example>
)
