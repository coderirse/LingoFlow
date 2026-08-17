package com.lingoflow.app.domain.model

/** High-level status of the translation engine, surfaced to the UI. */
enum class TranslationStatus {
    IDLE,
    PREPARING_MODEL,
    TRANSLATING
}
