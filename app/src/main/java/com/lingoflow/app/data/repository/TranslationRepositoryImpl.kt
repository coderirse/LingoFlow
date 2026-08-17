package com.lingoflow.app.data.repository

import com.lingoflow.app.domain.Translator
import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.TranslationResult
import com.lingoflow.app.domain.model.TranslationStatus
import com.lingoflow.app.domain.repository.TranslationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class TranslationRepositoryImpl @Inject constructor(
    private val translator: Translator
) : TranslationRepository {

    override val status: StateFlow<TranslationStatus> = translator.status

    override suspend fun translate(
        text: String,
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<TranslationResult> = translator.translate(text, sourceLanguage, targetLanguage)
}
