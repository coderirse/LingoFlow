package com.lingoflow.app.domain.model

import com.lingoflow.app.domain.model.translation.TranslationErrors

/**
 * Translation failure carrying a stable [code] from [TranslationErrors].
 * Only the UI layer turns codes into localized text; engine-specific
 * exception details never leak past the data layer.
 */
class TranslationException(
    val code: String = TranslationErrors.GENERIC,
    cause: Throwable? = null
) : Exception(code, cause)
