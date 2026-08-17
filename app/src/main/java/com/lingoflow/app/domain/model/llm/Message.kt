package com.lingoflow.app.domain.model.llm

/** A single chat message. [role] is one of "system", "user", "assistant". */
data class Message(
    val role: String,
    val content: String
)
