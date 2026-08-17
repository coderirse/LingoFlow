package com.lingoflow.app.data.translator

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.lingoflow.app.domain.Translator
import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.TranslationException
import com.lingoflow.app.domain.model.TranslationResult
import com.lingoflow.app.domain.model.TranslationStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * On-device [Translator] backed by ML Kit Translation and Language
 * Identification. Translation clients are cached per language pair for the
 * lifetime of the app process (this class is a [Singleton]), and models are
 * downloaded once via [RemoteModelManager] before the first translation.
 */
@Singleton
class MlKitTranslator @Inject constructor() : Translator {

    private val _status = MutableStateFlow(TranslationStatus.IDLE)
    override val status: StateFlow<TranslationStatus> = _status.asStateFlow()

    private val languageIdentifier: LanguageIdentifier = LanguageIdentification.getClient()
    private val modelManager: RemoteModelManager = RemoteModelManager.getInstance()

    // com.google.mlkit.nl.translate.Translator instances are heavyweight and
    // own on-device models; create at most one per language pair.
    private val clients = mutableMapOf<Pair<Language, Language>, com.google.mlkit.nl.translate.Translator>()

    override suspend fun translate(
        text: String,
        sourceLanguage: Language,
        targetLanguage: Language
    ): Result<TranslationResult> {
        if (text.isBlank()) {
            return Result.failure(TranslationException("Nothing to translate."))
        }
        return try {
            val resolvedSource = resolveSourceLanguage(text, sourceLanguage)
            if (resolvedSource == targetLanguage) {
                return Result.success(
                    TranslationResult(text, text, resolvedSource, targetLanguage)
                )
            }

            val client = getOrCreateClient(resolvedSource, targetLanguage)
            ensureModelReady(client, targetLanguage)

            _status.value = TranslationStatus.TRANSLATING
            val translated = client.translate(text).await()

            Result.success(
                TranslationResult(
                    originalText = text,
                    translatedText = translated,
                    sourceLanguage = resolvedSource,
                    targetLanguage = targetLanguage
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toUserFriendlyError())
        } finally {
            _status.value = TranslationStatus.IDLE
        }
    }

    private suspend fun resolveSourceLanguage(text: String, source: Language): Language {
        if (source != Language.AUTO) return source

        val tag = try {
            languageIdentifier.identifyLanguage(text).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The on-device language-id module failed (e.g. not yet
            // downloaded and no network). Guide the user instead of
            // surfacing a raw ML Kit error.
            throw TranslationException(
                "Language detection is unavailable. Please select the source language manually."
            )
        }
        if (tag != UNDETERMINED_LANGUAGE_TAG) {
            return Language.fromCode(tag)
                ?: throw TranslationException("Detected language is not supported yet.")
        }

        // identifyLanguage() gives up on short or ambiguous text ("und");
        // fall back to the candidate list before erroring out.
        val candidates = try {
            languageIdentifier.identifyPossibleLanguages(text).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        val best = candidates.maxByOrNull { it.confidence }?.languageTag
        return best?.let(Language::fromCode)
            ?: throw TranslationException(
                "Couldn't detect the language. Please select it manually."
            )
    }

    private fun getOrCreateClient(
        source: Language,
        target: Language
    ): com.google.mlkit.nl.translate.Translator {
        val sourceTag = TranslateLanguage.fromLanguageTag(requireNotNull(source.code))
        val targetTag = TranslateLanguage.fromLanguageTag(requireNotNull(target.code))
        require(sourceTag != null && targetTag != null) { "Unsupported language pair" }

        return synchronized(clients) {
            clients.getOrPut(source to target) {
                Translation.getClient(
                    TranslatorOptions.Builder()
                        .setSourceLanguage(sourceTag)
                        .setTargetLanguage(targetTag)
                        .build()
                )
            }
        }
    }

    private suspend fun ensureModelReady(
        client: com.google.mlkit.nl.translate.Translator,
        target: Language
    ) {
        // downloadModelIfNeeded() fetches both source and target models, so we
        // only probe the target model to decide whether to show "preparing".
        val targetModel = TranslateRemoteModel.Builder(requireNotNull(target.code)).build()
        val targetDownloaded = modelManager.isModelDownloaded(targetModel).await()
        if (!targetDownloaded) {
            _status.value = TranslationStatus.PREPARING_MODEL
        }
        client.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
    }

    private fun Exception.toUserFriendlyError(): Exception = when (this) {
        is TranslationException -> this
        is MlKitException -> when (errorCode) {
            MlKitException.UNAVAILABLE, MlKitException.NETWORK_ISSUE ->
                TranslationException("Translation model is unavailable. Check your network connection.")
            MlKitException.NOT_ENOUGH_SPACE ->
                TranslationException("Not enough storage to download the translation model.")
            else ->
                TranslationException("Translation failed. Please try again.", this)
        }
        else -> TranslationException("Translation failed. Please try again.", this)
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
        addOnCanceledListener { continuation.cancel() }
    }

    private companion object {
        const val UNDETERMINED_LANGUAGE_TAG = "und"
    }
}
