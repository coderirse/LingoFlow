package com.lingoflow.app.domain.repository

import com.lingoflow.app.domain.model.dictionary.DictionaryEntry

// TODO: Prompt 5 接入 Merriam-Webster 实现
interface DictionaryRepository {

    /** Looks up a headword; MW may return several homograph entries. */
    suspend fun lookup(word: String): Result<List<DictionaryEntry>>

    /** Searches headwords matching a query (may return several entries). */
    suspend fun search(word: String): Result<List<DictionaryEntry>>
}
