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

/** User-facing label for mode pickers and history chips. */
val TranslationMode.displayName: String
    get() = when (this) {
        TranslationMode.STANDARD -> "Standard"
        TranslationMode.NATURAL -> "Natural"
        TranslationMode.CONCISE -> "Concise"
        TranslationMode.FORMAL -> "Formal"
        TranslationMode.LEARNING -> "Learning"
    }
