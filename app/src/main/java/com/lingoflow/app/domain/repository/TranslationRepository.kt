package com.lingoflow.app.domain.repository

import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.TranslationResult
import com.lingoflow.app.domain.model.TranslationStatus
import kotlinx.coroutines.flow.StateFlow

/** Gateway the domain layer uses to perform translations. */
interface TranslationRepository {

    /** Current engine status (e.g. downloading a model). */
    val status: StateFlow<TranslationStatus>

    suspend fun translate(
        text: String,
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<TranslationResult>
}
