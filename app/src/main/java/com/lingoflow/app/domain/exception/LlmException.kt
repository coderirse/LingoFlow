package com.lingoflow.app.domain.exception

/** Failures surfaced by LLM providers, mapped to friendly UI messages upstream. */
sealed class LlmException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {

    class InvalidApiKey : LlmException("LLM API key was rejected")

    class RateLimited : LlmException("LLM rate limit exceeded")

    class Network(cause: Throwable? = null) : LlmException("Network error", cause)

    class ParseError(cause: Throwable) : LlmException("Failed to parse response", cause)
}
