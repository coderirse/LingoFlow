package com.lingoflow.app.domain.usecase

import com.lingoflow.app.domain.engine.StreamingTranslationEngine
import com.lingoflow.app.domain.engine.TranslationEngine
import com.lingoflow.app.domain.model.TranslationException
import com.lingoflow.app.domain.model.TranslationStatus
import com.lingoflow.app.domain.model.translation.TranslationErrors
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
            return Result.failure(TranslationException(TranslationErrors.NOTHING_TO_TRANSLATE))
        }
        return engine.translate(request)
    }

    /** Streaming translation, when the bound engine supports it; null otherwise. */
    fun translateStream(request: TranslationRequest): Flow<String>? {
        if (request.text.isBlank()) return null
        return (engine as? StreamingTranslationEngine)?.translateStream(request)
    }
}
