package com.lingoflow.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class WordLineSplitterTest {

    @Test
    fun `paragraph gaps become empty line entries`() {
        assertEquals(
            listOf("first paragraph", "", "second paragraph"),
            WordLineSplitter.lines("first paragraph\n\nsecond paragraph")
        )
    }

    @Test
    fun `runs of blank lines collapse into one gap`() {
        assertEquals(
            listOf("a", "", "b"),
            WordLineSplitter.lines("a\n\n\n\nb")
        )
    }

    @Test
    fun `leading and trailing blank lines are dropped`() {
        assertEquals(
            listOf("a", "", "b"),
            WordLineSplitter.lines("\n\na\n\nb\n\n")
        )
    }

    @Test
    fun `windows line breaks are handled`() {
        assertEquals(
            listOf("a", "", "b"),
            WordLineSplitter.lines("a\r\n\r\nb")
        )
    }

    @Test
    fun `single line has no gaps`() {
        assertEquals(
            listOf("one two three"),
            WordLineSplitter.lines("one two three")
        )
    }

    @Test
    fun `blank input produces no lines`() {
        assertEquals(emptyList<String>(), WordLineSplitter.lines("  \n  \n"))
    }
}
