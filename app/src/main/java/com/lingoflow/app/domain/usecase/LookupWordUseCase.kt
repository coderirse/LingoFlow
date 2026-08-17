package com.lingoflow.app.domain.usecase

import com.lingoflow.app.domain.model.TranslationException
import com.lingoflow.app.domain.model.dictionary.PosMeaning
import com.lingoflow.app.domain.model.dictionary.WordLookupInfo
import com.lingoflow.app.domain.model.llm.ChatRequest
import com.lingoflow.app.domain.model.llm.LlmProvider
import com.lingoflow.app.domain.model.llm.Message
import com.lingoflow.app.domain.model.settings.ProviderConfig
import com.lingoflow.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Produces concise Chinese dictionary info for an English word via the active
 * LLM provider. Results are cached in memory so repeated lookups are instant.
 * Any LLM/JSON failure surfaces as a [Result.failure]; callers fall back to
 * the Merriam-Webster English entry.
 */
open class LookupWordUseCase(
    private val settingsRepository: SettingsRepository,
    private val providerFactory: (ProviderConfig) -> LlmProvider
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val cache = object :
        LinkedHashMap<String, WordLookupInfo>(16, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, WordLookupInfo>?
        ): Boolean = size > MAX_ENTRIES
    }

    open suspend operator fun invoke(word: String): Result<WordLookupInfo> {
        val key = word.trim().lowercase()
        if (key.isEmpty()) {
            return Result.failure(TranslationException("Nothing to look up."))
        }
        synchronized(cache) {
            cache[key]?.let { return Result.success(it) }
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

        return try {
            val provider = providerFactory(config)
            val response = provider.chat(
                ChatRequest(
                    model = config.model.ifBlank { config.providerId.defaultModel },
                    messages = listOf(
                        Message(role = "system", content = SYSTEM_PROMPT),
                        Message(role = "user", content = key)
                    ),
                    temperature = 0.3f,
                    maxTokens = 400
                )
            )
            val info = parse(key, response.content)
            synchronized(cache) { cache[key] = info }
            Result.success(info)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parse(word: String, content: String): WordLookupInfo {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        require(start >= 0 && end > start) { "No JSON object in LLM output" }

        val root = json.parseToJsonElement(content.substring(start, end + 1)) as? JsonObject
            ?: throw IllegalStateException("LLM output is not a JSON object")

        val entries = root["entries"]?.jsonArray.orEmpty().mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val pos = obj["partOfSpeech"]?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null
            val meanings = obj["meanings"]?.jsonArray.orEmpty()
                .mapNotNull { it.jsonPrimitive.contentOrNull }
                .filter { it.isNotBlank() }
            if (meanings.isEmpty()) null else PosMeaning(pos, meanings)
        }
        require(entries.isNotEmpty()) { "No usable entries in LLM output" }

        return WordLookupInfo(
            word = root["word"]?.jsonPrimitive?.contentOrNull ?: word,
            entries = entries,
            example = root["example"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotBlank() },
            exampleTranslation = root["exampleTranslation"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotBlank() }
        )
    }

    private companion object {
        const val MAX_ENTRIES = 64

        const val SYSTEM_PROMPT =
            "你是一个英汉词典助手。用户会给你一个英文单词，请给出简明的英汉词典信息，" +
                "严格按如下 JSON 格式返回，不要输出任何其他文字或 markdown 标记：\n" +
                "{\n" +
                "  \"word\": \"单词原型\",\n" +
                "  \"entries\": [\n" +
                "    {\"partOfSpeech\": \"词性缩写如 vt./vi./n./adj.\", \"meanings\": [\"中文释义1\", \"中文释义2\"]}\n" +
                "  ],\n" +
                "  \"example\": \"一句地道英文例句\",\n" +
                "  \"exampleTranslation\": \"例句的中文翻译\"\n" +
                "}\n" +
                "要求：entries 至少一个；meanings 用中文；例句简短自然；如果输入是变形词" +
                "（如过去式、复数），word 字段返回原型。"
    }
}
