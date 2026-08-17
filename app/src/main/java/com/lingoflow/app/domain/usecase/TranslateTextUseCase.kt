package com.lingoflow.app.domain.usecase

import com.lingoflow.app.domain.engine.TranslationEngine
import com.lingoflow.app.domain.model.TranslationException
import com.lingoflow.app.domain.model.TranslationStatus
import com.lingoflow.app.domain.model.translation.TranslationRequest
import com.lingoflow.app.domain.model.translation.TranslationResponse
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Translates a piece of text according to the requested TranslationMode. */
class TranslateTextUseCase @Inject constructor(
    private val engine: TranslationEngine
) {
    /** Current engine status (e.g. downloading a model). */
    val status: StateFlow<TranslationStatus> = engine.status

    /** One-shot notices (e.g. LLM key missing → on-device fallback). */
    val fallbackMessages: Flow<String> = engine.fallbackMessages

    suspend operator fun invoke(
        request: TranslationRequest
    ): Result<TranslationResponse> {
        if (request.text.isBlank()) {
            return Result.failure(TranslationException("Nothing to translate."))
        }
        return engine.translate(request)
    }
}
