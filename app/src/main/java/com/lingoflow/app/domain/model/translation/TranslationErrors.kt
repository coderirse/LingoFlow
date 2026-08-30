package com.lingoflow.app.domain.model.translation

/**
 * Stable keys for user-facing translation errors. The data layer throws
 * [com.lingoflow.app.domain.model.TranslationException] carrying one of
 * these keys instead of English prose; only the UI layer (which knows the
 * active app language) turns keys into localized text. Unknown values pass
 * through unchanged so nothing is ever swallowed.
 */
object TranslationErrors {
    const val NOTHING_TO_TRANSLATE = "err_nothing_to_translate"

    const val LANGUAGE_DETECT_UNAVAILABLE = "err_language_detect_unavailable"
    const val LANGUAGE_UNSUPPORTED = "err_language_unsupported"
    const val LANGUAGE_UNDETECTED = "err_language_undetected"

    const val MODEL_UNAVAILABLE = "err_model_unavailable"
    const val NOT_ENOUGH_SPACE = "err_not_enough_space"

    const val GENERIC = "err_translation_failed"

    const val LLM_KEY_MISSING = "err_llm_key_missing"
    const val LLM_KEY_INVALID = "err_llm_key_invalid"
    const val LLM_RATE_LIMITED = "err_llm_rate_limited"
    const val LLM_NETWORK = "err_llm_network"
    const val LLM_SERVER = "err_llm_server"
    const val LLM_GENERIC = "err_llm_generic"
    const val INVALID_BASE_URL = "err_invalid_base_url"

    const val TRUNCATED = "err_truncated"
}
