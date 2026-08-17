package com.lingoflow.app.data.llm

import com.lingoflow.app.domain.model.llm.ChatRequest
import com.lingoflow.app.domain.model.llm.ChatResponse
import com.lingoflow.app.domain.model.llm.LlmProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Scripted [LlmProvider] for engine and router tests. */
class FakeLlmProvider(
    var chatResult: ChatResponse = ChatResponse(
        content = "fake translation",
        finishReason = "stop",
        usage = null
    ),
    var chatError: Throwable? = null,
    var streamFlow: Flow<String> = emptyFlow()
) : LlmProvider {

    override val id: String = "fake"
    override val name: String = "Fake"
    override val defaultBaseUrl: String = "http://localhost"
    override val defaultModel: String = "fake-model"

    var lastRequest: ChatRequest? = null
        private set

    override suspend fun chat(request: ChatRequest): ChatResponse {
        lastRequest = request
        chatError?.let { throw it }
        return chatResult
    }

    override suspend fun chatStream(request: ChatRequest): Flow<String> {
        lastRequest = request
        chatError?.let { throw it }
        return streamFlow
    }
}
