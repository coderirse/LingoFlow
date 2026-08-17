package com.lingoflow.app.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.lingoflow.app.domain.repository.FavoritesRepository
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
class FavoritesRepositoryTest {

    private lateinit var repository: FavoritesRepository

    @Before
    fun setUp() {
        // Unconfined dispatcher: DataStore IO must run without manual
        // scheduler advancing, otherwise edits would hang the test.
        val scope = TestScope(UnconfinedTestDispatcher())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = {
                Files.createTempDirectory("favorites_test")
                    .resolve("settings_${System.nanoTime()}.preferences_pb")
                    .toFile()
            }
        )
        repository = FavoritesRepositoryImpl(dataStore)
    }

    @Test
    fun `favorites start empty`() = runTest {
        assertTrue(repository.getFavorites().first().isEmpty())
    }

    @Test
    fun `addFavorite persists and normalizes the word`() = runTest {
        repository.addFavorite("  Hello ")

        assertEquals(setOf("hello"), repository.getFavorites().first())
        assertTrue(repository.isFavorite("HELLO").first())
    }

    @Test
    fun `removeFavorite deletes the word`() = runTest {
        repository.addFavorite("hello")
        repository.addFavorite("world")
        repository.removeFavorite("hello")

        assertEquals(setOf("world"), repository.getFavorites().first())
        assertFalse(repository.isFavorite("hello").first())
    }

    @Test
    fun `adding the same word twice keeps one entry`() = runTest {
        repository.addFavorite("hello")
        repository.addFavorite("hello")

        assertEquals(setOf("hello"), repository.getFavorites().first())
    }

    @Test
    fun `blank words are ignored`() = runTest {
        repository.addFavorite("   ")

        assertTrue(repository.getFavorites().first().isEmpty())
    }
}
