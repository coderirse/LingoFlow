package com.lingoflow.app.domain.model.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatRequestTest {

    @Test
    fun `temperature defaults to 0_7 and maxTokens to null`() {
        val request = ChatRequest(
            model = "deepseek-chat",
            messages = listOf(Message(role = "user", content = "Hi"))
        )
        assertEquals(0.7f, request.temperature)
        assertNull(request.maxTokens)
    }

    @Test
    fun `explicit values are kept`() {
        val request = ChatRequest(
            model = "deepseek-chat",
            messages = listOf(
                Message(role = "system", content = "You are a translator."),
                Message(role = "user", content = "Hi")
            ),
            temperature = 0.2f,
            maxTokens = 256
        )
        assertEquals(0.2f, request.temperature)
        assertEquals(256, request.maxTokens)
        assertEquals(2, request.messages.size)
    }
}
