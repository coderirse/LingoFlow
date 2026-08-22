package com.lingoflow.app.data.engine

import com.lingoflow.app.domain.engine.MlKitTranslationEngine
import com.lingoflow.app.domain.engine.StreamingTranslationEngine
import com.lingoflow.app.domain.engine.TranslationEngine
import com.lingoflow.app.domain.model.TranslationStatus
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.model.translation.TranslationNotices
import com.lingoflow.app.domain.model.translation.TranslationRequest
import com.lingoflow.app.domain.model.translation.TranslationResponse
import com.lingoflow.app.domain.repository.SettingsRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
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
        val engine = selectEngine(request)
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
                val result = engine.translate(effectiveRequest)
                if (result.isFailure && engine === llmEngine &&
                    request.mode == TranslationMode.STANDARD
                ) {
                    // Long STANDARD text is an LLM enhancement, not a hard
                    // dependency: keep the old behavior when the model call
                    // fails (network, rate limit, invalid key, truncation…).
                    _fallbackMessages.tryEmit(TranslationNotices.LLM_FAILED)
                    mlKitEngine.translate(
                        request.copy(mode = TranslationMode.STANDARD)
                    )
                } else {
                    result
                }
            } finally {
                mirror.cancel()
                _status.value = TranslationStatus.IDLE
            }
        }
    }

    private suspend fun selectEngine(request: TranslationRequest): TranslationEngine {
        if (
            request.mode == TranslationMode.STANDARD &&
            !isLongText(request.text)
        ) {
            return mlKitEngine
        }

        // Long STANDARD requests still need an LLM key; without one keep the
        // existing on-device behavior instead of failing the translation.
        val settings = settingsRepository.getSettings()
        val hasKey = settings.llmProviders[settings.activeLlmProviderId]
            ?.apiKey
            ?.isNotBlank() == true
        return if (hasKey) {
            llmEngine
        } else {
            _fallbackMessages.tryEmit(TranslationNotices.LLM_KEY_MISSING)
            mlKitEngine
        }
    }

    private fun isLongText(text: String): Boolean =
        text.trim().length >= LONG_TEXT_MIN_LENGTH

    /**
     * Streaming path for NATURAL/CONCISE/FORMAL and long STANDARD text.
     * Long STANDARD is an LLM formatting enhancement, so without an API key
     * (or when the model call fails before any output) it degrades to a
     * single-shot ML Kit emission plus a fallback notice; short STANDARD
     * never reaches the LLM here either.
     */
    override fun translateStream(request: TranslationRequest): Flow<String> = flow {
        val wantsLlm = request.mode != TranslationMode.STANDARD ||
            isLongText(request.text)
        val settings = settingsRepository.getSettings()
        val hasKey = settings.llmProviders[settings.activeLlmProviderId]
            ?.apiKey
            ?.isNotBlank() == true

        // Mirror the chosen engine's status (e.g. PREPARING_MODEL while
        // ML Kit downloads a model in the fallback path) for the whole
        // collection, including cancellation.
        val engine: TranslationEngine = if (wantsLlm && hasKey) llmEngine else mlKitEngine
        coroutineScope {
            val mirror = launch { engine.status.collect { _status.value = it } }
            try {
                if (wantsLlm && hasKey) {
                    var emitted = false
                    try {
                        llmEngine.translateStream(request).collect {
                            emitted = true
                            emit(it)
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (!emitted && request.mode == TranslationMode.STANDARD) {
                            // The LLM is an enhancement for STANDARD, never
                            // a hard dependency: fall back on-device when
                            // the model call failed before any output.
                            _fallbackMessages.tryEmit(TranslationNotices.LLM_FAILED)
                            emitOnDevice(request)
                        } else {
                            throw e
                        }
                    }
                } else {
                    if (wantsLlm) {
                        _fallbackMessages.tryEmit(TranslationNotices.LLM_KEY_MISSING)
                    }
                    emitOnDevice(request)
                }
            } finally {
                mirror.cancel()
                _status.value = TranslationStatus.IDLE
            }
        }
    }

    private suspend fun FlowCollector<String>.emitOnDevice(
        request: TranslationRequest
    ) {
        mlKitEngine.translate(request.copy(mode = TranslationMode.STANDARD))
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

    companion object {
        /** Texts at least this long (trimmed) are treated as "long text". */
        const val LONG_TEXT_MIN_LENGTH = 500
    }
}
