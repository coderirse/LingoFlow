package com.lingoflow.app.domain.model.llm

import kotlinx.coroutines.flow.Flow

/** An LLM backend able to serve OpenAI-compatible chat completions. */
interface LlmProvider {
    val id: String
    val name: String
    val defaultBaseUrl: String
    val defaultModel: String

    suspend fun chat(request: ChatRequest): ChatResponse

    /** Streams response deltas (content fragments) as they arrive. */
    suspend fun chatStream(request: ChatRequest): Flow<String>
}
