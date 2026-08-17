package com.lingoflow.app.domain.model.llm

/** Token accounting for a chat completion, when reported by the provider. */
data class TokenUsage(
    val promptTokens: Int?,
    val completionTokens: Int?
)
