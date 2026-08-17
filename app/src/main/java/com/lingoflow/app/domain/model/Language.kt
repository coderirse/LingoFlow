package com.lingoflow.app.domain.model

/**
 * Languages supported by LingoFlow. [AUTO] is only valid as a source language
 * and has no BCP-47 [code] of its own.
 */
enum class Language(val displayName: String, val code: String?) {
    AUTO("Auto", null),
    ENGLISH("English", "en"),
    CHINESE("中文", "zh"),
    JAPANESE("日本語", "ja"),
    KOREAN("한국어", "ko");

    companion object {
        /** Languages that may be picked as the translation target. */
        val targetSelectable: List<Language> = entries.filter { it != AUTO }

        /**
         * Maps a BCP-47 language tag (e.g. "en", "zh-CN") to a [Language],
         * or null when the tag is not supported.
         */
        fun fromCode(code: String): Language? {
            val normalized = code.lowercase()
            return entries.firstOrNull { language ->
                language.code != null &&
                    (normalized == language.code || normalized.startsWith("${language.code}-"))
            }
        }
    }
}
