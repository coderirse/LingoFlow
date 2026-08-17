package com.lingoflow.app.domain.model

/**
 * Translation failure carrying a user-facing [userMessage].
 * Engine-specific exception details never leak past the data layer.
 */
class TranslationException(
    val userMessage: String,
    cause: Throwable? = null
) : Exception(userMessage, cause)
