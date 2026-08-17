package com.lingoflow.app.domain.engine

import com.lingoflow.app.domain.model.translation.TranslationRequest
import kotlinx.coroutines.flow.Flow

/**
 * Capability of engines that can produce a translation incrementally.
 * Emits text deltas in order; the concatenation of all deltas is the
 * complete translation. Completion of the flow means the translation is
 * finished; errors surface as flow exceptions.
 */
interface StreamingTranslationEngine {
    fun translateStream(request: TranslationRequest): Flow<String>
}
