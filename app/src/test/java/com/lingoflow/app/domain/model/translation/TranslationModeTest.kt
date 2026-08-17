package com.lingoflow.app.domain.model.translation

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationModeTest {

    @Test
    fun `all five modes exist`() {
        val modes = TranslationMode.entries
        assertEquals(5, modes.size)
        assertEquals(
            listOf(
                TranslationMode.STANDARD,
                TranslationMode.NATURAL,
                TranslationMode.CONCISE,
                TranslationMode.FORMAL,
                TranslationMode.LEARNING
            ),
            modes
        )
    }

    @Test
    fun `STANDARD is the first and default mode`() {
        assertEquals(TranslationMode.STANDARD, TranslationMode.entries.first())
    }
}
