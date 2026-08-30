package com.lingoflow.app.data.engine

import com.lingoflow.app.domain.engine.StreamingTranslationEngine
import com.lingoflow.app.domain.engine.TranslationEngine
import com.lingoflow.app.domain.exception.LlmException
import com.lingoflow.app.domain.model.TranslationException
import com.lingoflow.app.domain.model.TranslationStatus
import com.lingoflow.app.domain.model.dictionary.Synonym
import com.lingoflow.app.domain.model.learning.ContextExplanation
import com.lingoflow.app.domain.model.llm.ChatRequest
import com.lingoflow.app.domain.model.llm.LlmProvider
import com.lingoflow.app.domain.model.llm.Message
import com.lingoflow.app.domain.model.settings.ProviderConfig
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.model.translation.TranslationErrors
import com.lingoflow.app.domain.model.translation.TranslationRequest
import com.lingoflow.app.domain.model.translation.TranslationResponse
import com.lingoflow.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * LLM-backed [TranslationEngine] for the non-STANDARD modes. Builds the
 * provider from the active Settings configuration on every call, so key or
 * provider changes apply immediately.
 */
class LlmTranslationEngine(
    private val settingsRepository: SettingsRepository,
    private val providerFactory: (ProviderConfig) -> LlmProvider
) : TranslationEngine, StreamingTranslationEngine {

    private val _status = MutableStateFlow(TranslationStatus.IDLE)
    override val status: StateFlow<TranslationStatus> = _status.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun translate(
        request: TranslationRequest
    ): Result<TranslationResponse> {
        val config = activeProviderConfig()
            ?: return Result.failure(TranslationException(TranslationErrors.LLM_KEY_MISSING))

        val provider = providerFactory(config)
        val chatRequest = buildChatRequest(request, config)

        return try {
            _status.value = TranslationStatus.TRANSLATING
            val response = provider.chat(chatRequest)
            if (response.finishReason == "length") {
                // A truncated "success" must never be presented as a
                // complete translation (it would also land in history).
                return Result.failure(TranslationException(TranslationErrors.TRUNCATED))
            }
            Result.success(buildResponse(request.mode, response.content))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toUserFriendlyError())
        } finally {
            _status.value = TranslationStatus.IDLE
        }
    }

    override fun translateStream(request: TranslationRequest): Flow<String> = flow {
        require(request.mode in STREAMABLE_MODES) {
            "Only NATURAL/CONCISE/FORMAL and long STANDARD modes support streaming"
        }
        val config = activeProviderConfig()
            ?: throw TranslationException(TranslationErrors.LLM_KEY_MISSING)

        val provider = providerFactory(config)
        try {
            _status.value = TranslationStatus.TRANSLATING
            provider.chatStream(buildChatRequest(request, config)).collect { emit(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw e.toUserFriendlyError()
        } finally {
            _status.value = TranslationStatus.IDLE
        }
    }

    private suspend fun activeProviderConfig(): ProviderConfig? {
        val settings = settingsRepository.getSettings()
        val config = settings.llmProviders[settings.activeLlmProviderId]
            ?: ProviderConfig(
                providerId = settings.activeLlmProviderId,
                apiKey = "",
                baseUrl = null,
                model = settings.activeLlmProviderId.defaultModel
            )
        return config.takeIf { it.apiKey.isNotBlank() }
    }

    private fun buildChatRequest(
        request: TranslationRequest,
        config: ProviderConfig
    ) = ChatRequest(
        model = config.model.ifBlank { config.providerId.defaultModel },
        messages = listOf(
            Message(role = "system", content = systemPrompt(request)),
            Message(role = "user", content = request.text)
        ),
        temperature = config.temperature,
        // Long STANDARD translations are the only outputs big enough to
        // hit a provider's (often low) default output cap; raise it there
        // and leave the other modes on the provider default.
        maxTokens = if (request.mode == TranslationMode.STANDARD) {
            LONG_STANDARD_MAX_TOKENS
        } else {
            null
        }
    )

    private fun systemPrompt(request: TranslationRequest): String {
        val source = request.sourceLanguage.displayName
        val target = request.targetLanguage.displayName
        return when (request.mode) {
            TranslationMode.STANDARD ->
                "You are a professional translator. Translate the user's text from $source " +
                    "to $target faithfully, and organize the layout of the translation " +
                    "for readability: split it into paragraphs at natural boundaries " +
                    "instead of one solid block; render enumerations, steps and lists " +
                    "(even ones written inline in the source) as numbered or bulleted " +
                    "lists with exactly one item per line; keep short headings or " +
                    "labels on their own line. Use real newline characters (one blank " +
                    "line between paragraphs), never literal \"\\n\". Do not add, omit " +
                    "or summarize content. Output ONLY the translation, no explanations."
            TranslationMode.NATURAL ->
                "You are a professional translator. Translate the user's text from $source " +
                    "to $target naturally and idiomatically, as a native speaker would say it. " +
                    "Output ONLY the translation, no explanations."
            TranslationMode.CONCISE ->
                "You are a professional translator. Translate the user's text from $source " +
                    "to $target as concisely as possible while keeping the meaning. " +
                    "Output ONLY the translation, no explanations."
            TranslationMode.FORMAL ->
                "You are a professional translator. Translate the user's text from $source " +
                    "to $target in a formal register suitable for business or academic contexts. " +
                    "Output ONLY the translation, no explanations."
            TranslationMode.LEARNING ->
                "You are an English learning assistant for Chinese learners. " +
                    "Translate the user's text from $source to $target, then analyze it. " +
                    "Respond with ONLY a JSON object (no markdown fences) with these keys: " +
                    "\"translation\" (string), \"meaning_in_context\" (string, explain the key " +
                    "word or phrase usage in Chinese), \"grammar_note\" (string or null, in " +
                    "Chinese), \"usage_note\" (string or null, in Chinese), \"synonyms\" " +
                    "(array of English strings)."
        }
    }

    private fun buildResponse(
        mode: TranslationMode,
        content: String
    ): TranslationResponse {
        if (mode == TranslationMode.STANDARD) {
            return TranslationResponse.Standard(
                translatedText = LongTextFormatter.format(content)
            )
        }
        if (mode != TranslationMode.LEARNING) {
            return TranslationResponse.Standard(translatedText = content.trim())
        }

        val parsed = runCatching {
            val cleaned = content.trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            json.parseToJsonElement(cleaned) as? JsonObject
        }.getOrNull()

        val translation = parsed?.get("translation")?.jsonPrimitive?.contentOrNull
            ?: content.trim()
        if (parsed == null) {
            // Model did not follow the JSON contract; degrade gracefully.
            return TranslationResponse.Standard(translatedText = translation)
        }

        val explanation = ContextExplanation(
            originalSentence = "",
            selectedWord = "",
            meaningInContext = parsed.stringOrNull("meaning_in_context") ?: "",
            grammarNote = parsed.stringOrNull("grammar_note"),
            usageNote = parsed.stringOrNull("usage_note"),
            synonymsInContext = parsed.synonyms()
        )
        return TranslationResponse.Learning(
            translatedText = translation,
            dictionaryEntries = emptyList(),
            contextExplanation = explanation
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }

    private fun JsonObject.synonyms(): List<Synonym> =
        runCatching {
            this["synonyms"]?.jsonArray.orEmpty().mapNotNull {
                Synonym(word = it.jsonPrimitive.contentOrNull ?: return@mapNotNull null, context = null)
            }
        }.getOrDefault(emptyList())

    private fun Exception.toUserFriendlyError(): Exception = when (this) {
        is TranslationException -> this
        is LlmException.InvalidApiKey -> TranslationException(TranslationErrors.LLM_KEY_INVALID, this)
        is LlmException.RateLimited -> TranslationException(TranslationErrors.LLM_RATE_LIMITED, this)
        is LlmException.Network -> TranslationException(TranslationErrors.LLM_NETWORK, this)
        is LlmException.InvalidBaseUrl -> TranslationException(TranslationErrors.INVALID_BASE_URL, this)
        is LlmException.HttpError -> TranslationException(TranslationErrors.LLM_SERVER, this)
        is LlmException.Truncated -> TranslationException(TranslationErrors.TRUNCATED, this)
        else -> TranslationException(TranslationErrors.GENERIC, this)
    }

    private companion object {
        const val LONG_STANDARD_MAX_TOKENS = 8192

        val STREAMABLE_MODES = setOf(
            TranslationMode.NATURAL,
            TranslationMode.CONCISE,
            TranslationMode.FORMAL,
            TranslationMode.STANDARD
        )
    }
}
