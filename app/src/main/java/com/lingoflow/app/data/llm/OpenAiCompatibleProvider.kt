package com.lingoflow.app.data.llm

import com.lingoflow.app.domain.exception.LlmException
import com.lingoflow.app.domain.model.llm.ChatRequest
import com.lingoflow.app.domain.model.llm.ChatResponse
import com.lingoflow.app.domain.model.llm.LlmProvider
import com.lingoflow.app.domain.model.llm.TokenUsage
import com.lingoflow.app.domain.model.settings.ProviderConfig
import java.io.IOException
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * OpenAI-compatible chat completions client. Serves DeepSeek, OpenAI,
 * Moonshot and user-defined CUSTOM endpoints; Anthropic/Gemini are expected
 * to be pointed at an OpenAI-compatible gateway via baseUrl override.
 *
 * A new lightweight instance is created per translation request; all
 * instances share the injected [OkHttpClient].
 */
class OpenAiCompatibleProvider(
    private val client: OkHttpClient,
    private val config: ProviderConfig
) : LlmProvider {

    override val id: String = config.providerId.name
    override val name: String = config.providerId.name
    override val defaultBaseUrl: String = config.providerId.defaultBaseUrl
    override val defaultModel: String = config.providerId.defaultModel

    private val json = Json { ignoreUnknownKeys = true }

    private val baseUrl: String =
        (config.baseUrl?.ifBlank { null } ?: config.providerId.defaultBaseUrl).trimEnd('/')

    override suspend fun chat(request: ChatRequest): ChatResponse {
        val httpRequest = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer ${config.apiKey}")
            .post(buildRequestBody(request, stream = false))
            .build()

        val response = try {
            client.newCall(httpRequest).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw LlmException.Network(e)
        }

        response.use {
            throwForHttpError(it.code)
            val body = it.body?.string().orEmpty()
            return try {
                val parsed = json.decodeFromString<ChatCompletionResponse>(body)
                ChatResponse(
                    content = parsed.choices.firstOrNull()?.message?.content.orEmpty(),
                    finishReason = parsed.choices.firstOrNull()?.finishReason,
                    usage = parsed.usage?.let { u ->
                        TokenUsage(u.promptTokens, u.completionTokens)
                    }
                )
            } catch (e: Exception) {
                throw LlmException.ParseError(e)
            }
        }
    }

    override suspend fun chatStream(request: ChatRequest): Flow<String> = callbackFlow {
        val httpRequest = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer ${config.apiKey}")
            .post(buildRequestBody(request, stream = true))
            .build()

        val call = client.newCall(httpRequest)
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    close(runCatching { throwForHttpError(response.code) }.exceptionOrNull())
                    response.close()
                    return
                }
                try {
                    response.body?.source()?.use { source ->
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (!line.startsWith("data:")) continue
                            val data = line.removePrefix("data:").trim()
                            if (data == "[DONE]") break
                            val parsed = json.decodeFromString<ChatCompletionChunk>(data)
                            val delta = parsed.choices.firstOrNull()?.delta?.content
                            if (!delta.isNullOrEmpty()) {
                                trySend(delta)
                            }
                        }
                    }
                    close()
                } catch (e: IOException) {
                    // Mid-stream read failures (timeouts, dropped connections)
                    // are network problems, not malformed data.
                    close(LlmException.Network(e))
                } catch (e: Exception) {
                    close(LlmException.ParseError(e))
                } finally {
                    response.close()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                close(LlmException.Network(e))
            }
        })

        awaitClose { call.cancel() }
    }

    private fun buildRequestBody(request: ChatRequest, stream: Boolean) =
        buildJsonObject {
            put("model", request.model)
            putJsonArray("messages") {
                request.messages.forEach { message ->
                    addJsonObject {
                        put("role", message.role)
                        put("content", message.content)
                    }
                }
            }
            put("temperature", request.temperature)
            request.maxTokens?.let { put("max_tokens", it) }
            if (stream) put("stream", true)
        }.toString().toRequestBody(JSON_MEDIA_TYPE)

    private fun throwForHttpError(code: Int) {
        when (code) {
            401, 403 -> throw LlmException.InvalidApiKey()
            429 -> throw LlmException.RateLimited()
            in 400..599 -> throw LlmException.Network()
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWith(Result.failure(e))
                }
            }
        })
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

@Serializable
private data class ChatCompletionResponse(
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null
) {
    @Serializable
    data class Choice(
        val message: MessageBody? = null,
        @SerialName("finish_reason") val finishReason: String? = null
    )

    @Serializable
    data class MessageBody(val content: String? = null)

    @Serializable
    data class Usage(
        @SerialName("prompt_tokens") val promptTokens: Int? = null,
        @SerialName("completion_tokens") val completionTokens: Int? = null
    )
}

@Serializable
private data class ChatCompletionChunk(
    val choices: List<Choice> = emptyList()
) {
    @Serializable
    data class Choice(val delta: Delta? = null)

    @Serializable
    data class Delta(val content: String? = null)
}
