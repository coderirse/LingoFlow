package com.lingoflow.app.data.llm

import com.lingoflow.app.domain.exception.LlmException
import com.lingoflow.app.domain.model.llm.ChatRequest
import com.lingoflow.app.domain.model.llm.LlmProviderId
import com.lingoflow.app.domain.model.llm.Message
import com.lingoflow.app.domain.model.settings.ProviderConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OpenAiCompatibleProviderTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun createProvider(apiKey: String = "sk-test") = OpenAiCompatibleProvider(
        client = OkHttpClient(),
        config = ProviderConfig(
            providerId = LlmProviderId.CUSTOM,
            apiKey = apiKey,
            baseUrl = server.url("/v1").toString(),
            model = "test-model"
        )
    )

    private fun chatRequest() = ChatRequest(
        model = "test-model",
        messages = listOf(Message(role = "user", content = "Hello"))
    )

    @Test
    fun `chat posts request and parses response`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "choices": [
                    {"message": {"role": "assistant", "content": "你好"}, "finish_reason": "stop"}
                  ],
                  "usage": {"prompt_tokens": 5, "completion_tokens": 3}
                }
                """.trimIndent()
            )
        )

        val response = createProvider().chat(chatRequest())

        assertEquals("你好", response.content)
        assertEquals("stop", response.finishReason)
        assertEquals(5, response.usage?.promptTokens)
        assertEquals(3, response.usage?.completionTokens)

        val recorded = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("/v1/chat/completions", recorded.path)
        assertEquals("Bearer sk-test", recorded.getHeader("Authorization"))
        assertTrue(recorded.body.readUtf8().contains("\"model\":\"test-model\""))
    }

    @Test
    fun `chatStream emits deltas until DONE`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                data: {"choices": [{"delta": {"content": "你"}}]}

                data: {"choices": [{"delta": {"content": "好"}}]}

                data: [DONE]

                """.trimIndent()
            )
        )

        val chunks = createProvider().chatStream(chatRequest()).toList()

        assertEquals(listOf("你", "好"), chunks)
    }

    @Test
    fun `chatStream ends with Truncated when finish_reason is length`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                data: {"choices": [{"delta": {"content": "部分"}}]}

                data: {"choices": [{"delta": {}, "finish_reason": "length"}]}

                data: [DONE]

                """.trimIndent()
            )
        )

        var received = mutableListOf<String>()
        try {
            createProvider().chatStream(chatRequest()).collect { received += it }
            org.junit.Assert.fail("Expected LlmException.Truncated")
        } catch (e: LlmException.Truncated) {
            // Deltas streamed before the cap are still delivered.
            assertEquals(listOf("部分"), received)
        }
    }

    @Test
    fun `http 401 maps to InvalidApiKey`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        try {
            createProvider().chat(chatRequest())
            org.junit.Assert.fail("Expected LlmException.InvalidApiKey")
        } catch (e: LlmException.InvalidApiKey) {
            // expected
        }
    }

    @Test
    fun `http 429 maps to RateLimited`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429))

        try {
            createProvider().chat(chatRequest())
            org.junit.Assert.fail("Expected LlmException.RateLimited")
        } catch (e: LlmException.RateLimited) {
            // expected
        }
    }

    @Test
    fun `cancelling chat is prompt while the response body is still downloading`() {
        // Headers arrive immediately but the body drips in over seconds —
        // the shape OpenAI-compatible gateways produce for non-streaming
        // completions. Regression guard: the caller must not stay trapped
        // inside a blocking body read after cancellation (it used to wait
        // for the entire download before the CancellationException landed).
        val body = """{"choices":[{"message":{"content":"${"x".repeat(2048)}"}}]}"""
        server.enqueue(
            MockResponse()
                .setBodyDelay(10, TimeUnit.SECONDS)
                .setBody(body)
        )

        kotlinx.coroutines.runBlocking {
            val job = launch {
                createProvider().chat(chatRequest())
            }
            // Let the call get in flight before cancelling it.
            Thread.sleep(500)
            val startAt = System.nanoTime()
            job.cancel()
            job.join()
            val elapsedMs = (System.nanoTime() - startAt) / 1_000_000
            assertTrue(
                "cancellation took ${elapsedMs}ms to surface",
                elapsedMs < 2_000
            )
        }    }
}
