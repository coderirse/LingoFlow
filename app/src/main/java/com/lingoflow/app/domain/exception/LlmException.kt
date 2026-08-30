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

    /** finish_reason == "length": the model hit its output token cap. */
    class Truncated : LlmException("Output was cut off by the model's output limit")

    /** The configured base URL is blank or not a valid http(s) URL. */
    class InvalidBaseUrl : LlmException("Base URL is missing or invalid")

    /**
     * The server answered with an HTTP error outside the mapped codes;
     * [detail] carries a short snippet of the response body for diagnosis.
     */
    class HttpError(val code: Int, val detail: String? = null) :
        LlmException("HTTP $code${detail?.let { ": $it" }.orEmpty()}")
}
