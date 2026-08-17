package com.lingoflow.app.domain.exception

/** Failures surfaced by the dictionary pipeline, mapped to friendly UI messages. */
sealed class DictionaryException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {

    class NoApiKey : DictionaryException("Merriam-Webster API key is not set")

    class InvalidApiKey : DictionaryException("Merriam-Webster API key was rejected")

    class Network(cause: Throwable? = null) : DictionaryException("Network error", cause)

    class NotFound(
        val suggestions: List<String> = emptyList()
    ) : DictionaryException("Word not found")

    class ParseError(cause: Throwable) : DictionaryException("Failed to parse response", cause)
}
