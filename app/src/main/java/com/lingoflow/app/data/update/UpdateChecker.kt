package com.lingoflow.app.data.update

import com.lingoflow.app.BuildConfig
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** Latest GitHub release summary for the update check. */
data class ReleaseInfo(
    val tagName: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?
) {
    /** True when this release is newer than the installed build. */
    val isNewerThanInstalled: Boolean
        get() = VersionCompare.isNewer(tagName, BuildConfig.VERSION_NAME)
}

/** Queries the GitHub Releases API for the newest published release. */
@Singleton
class UpdateChecker @Inject constructor(
    private val client: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkLatestRelease(): Result<ReleaseInfo> {
        val request = Request.Builder()
            .url(RELEASES_API_URL)
            .header("Accept", "application/vnd.github+json")
            .build()

        val response = try {
            client.newCall(request).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            return Result.failure(e)
        }

        response.use {
            if (!it.isSuccessful) {
                return Result.failure(IOException("GitHub API error: HTTP ${it.code}"))
            }
            return try {
                val body = it.body?.string().orEmpty()
                val root = json.parseToJsonElement(body).jsonObject
                val assets = root["assets"]?.jsonArray.orEmpty()
                val apkUrl = assets.firstOrNull { asset ->
                    asset.jsonObject["browser_download_url"]
                        ?.jsonPrimitive?.contentOrNull
                        ?.endsWith(".apk") == true
                }?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.contentOrNull

                Result.success(
                    ReleaseInfo(
                        tagName = root["tag_name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        htmlUrl = root["html_url"]?.jsonPrimitive?.contentOrNull
                            ?: REPO_PAGE_URL,
                        apkDownloadUrl = apkUrl
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

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
        const val REPO_PAGE_URL = "https://github.com/coderirse/LingoFlow"
        const val RELEASES_API_URL =
            "https://api.github.com/repos/coderirse/LingoFlow/releases/latest"
    }
}
