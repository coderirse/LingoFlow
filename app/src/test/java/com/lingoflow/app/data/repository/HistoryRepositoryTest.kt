package com.lingoflow.app.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.history.TranslationHistoryItem
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.domain.repository.HistoryRepository
import java.nio.file.Files
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryRepositoryTest {

    private lateinit var repository: HistoryRepository

    @Before
    fun setUp() {
        // Unconfined dispatcher: DataStore IO must run without manual
        // scheduler advancing, otherwise edits would hang the test.
        val dataStore = PreferenceDataStoreFactory.create(
            scope = TestScope(UnconfinedTestDispatcher()),
            produceFile = {
                Files.createTempDirectory("history_test")
                    .resolve("settings_${System.nanoTime()}.preferences_pb")
                    .toFile()
            }
        )
        repository = HistoryRepositoryImpl(dataStore)
    }

    private fun item(
        text: String,
        timestamp: Long,
        favorite: Boolean = false
    ) = TranslationHistoryItem(
        sourceText = text,
        translatedText = "translated-$text",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.CHINESE,
        mode = TranslationMode.STANDARD,
        timestamp = timestamp,
        isFavorite = favorite
    )

    @Test
    fun `history starts empty`() = runTest {
        assertTrue(repository.getAllHistory().first().isEmpty())
    }

    @Test
    fun `added items come back newest first`() = runTest {
        repository.addHistory(item("old", timestamp = 1_000))
        repository.addHistory(item("new", timestamp = 2_000))

        val history = repository.getAllHistory().first()
        assertEquals(listOf("new", "old"), history.map { it.sourceText })
    }

    @Test
    fun `delete removes the matching record`() = runTest {
        val a = item("a", 1_000)
        val b = item("b", 2_000)
        repository.addHistory(a)
        repository.addHistory(b)

        repository.deleteHistory(a.id)

        val history = repository.getAllHistory().first()
        assertEquals(listOf(b.id), history.map { it.id })
    }

    @Test
    fun `clearAll wipes every record`() = runTest {
        repository.addHistory(item("a", 1_000))
        repository.addHistory(item("b", 2_000))

        repository.clearAllHistory()

        assertTrue(repository.getAllHistory().first().isEmpty())
    }

    @Test
    fun `history is capped at fifty entries`() = runTest {
        repeat(55) { index ->
            repository.addHistory(item("text-$index", timestamp = index.toLong()))
        }

        val history = repository.getAllHistory().first()
        assertEquals(50, history.size)
        // The oldest five were dropped; the newest one is still there.
        assertEquals("text-54", history.first().sourceText)
        assertFalse(history.any { it.sourceText == "text-0" })
    }

    @Test
    fun `toggleFavorite flips the flag on the matching record only`() = runTest {
        val a = item("a", 1_000, favorite = false)
        val b = item("b", 2_000, favorite = false)
        repository.addHistory(a)
        repository.addHistory(b)

        repository.toggleFavorite(a.id)

        val history = repository.getAllHistory().first()
        assertTrue(history.single { it.id == a.id }.isFavorite)
        assertFalse(history.single { it.id == b.id }.isFavorite)
    }
}
