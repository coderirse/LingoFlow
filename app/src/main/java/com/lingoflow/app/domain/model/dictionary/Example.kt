package com.lingoflow.app.domain.model.dictionary

/** An example sentence illustrating usage. */
data class Example(
    val sentence: String,
    /** Attribution, e.g. the author or corpus the sentence comes from. */
    val source: String?
)
