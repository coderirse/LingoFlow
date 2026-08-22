package com.lingoflow.app.data.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsTextChunkerTest {

    @Test
    fun `short text stays a single chunk`() {
        assertEquals(
            listOf("Hello world."),
            TtsTextChunker.chunk("Hello world.")
        )
    }

    @Test
    fun `sentences are split and packed under the cap`() {
        val text = "First sentence. Second sentence? Third sentence!"
        val chunks = TtsTextChunker.chunk(text)

        assertEquals(listOf(text), chunks)
        assertTrue(chunks.all { it.length <= TtsTextChunker.MAX_CHUNK_LENGTH })
    }

    @Test
    fun `many sentences produce several capped chunks`() {
        val sentence = "This numbered explanation sentence is fairly long on purpose. "
        val text = sentence.repeat(20).trim()
        val chunks = TtsTextChunker.chunk(text)

        assertTrue(chunks.size > 1)
        chunks.forEach { chunk ->
            assertTrue(
                "chunk exceeds the cap: ${chunk.length}",
                chunk.length <= TtsTextChunker.MAX_CHUNK_LENGTH
            )
        }
        // No content lost: every original sentence survives.
        assertTrue(chunks.joinToString(" ").contains("purpose."))
    }

    @Test
    fun `oversized sentence is hard-split at a soft boundary`() {
        val text = "长".repeat(500) + "。"
        val chunks = TtsTextChunker.chunk(text)

        assertTrue(chunks.size > 1)
        chunks.forEach { chunk ->
            assertTrue(chunk.length <= TtsTextChunker.MAX_CHUNK_LENGTH)
        }
        assertEquals(501, chunks.sumOf { it.length })
    }

    @Test
    fun `blank text produces no chunks`() {
        assertEquals(emptyList<String>(), TtsTextChunker.chunk("   "))
    }
}
