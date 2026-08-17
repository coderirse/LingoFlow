package com.lingoflow.app.data.translator

import com.lingoflow.app.domain.Translator
import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.TranslationException
import com.lingoflow.app.domain.model.TranslationResult
import com.lingoflow.app.domain.model.TranslationStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Development stub for [Translator]. Returns canned translations for a few
 * common phrases and echoes everything else with a marker prefix, so the full
 * UI → ViewModel → UseCase → Repository pipeline can be exercised without a
 * real translation service.
 */
@Singleton
class FakeTranslator @Inject constructor() : Translator {

    private val _status = MutableStateFlow(TranslationStatus.IDLE)
    override val status: StateFlow<TranslationStatus> = _status.asStateFlow()

    override suspend fun translate(
        text: String,
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<TranslationResult> {
        if (text.isBlank()) {
            return Result.failure(TranslationException("Nothing to translate."))
        }
        // Simulate latency so the loading state is observable.
        _status.value = TranslationStatus.TRANSLATING
        try {
            delay(FAKE_LATENCY_MS)

            val detectedSource = if (sourceLanguage == Language.AUTO) {
                detectLanguage(text)
            } else {
                sourceLanguage
            }

            val translated = when {
                detectedSource == targetLanguage -> text
                else -> knownTranslations[text.trim().lowercase()]
                    ?: "[Fake Translation] $text"
            }

            return Result.success(
                TranslationResult(
                    originalText = text,
                    translatedText = translated,
                    sourceLanguage = detectedSource,
                    targetLanguage = targetLanguage
                )
            )
        } finally {
            _status.value = TranslationStatus.IDLE
        }
    }

    private fun detectLanguage(text: String): Language =
        if (text.any { it in '\u4e00'..'\u9fff' }) Language.CHINESE else Language.ENGLISH

    private companion object {
        const val FAKE_LATENCY_MS = 500L

        val knownTranslations = mapOf(
            "hello" to "你好",
            "how are you?" to "你好吗？",
            "how are you" to "你好吗",
            "thank you" to "谢谢",
            "good morning" to "早上好"
        )
    }
}
