package com.lingoflow.app.domain.engine

import com.lingoflow.app.domain.model.TranslationStatus
import com.lingoflow.app.domain.model.translation.TranslationRequest
import com.lingoflow.app.domain.model.translation.TranslationResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Mode-aware translation engine. Unlike the plain [com.lingoflow.app.domain.Translator]
 * used by the current ML Kit pipeline, engines understand TranslationMode and
 * return the richer TranslationResponse hierarchy.
 */
interface TranslationEngine {

    /** Current engine status (e.g. downloading a model). */
    val status: StateFlow<TranslationStatus>

    /**
     * One-shot notices for the UI (e.g. "fell back to on-device translation
     * because no LLM API key is set"). Engines without notices emit nothing.
     */
    val fallbackMessages: Flow<String> get() = emptyFlow()

    suspend fun translate(request: TranslationRequest): Result<TranslationResponse>
}
