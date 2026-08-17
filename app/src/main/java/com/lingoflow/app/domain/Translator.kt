package com.lingoflow.app.domain

import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.TranslationResult
import com.lingoflow.app.domain.model.TranslationStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Translation engine abstraction. Implementations live in the data layer
 * (MlKitTranslator in production, FakeTranslator for tests).
 */
interface Translator {

    /** Current engine status (e.g. downloading a model). */
    val status: StateFlow<TranslationStatus>

    suspend fun translate(
        text: String,
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<TranslationResult>
}
