package com.lingoflow.app.domain.model.llm

/** OpenAI-compatible chat completion request. */
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float = 0.7f,
    val maxTokens: Int? = null
)
