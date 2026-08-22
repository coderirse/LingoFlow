package com.lingoflow.app.domain.model.translation

/**
 * Stable keys for transient translation notices. The data layer emits keys
 * instead of user-facing text so the UI layer can localize them; values
 * that are not known keys pass through unchanged.
 */
object TranslationNotices {
    /** No LLM API key configured; the on-device engine was used instead. */
    const val LLM_KEY_MISSING = "notice_llm_key_missing"

    /** The LLM call failed; the on-device engine was used instead. */
    const val LLM_FAILED = "notice_llm_failed"

    /** A streaming translation ended early; the partial result was kept. */
    const val STREAM_INTERRUPTED = "notice_stream_interrupted"
}
