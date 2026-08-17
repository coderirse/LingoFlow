package com.lingoflow.app.domain.model.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DictionaryEntryTest {

    @Test
    fun `entry aggregates phonetics senses phrases and etymology`() {
        val example = Example(sentence = "The plan worked.", source = "Merriam-Webster")
        val definition = Definition(
            meaning = "to produce the desired result",
            senseNumber = "1 a",
            labels = listOf("formal"),
            examples = listOf(example)
        )
        val entry = DictionaryEntry(
            word = "work",
            phonetics = listOf(Phonetic(text = "/ˈwərk/", audioUrl = null)),
            entries = listOf(PartOfSpeechEntry(partOfSpeech = "verb", definitions = listOf(definition))),
            phrases = listOf(
                PhrasalVerb(phrase = "work out", meaning = "to exercise", examples = listOf(example))
            ),
            etymology = "Old English weorc"
        )

        assertEquals("work", entry.word)
        assertEquals("/ˈwərk/", entry.phonetics.single().text)
        assertEquals("verb", entry.entries.single().partOfSpeech)
        assertEquals("1 a", entry.entries.single().definitions.single().senseNumber)
        assertEquals(listOf("formal"), entry.entries.single().definitions.single().labels)
        assertEquals("work out", entry.phrases.single().phrase)
        assertEquals("Old English weorc", entry.etymology)
    }

    @Test
    fun `copy preserves nested structure`() {
        val definition = Definition(
            meaning = "m",
            senseNumber = null,
            labels = emptyList(),
            examples = emptyList()
        )
        val original = DictionaryEntry(
            word = "a",
            phonetics = emptyList(),
            entries = listOf(PartOfSpeechEntry("noun", listOf(definition))),
            phrases = emptyList(),
            etymology = null
        )
        val copied = original.copy(word = "b")
        assertEquals("b", copied.word)
        assertEquals(original.entries, copied.entries)
        assertNull(copied.etymology)
    }
}
