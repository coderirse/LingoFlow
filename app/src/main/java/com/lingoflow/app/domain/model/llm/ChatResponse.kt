package com.lingoflow.app.domain.model.llm

/** Normalized chat completion result, independent of the provider's wire format. */
data class ChatResponse(
    val content: String,
    val finishReason: String?,
    val usage: TokenUsage?
)
