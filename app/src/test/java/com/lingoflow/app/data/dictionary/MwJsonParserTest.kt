package com.lingoflow.app.data.dictionary

import com.lingoflow.app.domain.exception.DictionaryException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MwJsonParserTest {

    private val entryJson = """
        [
          {
            "meta": {"id": "test:1", "uuid": "abc", "stems": ["test", "tests"]},
            "hwi": {"hw": "test", "prs": [{"mw": "ˈtest", "sound": {"audio": "test0001", "ref": "c", "stat": "1"}}]},
            "fl": "noun",
            "def": [{"sseq": [[["sense", {"sn": "1", "sls": ["formal"], "dt": [["text", "{bc}a means of testing"], ["vis", [{"t": "The {wi}test{\/wi} was hard."}]]]}]]]}],
            "shortdef": ["a means of testing"]
          }
        ]
    """.trimIndent()

    @Test
    fun `parses a full entry`() {
        val result = MwJsonParser.parse(entryJson)
        assertTrue(result.isSuccess)

        val entry = result.getOrThrow().single()
        assertEquals("test", entry.word)
        assertEquals("noun", entry.entries.single().partOfSpeech)

        val phonetic = entry.phonetics.single()
        assertEquals("ˈtest", phonetic.text)
        assertEquals(
            "https://media.merriam-webster.com/audio/prons/en/us/mp3/t/test0001.mp3",
            phonetic.audioUrl
        )
    }

    @Test
    fun `parses definitions with cleaned markup`() {
        val entry = MwJsonParser.parse(entryJson).getOrThrow().single()
        val definition = entry.entries.single().definitions.single()

        assertEquals("1", definition.senseNumber)
        assertEquals(listOf("formal"), definition.labels)
        assertEquals("a means of testing", definition.meaning)
        assertEquals("The test was hard.", definition.examples.single().sentence)
    }

    @Test
    fun `strips homograph suffix and syllable markers`() {
        val json = """
            [
              {
                "meta": {"id": "considerable:1", "stems": ["considerable"]},
                "hwi": {"hw": "con*sid*er*able"}
              }
            ]
        """.trimIndent()

        // meta.id is the source of word; hwi.hw only loses asterisks.
        val entry = MwJsonParser.parse(json).getOrThrow().single()
        assertEquals("considerable", entry.word)
        assertTrue(entry.entries.single().partOfSpeech.isEmpty())
        assertNull(entry.etymology)
    }

    @Test
    fun `string array becomes NotFound with suggestions`() {
        val result = MwJsonParser.parse("""["test", "tested", "testing"]""")

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is DictionaryException.NotFound)
        assertEquals(
            listOf("test", "tested", "testing"),
            (error as DictionaryException.NotFound).suggestions
        )
    }

    @Test
    fun `parses phrasal verbs and etymology`() {
        val json = """
            [
              {
                "meta": {"id": "abide", "stems": ["abide"]},
                "hwi": {"hw": "abide"},
                "fl": "verb",
                "def": [{"sseq": [[["sense", {"sn": "1", "dt": [["text", "{bc}to remain"]]}]]]}],
                "dros": [
                  {
                    "drp": "abide by",
                    "def": [{"sseq": [[["sense", {"dt": [["text", "{bc}to conform to"], ["vis", [{"t": "abide by the rules"}]]]}]]]}]
                  }
                ],
                "et": [["text", "Middle English, from Old English {it}ābīdan{\/it}"]]
              }
            ]
        """.trimIndent()

        val entry = MwJsonParser.parse(json).getOrThrow().single()
        assertEquals("abide by", entry.phrases.single().phrase)
        assertEquals("to conform to", entry.phrases.single().meaning)
        assertEquals(
            "abide by the rules",
            entry.phrases.single().examples.single().sentence
        )
        assertEquals("Middle English, from Old English ābīdan", entry.etymology)
    }

    @Test
    fun `invalid json becomes ParseError`() {
        val result = MwJsonParser.parse("{not json")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DictionaryException.ParseError)
    }

    @Test
    fun `audio subdirectory rules are applied`() {
        fun audioOf(name: String): String? {
            val json = """
                [{"meta": {"id": "w"}, "hwi": {"hw": "w", "prs": [{"mw": "w", "sound": {"audio": "$name"}}]}}]
            """.trimIndent()
            return MwJsonParser.parse(json).getOrThrow().single()
                .phonetics.single().audioUrl
        }

        assertEquals(
            "https://media.merriam-webster.com/audio/prons/en/us/mp3/b/batch001.mp3",
            audioOf("batch001")
        )
        assertEquals(
            "https://media.merriam-webster.com/audio/prons/en/us/mp3/bix/bix001.mp3",
            audioOf("bix001")
        )
        assertEquals(
            "https://media.merriam-webster.com/audio/prons/en/us/mp3/gg/ggtest01.mp3",
            audioOf("ggtest01")
        )
        assertEquals(
            "https://media.merriam-webster.com/audio/prons/en/us/mp3/number/1std0001.mp3",
            audioOf("1std0001")
        )
    }
}
