package com.lingoflow.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageTest {

    @Test
    fun `language codes are stable`() {
        assertNull(Language.AUTO.code)
        assertEquals("en", Language.ENGLISH.code)
        assertEquals("zh", Language.CHINESE.code)
        assertEquals("ja", Language.JAPANESE.code)
        assertEquals("ko", Language.KOREAN.code)
    }

    @Test
    fun `fromCode maps exact tags`() {
        assertEquals(Language.ENGLISH, Language.fromCode("en"))
        assertEquals(Language.CHINESE, Language.fromCode("zh"))
        assertEquals(Language.JAPANESE, Language.fromCode("ja"))
        assertEquals(Language.KOREAN, Language.fromCode("ko"))
    }

    @Test
    fun `fromCode maps region tags`() {
        assertEquals(Language.CHINESE, Language.fromCode("zh-CN"))
        assertEquals(Language.ENGLISH, Language.fromCode("en-US"))
    }

    @Test
    fun `fromCode returns null for unsupported tags`() {
        assertNull(Language.fromCode("und"))
        assertNull(Language.fromCode("fr"))
    }

    @Test
    fun `target languages never include AUTO`() {
        assertFalse(Language.targetSelectable.contains(Language.AUTO))
        assertTrue(Language.targetSelectable.containsAll(Language.entries - Language.AUTO))
    }
}
