package com.lingoflow.app.data.engine

import com.lingoflow.app.domain.engine.MlKitTranslationEngine
import com.lingoflow.app.domain.engine.StreamingTranslationEngine
import com.lingoflow.app.domain.engine.TranslationEngine
import com.lingoflow.app.domain.model.TranslationStatus
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.model.translation.TranslationRequest
import com.lingoflow.app.domain.model.translation.TranslationResponse
import com.lingoflow.app.domain.repository.SettingsRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Routes translation requests to the right engine: STANDARD always runs
 * on-device (ML Kit); LLM modes run through [LlmTranslationEngine] when the
 * active provider has an API key, otherwise they fall back to ML Kit and the
 * UI is notified via [fallbackMessages].
 */
class TranslationRouter(
    private val mlKitEngine: MlKitTranslationEngine,
    private val llmEngine: LlmTranslationEngine,
    private val settingsRepository: SettingsRepository
) : TranslationEngine, StreamingTranslationEngine {

    private val _status = MutableStateFlow(TranslationStatus.IDLE)
    override val status: StateFlow<TranslationStatus> = _status.asStateFlow()

    private val _fallbackMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val fallbackMessages: Flow<String> = _fallbackMessages

    override suspend fun translate(
        request: TranslationRequest
    ): Result<TranslationResponse> {
        val engine = selectEngine(request.mode)
        // ML Kit only understands STANDARD; rewrite the mode when an LLM
        // request falls back to the on-device engine.
        val effectiveRequest = if (engine === mlKitEngine) {
            request.copy(mode = TranslationMode.STANDARD)
        } else {
            request
        }
        return coroutineScope {
            val mirror = launch {
                engine.status.collect { _status.value = it }
            }
            try {
                engine.translate(effectiveRequest)
            } finally {
                mirror.cancel()
                _status.value = TranslationStatus.IDLE
            }
        }
    }

    private suspend fun selectEngine(mode: TranslationMode): TranslationEngine {
        if (mode == TranslationMode.STANDARD) return mlKitEngine

        val settings = settingsRepository.getSettings()
        val hasKey = settings.llmProviders[settings.activeLlmProviderId]
            ?.apiKey
            ?.isNotBlank() == true
        return if (hasKey) {
            llmEngine
        } else {
            _fallbackMessages.tryEmit(
                "LLM API key not set. Using on-device translation."
            )
            mlKitEngine
        }
    }

    /**
     * Streaming path for NATURAL/CONCISE/FORMAL. Without an API key it
     * degrades to a single-shot ML Kit emission plus a fallback notice.
     */
    override fun translateStream(request: TranslationRequest): Flow<String> = flow {
        val settings = settingsRepository.getSettings()
        val hasKey = settings.llmProviders[settings.activeLlmProviderId]
            ?.apiKey
            ?.isNotBlank() == true

        if (hasKey) {
            llmEngine.translateStream(request).collect { emit(it) }
        } else {
            _fallbackMessages.tryEmit(
                "LLM API key not set. Using on-device translation."
            )
            val result = mlKitEngine.translate(request.copy(mode = TranslationMode.STANDARD))
            result
                .onSuccess { response ->
                    emit(
                        when (response) {
                            is TranslationResponse.Standard -> response.translatedText
                            is TranslationResponse.Learning -> response.translatedText
                        }
                    )
                }
                .onFailure { throw it }
        }
    }
}
