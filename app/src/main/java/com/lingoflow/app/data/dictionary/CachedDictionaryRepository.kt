package com.lingoflow.app.data.dictionary

import com.lingoflow.app.domain.model.dictionary.DictionaryEntry
import com.lingoflow.app.domain.repository.DictionaryRepository

/**
 * In-memory caching [DictionaryRepository] decorator. Successful lookups are
 * kept in a small LRU map so repeated lookups of the same word are instant;
 * failures are never cached. Capped at [MAX_ENTRIES] with eldest eviction.
 */
class CachedDictionaryRepository(
    private val delegate: DictionaryRepository
) : DictionaryRepository {

    private val cache = object :
        LinkedHashMap<String, List<DictionaryEntry>>(16, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, List<DictionaryEntry>>?
        ): Boolean = size > MAX_ENTRIES
    }

    override suspend fun lookup(word: String): Result<List<DictionaryEntry>> {
        val key = word.trim().lowercase()
        synchronized(cache) {
            cache[key]?.let { return Result.success(it) }
        }
        return delegate.lookup(key).onSuccess { entries ->
            synchronized(cache) { cache[key] = entries }
        }
    }

    override suspend fun search(word: String): Result<List<DictionaryEntry>> = lookup(word)

    private companion object {
        const val MAX_ENTRIES = 64
    }
}
