package com.lingoflow.app.domain.model.translation

/** How a translation should be produced. */
enum class TranslationMode {
    /** Plain ML Kit translation (current behavior). */
    STANDARD,

    /** Idiomatic, natural-sounding rendering (LLM). */
    NATURAL,

    /** Shorter, tighter rendering (LLM). */
    CONCISE,

    /** Formal register (LLM). */
    FORMAL,

    /** Translation plus dictionary entries and in-context explanation. */
    LEARNING
}
