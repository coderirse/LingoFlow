package com.lingoflow.app.ui.learning

import com.lingoflow.app.data.repository.FakeFavoritesRepository
import com.lingoflow.app.data.repository.FakeHistoryRepository
import com.lingoflow.app.domain.model.Language
import com.lingoflow.app.domain.model.history.TranslationHistoryItem
import com.lingoflow.app.domain.model.translation.TranslationMode
import com.lingoflow.app.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LearningViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun historyItem(
        source: String,
        favorite: Boolean
    ) = TranslationHistoryItem(
        sourceText = source,
        translatedText = "译文-$source",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.CHINESE,
        mode = TranslationMode.STANDARD,
        isFavorite = favorite
    )

    @Test
    fun `favorite words are exposed sorted`() = runTest {
        val favorites = FakeFavoritesRepository()
        favorites.addFavorite("zebra")
        favorites.addFavorite("apple")
        val viewModel = LearningViewModel(favorites, FakeHistoryRepository())
        // stateIn(WhileSubscribed) only produces while someone collects.
        backgroundScope.launch { viewModel.favoriteWords.collect { } }
        advanceUntilIdle()

        assertEquals(listOf("apple", "zebra"), viewModel.favoriteWords.value)
    }

    @Test
    fun `only favorited history records show up as favorite translations`() = runTest {
        val history = FakeHistoryRepository()
        history.addHistory(historyItem("hello", favorite = true))
        history.addHistory(historyItem("world", favorite = false))
        val viewModel = LearningViewModel(FakeFavoritesRepository(), history)
        backgroundScope.launch { viewModel.favoriteTranslations.collect { } }
        advanceUntilIdle()

        val translations = viewModel.favoriteTranslations.value
        assertEquals(1, translations.size)
        assertEquals("hello", translations.single().sourceText)
    }

    @Test
    fun `remove deletes a favorite word`() = runTest {
        val favorites = FakeFavoritesRepository()
        favorites.addFavorite("apple")
        val viewModel = LearningViewModel(favorites, FakeHistoryRepository())
        backgroundScope.launch { viewModel.favoriteWords.collect { } }
        advanceUntilIdle()
        assertEquals(listOf("apple"), viewModel.favoriteWords.value)

        viewModel.remove("apple")
        advanceUntilIdle()

        assertTrue(viewModel.favoriteWords.value.isEmpty())
        assertFalse(favorites.isFavorite("apple").first())
    }

    @Test
    fun `removeTranslation unflags the history record`() = runTest {
        val history = FakeHistoryRepository()
        val item = historyItem("hello", favorite = true)
        history.addHistory(item)
        val viewModel = LearningViewModel(FakeFavoritesRepository(), history)
        backgroundScope.launch { viewModel.favoriteTranslations.collect { } }
        advanceUntilIdle()
        assertEquals(1, viewModel.favoriteTranslations.value.size)

        viewModel.removeTranslation(item.id)
        advanceUntilIdle()

        assertTrue(viewModel.favoriteTranslations.value.isEmpty())
        assertFalse(history.favoriteOf(item.id).first() == true)
    }
}
