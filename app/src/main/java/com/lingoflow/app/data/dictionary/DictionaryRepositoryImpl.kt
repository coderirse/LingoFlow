package com.lingoflow.app.data.dictionary

import com.lingoflow.app.domain.exception.DictionaryException
import com.lingoflow.app.domain.model.dictionary.DictionaryEntry
import com.lingoflow.app.domain.repository.DictionaryRepository
import com.lingoflow.app.domain.repository.SettingsRepository
import java.io.IOException
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Merriam-Webster Collegiate Dictionary client. Reads the API key from
 * [SettingsRepository] on every lookup so Settings edits take effect
 * immediately.
 */
class DictionaryRepositoryImpl(
    private val client: OkHttpClient,
    private val settingsRepository: SettingsRepository,
    private val baseUrl: String = DEFAULT_BASE_URL
) : DictionaryRepository {

    override suspend fun lookup(word: String): Result<List<DictionaryEntry>> {
        if (word.isBlank()) {
            return Result.failure(DictionaryException.NotFound())
        }
        val apiKey = settingsRepository.getSettings().dictionaryApiKey
        if (apiKey.isBlank()) {
            return Result.failure(DictionaryException.NoApiKey())
        }

        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegment(word.trim())
            .addQueryParameter("key", apiKey)
            .build()

        // await() resumes on the caller's dispatcher (Main for ViewModels) and
        // body.string() is a blocking read — keep all of it off the main
        // thread or the UI freezes for the whole download.
        return withContext(Dispatchers.IO) {
            val response = try {
                client.newCall(Request.Builder().url(url).build()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                return@withContext Result.failure(DictionaryException.Network(e))
            }

            response.use {
                when {
                    it.code == 403 -> Result.failure(DictionaryException.InvalidApiKey())
                    !it.isSuccessful -> Result.failure(DictionaryException.Network())
                    else -> {
                        val body = it.body?.string().orEmpty()
                        // MW answers rejected/unsubscribed keys with HTTP 200 and a
                        // plain-text error; surface it as InvalidApiKey instead of
                        // letting it fail JSON parsing further down.
                        if (body.startsWith("Invalid API key")) {
                            Result.failure(DictionaryException.InvalidApiKey())
                        } else {
                            MwJsonParser.parse(body)
                        }
                    }
                }
            }
        }
    }

    // MW uses one endpoint for lookup and search.
    override suspend fun search(word: String): Result<List<DictionaryEntry>> = lookup(word)

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWith(Result.failure(e))
                }
            }
        })
    }

    companion object {
        const val DEFAULT_BASE_URL =
            "https://www.dictionaryapi.com/api/v3/references/collegiate/json/"
    }
}
