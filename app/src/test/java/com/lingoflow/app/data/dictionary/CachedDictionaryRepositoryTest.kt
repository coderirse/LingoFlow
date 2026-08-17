package com.lingoflow.app.data.dictionary

import com.lingoflow.app.domain.exception.DictionaryException
import com.lingoflow.app.domain.model.dictionary.DictionaryEntry
import com.lingoflow.app.domain.repository.DictionaryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedDictionaryRepositoryTest {

    private val entry = DictionaryEntry(
        word = "hello",
        phonetics = emptyList(),
        entries = emptyList(),
        phrases = emptyList(),
        etymology = null
    )

    private class CountingDelegate(
        var result: Result<List<DictionaryEntry>>
    ) : DictionaryRepository {
        var calls = 0
            private set

        override suspend fun lookup(word: String): Result<List<DictionaryEntry>> {
            calls++
            return result
        }

        override suspend fun search(word: String): Result<List<DictionaryEntry>> = lookup(word)
    }

    @Test
    fun `second lookup of the same word is served from cache`() = runTest {
        val delegate = CountingDelegate(Result.success(listOf(entry)))
        val repository = CachedDictionaryRepository(delegate)

        repository.lookup("hello")
        repository.lookup("hello")
        val result = repository.lookup("hello")

        assertEquals(1, delegate.calls)
        assertTrue(result.isSuccess)
        assertEquals(listOf(entry), result.getOrThrow())
    }

    @Test
    fun `cache key is case insensitive`() = runTest {
        val delegate = CountingDelegate(Result.success(listOf(entry)))
        val repository = CachedDictionaryRepository(delegate)

        repository.lookup("Hello")
        repository.lookup("HELLO")

        assertEquals(1, delegate.calls)
    }

    @Test
    fun `failures are never cached`() = runTest {
        val delegate = CountingDelegate(Result.failure(DictionaryException.Network()))
        val repository = CachedDictionaryRepository(delegate)

        repository.lookup("hello")
        repository.lookup("hello")

        assertEquals(2, delegate.calls)
    }

    @Test
    fun `different words are cached independently`() = runTest {
        val delegate = CountingDelegate(Result.success(listOf(entry)))
        val repository = CachedDictionaryRepository(delegate)

        repository.lookup("hello")
        repository.lookup("world")
        repository.lookup("hello")

        assertEquals(2, delegate.calls)
    }
}
