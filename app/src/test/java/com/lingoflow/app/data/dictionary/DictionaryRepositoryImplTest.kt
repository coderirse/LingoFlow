package com.lingoflow.app.data.dictionary

import com.lingoflow.app.data.repository.FakeSettingsRepository
import com.lingoflow.app.domain.exception.DictionaryException
import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.settings.AppSettings
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DictionaryRepositoryImplTest {

    private lateinit var server: MockWebServer

    private val entryJson = """
        [
          {
            "meta": {"id": "test:1", "stems": ["test"]},
            "hwi": {"hw": "test", "prs": [{"mw": "ˈtest"}]},
            "fl": "noun",
            "def": [{"sseq": [[["sense", {"sn": "1", "dt": [["text", "{bc}a means of testing"]]}]]]}],
            "shortdef": ["a means of testing"]
          }
        ]
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun settingsWithKey(key: String) = FakeSettingsRepository(
        AppSettings(
            activeLlmProviderId = LlmProviderId.DEEPSEEK,
            llmProviders = emptyMap(),
            dictionaryApiKey = key
        )
    )

    private fun createRepository(
        apiKey: String = "valid-key",
        client: OkHttpClient = OkHttpClient()
    ) = DictionaryRepositoryImpl(
        client = client,
        settingsRepository = settingsWithKey(apiKey),
        baseUrl = server.url("/api/v3/references/collegiate/json/").toString()
    )

    @Test
    fun `successful lookup returns entries`() = runTest {
        server.enqueue(MockResponse().setBody(entryJson))

        val result = createRepository().lookup("test")

        assertTrue(result.isSuccess)
        assertEquals("test", result.getOrThrow().single().word)

        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertTrue(request.path!!.startsWith("/api/v3/references/collegiate/json/test"))
        assertTrue(request.path!!.contains("key=valid-key"))
    }

    @Test
    fun `http 403 becomes InvalidApiKey`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))

        val result = createRepository().lookup("test")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DictionaryException.InvalidApiKey)
    }

    @Test
    fun `blank api key short circuits with NoApiKey`() = runTest {
        val result = createRepository(apiKey = "").lookup("test")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DictionaryException.NoApiKey)
        // No request should have been made.
        assertTrue(server.takeRequest(200, TimeUnit.MILLISECONDS) == null)
    }

    @Test
    fun `stalled server becomes Network error`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val impatientClient = OkHttpClient.Builder()
            .readTimeout(200, TimeUnit.MILLISECONDS)
            .build()

        val result = createRepository(client = impatientClient).lookup("test")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DictionaryException.Network)
    }

    @Test
    fun `http 500 becomes Network error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = createRepository().lookup("test")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DictionaryException.Network)
    }
}
