package com.lingoflow.app.domain.engine

import com.lingoflow.app.domain.Translator
import com.lingoflow.app.domain.model.TranslationStatus
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.model.translation.TranslationRequest
import com.lingoflow.app.domain.model.translation.TranslationResponse
import kotlinx.coroutines.flow.StateFlow

/**
 * TranslationEngine adapter over the existing ML Kit [Translator] pipeline.
 * Only STANDARD mode is meaningful here; richer modes are other engines' job.
 */
class MlKitTranslationEngine(
    private val translator: Translator
) : TranslationEngine {

    override val status: StateFlow<TranslationStatus> = translator.status

    override suspend fun translate(
        request: TranslationRequest
    ): Result<TranslationResponse> {
        require(request.mode == TranslationMode.STANDARD) {
            "MlKitTranslationEngine only supports STANDARD mode"
        }
        return translator.translate(
            text = request.text,
            sourceLanguage = request.sourceLanguage,
            targetLanguage = request.targetLanguage
        ).map { result ->
            TranslationResponse.Standard(
                translatedText = result.translatedText,
                detectedLanguage = result.sourceLanguage
            )
        }
    }
}
