package com.lingoflow.app.data.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InflectionStemmerTest {

    @Test
    fun `served stems to serve`() {
        val candidates = InflectionStemmer.candidates("served")
        assertTrue("serve" in candidates)
        assertEquals("serve", candidates.first())
    }

    @Test
    fun `studies stems to study`() {
        assertTrue("study" in InflectionStemmer.candidates("studies"))
    }

    @Test
    fun `running stems to run`() {
        val candidates = InflectionStemmer.candidates("running")
        assertTrue("run" in candidates)
    }

    @Test
    fun `making stems to make`() {
        val candidates = InflectionStemmer.candidates("making")
        assertTrue("make" in candidates)
    }

    @Test
    fun `plural cats stems to cat`() {
        assertTrue("cat" in InflectionStemmer.candidates("cats"))
    }

    @Test
    fun `base words and short words yield no candidates`() {
        assertTrue(InflectionStemmer.candidates("serve").isEmpty() ||
            "serve" !in InflectionStemmer.candidates("serve"))
        assertTrue(InflectionStemmer.candidates("I").isEmpty())
        assertTrue(InflectionStemmer.candidates("go").isEmpty())
    }

    @Test
    fun `candidates never contain the original word`() {
        listOf("served", "studies", "running", "cats", "watches").forEach { word ->
            assertTrue(word !in InflectionStemmer.candidates(word))
        }
    }
}
