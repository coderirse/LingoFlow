package com.lingoflow.app.data.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LongTextFormatterTest {

    @Test
    fun `numbered items are put on separate lines`() {
        val formatted = LongTextFormatter.format(
            "Intro. 1. First item 2. Second item 3. Third item"
        )

        assertTrue(formatted.contains("\n1. First item"))
        assertTrue(formatted.contains("\n2. Second item"))
        assertTrue(formatted.contains("\n3. Third item"))
    }

    @Test
    fun `escaped newlines become real line breaks`() {
        val formatted = LongTextFormatter.format(
            "First paragraph\\n\\nSecond paragraph"
        )

        assertTrue(formatted.contains("First paragraph\n\nSecond paragraph"))
    }

    @Test
    fun `bullets after a sentence end start a new line`() {
        val formatted = LongTextFormatter.format(
            "Here is the plan: - item one"
        )

        assertTrue(formatted.contains("\n- item one"))
    }

    @Test
    fun `mid-sentence dashes are not split`() {
        val text = "A rare - if delightful - mistake stays inline"

        assertEquals(text, LongTextFormatter.format(text))
    }

    @Test
    fun `capitalized segments inside a sentence are not headings`() {
        val text = "The Steps: three parts of the process follow"

        assertEquals(text, LongTextFormatter.format(text))
    }

    @Test
    fun `dates are not split into numbered items`() {
        val formatted = LongTextFormatter.format("In 2026. The next sentence.")

        assertEquals("In 2026. The next sentence.", formatted)
    }

    @Test
    fun `section headings are separated from surrounding text`() {
        val formatted = LongTextFormatter.format(
            "Done. Evidence to prepare for upload:1. Order screenshot"
        )

        assertTrue(formatted.contains("\nEvidence to prepare for upload:"))
        assertTrue(formatted.contains("\n1. Order screenshot"))
    }
}
