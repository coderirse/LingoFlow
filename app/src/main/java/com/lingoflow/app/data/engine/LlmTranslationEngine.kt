package com.lingoflow.app.data.engine

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
import com.lingoflow.app.domain.model.translation.TranslationRequest
import com.lingoflow.app.domain.model.translation.TranslationResponse
import com.lingoflow.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : TranslationEngine {

    private val _status = MutableStateFlow(TranslationStatus.IDLE)
    override val status: StateFlow<TranslationStatus> = _status.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun translate(
        request: TranslationRequest
    ): Result<TranslationResponse> {
        if (request.mode == TranslationMode.STANDARD) {
            return Result.failure(TranslationException("STANDARD mode is handled on-device."))
        }

        val settings = settingsRepository.getSettings()
        val config = settings.llmProviders[settings.activeLlmProviderId]
            ?: ProviderConfig(
                providerId = settings.activeLlmProviderId,
                apiKey = "",
                baseUrl = null,
                model = settings.activeLlmProviderId.defaultModel
            )
        if (config.apiKey.isBlank()) {
            return Result.failure(TranslationException("LLM API key is not configured."))
        }

        val provider = providerFactory(config)
        val chatRequest = ChatRequest(
            model = config.model.ifBlank { config.providerId.defaultModel },
            messages = listOf(
                Message(role = "system", content = systemPrompt(request)),
                Message(role = "user", content = request.text)
            ),
            temperature = config.temperature
        )

        return try {
            _status.value = TranslationStatus.TRANSLATING
            val response = provider.chat(chatRequest)
            Result.success(buildResponse(request.mode, response.content))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toUserFriendlyError())
        } finally {
            _status.value = TranslationStatus.IDLE
        }
    }

    private fun systemPrompt(request: TranslationRequest): String {
        val source = request.sourceLanguage.displayName
        val target = request.targetLanguage.displayName
        return when (request.mode) {
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
                "You are an English learning assistant. Translate the user's text from $source " +
                    "to $target, then analyze it for a language learner. " +
                    "Respond with ONLY a JSON object (no markdown fences) with these keys: " +
                    "\"translation\" (string), \"meaning_in_context\" (string, explain the key " +
                    "word or phrase usage), \"grammar_note\" (string or null), " +
                    "\"usage_note\" (string or null), \"synonyms\" (array of strings)."
            TranslationMode.STANDARD -> ""
        }
    }

    private fun buildResponse(
        mode: TranslationMode,
        content: String
    ): TranslationResponse {
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
        is LlmException.InvalidApiKey ->
            TranslationException("LLM API key is invalid. Please check your Settings.")
        is LlmException.RateLimited ->
            TranslationException("LLM rate limit reached. Please try again later.")
        is LlmException.Network ->
            TranslationException("Network error. Please check your connection.")
        else -> TranslationException("Translation failed. Please try again.", this)
    }
}
