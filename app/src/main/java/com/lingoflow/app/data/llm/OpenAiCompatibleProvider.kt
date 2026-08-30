package com.lingoflow.app.data.llm

import com.lingoflow.app.data.common.await
import com.lingoflow.app.domain.exception.LlmException
import com.lingoflow.app.domain.model.llm.ChatRequest
import com.lingoflow.app.domain.model.llm.ChatResponse
import com.lingoflow.app.domain.model.llm.LlmProvider
import com.lingoflow.app.domain.model.llm.TokenUsage
import com.lingoflow.app.domain.model.settings.ProviderConfig
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * OpenAI-compatible chat completions client. Serves DeepSeek, OpenAI,
 * Gemini and Moonshot through their native OpenAI-compatible endpoints, and
 * any Anthropic/Gemini gateway the user configures via a CUSTOM base URL.
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

    /** Parsed once; a blank/invalid URL fails fast with a typed error. */
    private val endpointUrl: HttpUrl =
        resolveEndpointUrl()

    private fun resolveEndpointUrl(): HttpUrl {
        val base = (config.baseUrl?.ifBlank { null } ?: config.providerId.defaultBaseUrl)
            .trim()
            .trimEnd('/')
        if (base.isEmpty()) throw LlmException.InvalidBaseUrl()
        return base.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addPathSegments("chat/completions")
            ?.build()
            ?: throw LlmException.InvalidBaseUrl()
    }

    override suspend fun chat(request: ChatRequest): ChatResponse =
        // The whole call — including the blocking body download and JSON
        // parse — runs on OkHttp's own threads; only the finished
        // ChatResponse resumes the caller. The caller therefore never sits
        // inside a blocking read, and cancelling it cancels the HTTP call,
        // which aborts the download immediately. (Resuming the caller at
        // onResponse and reading the body on the caller's dispatcher used
        // to leave cancellation waiting for the full body download.)
        suspendCancellableCoroutine { continuation ->
            val httpRequest = Request.Builder()
                .url(endpointUrl)
                .header("Authorization", "Bearer ${config.apiKey}")
                .post(buildRequestBody(request, stream = false))
                .build()

            val call = client.newCall(httpRequest)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : okhttp3.Callback {
                override fun onResponse(call: okhttp3.Call, response: Response) {
                    val outcome = runCatching { parseBody(response) }
                    if (continuation.isActive) {
                        continuation.resumeWith(outcome)
                    }
                }

                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(LlmException.Network(e))
                    }
                }
            })
        }

    private fun parseBody(response: Response): ChatResponse = response.use {
        // The body can only be consumed once — read it first, then decide
        // whether it is an error payload or a chat completion.
        val body = it.body?.string().orEmpty()
        throwForHttpError(it.code, body)
        try {
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

    override suspend fun chatStream(request: ChatRequest): Flow<String> = channelFlow {
        val httpRequest = Request.Builder()
            .url(endpointUrl)
            .header("Authorization", "Bearer ${config.apiKey}")
            .post(buildRequestBody(request, stream = true))
            .build()

        val call = client.newCall(httpRequest)
        // The SSE loop runs on IO and suspends in send(): backpressure
        // instead of the lossy trySend, so a slow collector can never drop
        // a delta (one dropped character is a typo in the translation).
        launch(Dispatchers.IO) {
            try {
                call.await().use { response ->
                    if (!response.isSuccessful) {
                        throwForHttpError(response.code, response.body?.string())
                        return@use
                    }
                    var lastFinishReason: String? = null
                    response.body?.source()?.use { source ->
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (!line.startsWith("data:")) continue
                            val data = line.removePrefix("data:").trim()
                            if (data.isEmpty()) continue
                            if (data == "[DONE]") break
                            // One malformed keep-alive/comment line from a
                            // non-conforming gateway must not kill the whole
                            // stream — skip it and keep reading.
                            val chunk = runCatching {
                                json.decodeFromString<ChatCompletionChunk>(data)
                            }.getOrNull() ?: continue
                            val choice = chunk.choices.firstOrNull()
                            choice?.finishReason?.let { lastFinishReason = it }
                            val delta = choice?.delta?.content
                            if (!delta.isNullOrEmpty()) {
                                send(delta)
                            }
                        }
                    }
                    // finish_reason == "length" means the model hit its
                    // output cap: end the stream with an error instead of
                    // pretending it completed. Deltas already delivered stay
                    // with the collector as a partial result.
                    if (lastFinishReason == "length") {
                        close(LlmException.Truncated())
                    }
                }
                close()
            } catch (e: CancellationException) {
                // Collector went away; awaitClose already cancelled the call.
            } catch (e: LlmException) {
                close(e)
            } catch (e: IOException) {
                // Mid-stream read failures (timeouts, dropped connections)
                // are network problems, not malformed data.
                close(LlmException.Network(e))
            } catch (e: Exception) {
                close(LlmException.ParseError(e))
            }
        }
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

    private fun throwForHttpError(code: Int, errorBody: String?) {
        when (code) {
            401, 403 -> throw LlmException.InvalidApiKey()
            429 -> throw LlmException.RateLimited()
            in 400..599 -> throw LlmException.HttpError(code, errorBody?.snippet())
        }
    }

    /** First non-empty line, trimmed and capped — enough to spot "quota exceeded". */
    private fun String.snippet(): String? =
        lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
            ?.take(MAX_ERROR_SNIPPET_LENGTH)

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val MAX_ERROR_SNIPPET_LENGTH = 160
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
    data class Choice(
        val delta: Delta? = null,
        @SerialName("finish_reason") val finishReason: String? = null
    )

    @Serializable
    data class Delta(val content: String? = null)
}
