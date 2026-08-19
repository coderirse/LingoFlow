package com.lingoflow.app.ui.dictionary

import com.lingoflow.app.data.repository.FakeFavoritesRepository
import com.lingoflow.app.data.repository.FakeSettingsRepository
import com.lingoflow.app.data.tts.FakeTtsEngine
import com.lingoflow.app.domain.exception.DictionaryException
import com.lingoflow.app.domain.model.dictionary.DictionaryEntry
import com.lingoflow.app.domain.repository.DictionaryRepository
import com.lingoflow.app.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DictionaryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleEntry = DictionaryEntry(
        word = "hello",
        phonetics = emptyList(),
        entries = emptyList(),
        phrases = emptyList(),
        etymology = null
    )

    private class FakeDictionaryRepository(
        var result: Result<List<DictionaryEntry>>,
        var perWord: ((String) -> Result<List<DictionaryEntry>>)? = null
    ) : DictionaryRepository {
        var lookUpCount = 0
            private set

        override suspend fun lookup(word: String): Result<List<DictionaryEntry>> {
            lookUpCount++
            return perWord?.invoke(word) ?: result
        }

        override suspend fun search(word: String): Result<List<DictionaryEntry>> = lookup(word)
    }

    private fun createViewModel(
        repository: FakeDictionaryRepository,
        favorites: FakeFavoritesRepository = FakeFavoritesRepository(),
        tts: FakeTtsEngine = FakeTtsEngine(),
        lookup: com.lingoflow.app.domain.usecase.LookupWordUseCase =
            com.lingoflow.app.domain.usecase.LookupWordUseCase(FakeSettingsRepository()) {
                com.lingoflow.app.data.llm.FakeLlmProvider()
            }
    ) = DictionaryViewModel(repository, favorites, tts, lookup)

    @Test
    fun `successful lookup exposes Success state`() = runTest {
        val repository = FakeDictionaryRepository(Result.success(listOf(sampleEntry)))
        val viewModel = createViewModel(repository)

        viewModel.lookUp("hello")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DictionaryUiState.Success)
        assertEquals(listOf(sampleEntry), (state as DictionaryUiState.Success).entries)
    }

    @Test
    fun `blank word does not trigger a request`() = runTest {
        val repository = FakeDictionaryRepository(Result.success(listOf(sampleEntry)))
        val viewModel = createViewModel(repository)

        viewModel.lookUp("")
        advanceUntilIdle()

        assertEquals(0, repository.lookUpCount)
        assertTrue(viewModel.uiState.value is DictionaryUiState.Idle)
    }

    @Test
    fun `NotFound surfaces Error state with suggestions`() = runTest {
        val repository = FakeDictionaryRepository(
            Result.failure(DictionaryException.NotFound(listOf("hello", "hell")))
        )
        val viewModel = createViewModel(repository)

        viewModel.lookUp("helllo")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DictionaryUiState.Error)
        val error = (state as DictionaryUiState.Error).error
        assertTrue(error is DictionaryException.NotFound)
        assertEquals(
            listOf("hello", "hell"),
            (error as DictionaryException.NotFound).suggestions
        )
    }

    @Test
    fun `repository failure passes through as Error state`() = runTest {
        val repository = FakeDictionaryRepository(
            Result.failure(DictionaryException.Network())
        )
        val viewModel = createViewModel(repository)

        viewModel.lookUp("hello")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DictionaryUiState.Error)
        assertTrue((state as DictionaryUiState.Error).error is DictionaryException.Network)
    }

    @Test
    fun `toggleFavorite adds then removes the word`() = runTest {
        val favorites = FakeFavoritesRepository()
        val repository = FakeDictionaryRepository(Result.success(listOf(sampleEntry)))
        val viewModel = createViewModel(repository, favorites)

        viewModel.toggleFavorite("Hello")
        advanceUntilIdle()
        assertTrue(favorites.isFavorite("hello").first())

        viewModel.toggleFavorite("hello")
        advanceUntilIdle()
        assertFalse(favorites.isFavorite("hello").first())
    }

    @Test
    fun `speak delegates to the tts engine`() = runTest {
        val tts = FakeTtsEngine()
        val repository = FakeDictionaryRepository(Result.success(listOf(sampleEntry)))
        val viewModel = createViewModel(repository, tts = tts)

        viewModel.speak("hello")

        assertEquals(listOf("hello"), tts.spoken)
    }

    @Test
    fun `inflected word falls back to its base form`() = runTest {
        val repository = FakeDictionaryRepository(
            result = Result.failure(DictionaryException.NotFound()),
            perWord = { word ->
                if (word == "serve") {
                    Result.success(listOf(sampleEntry.copy(word = "serve")))
                } else {
                    Result.failure(DictionaryException.NotFound(listOf("serve")))
                }
            }
        )
        val viewModel = createViewModel(repository)

        viewModel.lookUp("served")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DictionaryUiState.Success)
        assertEquals("serve", (state as DictionaryUiState.Success).entries.single().word)
    }

    @Test
    fun `unrecoverable not found keeps the error state`() = runTest {
        val repository = FakeDictionaryRepository(
            result = Result.failure(DictionaryException.NotFound())
        )
        val viewModel = createViewModel(repository)

        viewModel.lookUp("zzzq")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is DictionaryUiState.Error)
    }
    @Test
    fun `lookup info loads chinese glosses alongside the entry`() = runTest {
        val provider = com.lingoflow.app.data.llm.FakeLlmProvider(
            streamFlow = kotlinx.coroutines.flow.flowOf(
                "WORD: hello\nPOS: int. | 你好；喂\n",
                "EX: Hello, world.\nEX_CN: 你好，世界。\n"
            )
        )
        val settings = com.lingoflow.app.data.repository.FakeSettingsRepository(
            com.lingoflow.app.domain.model.settings.AppSettings(
                activeLlmProviderId = com.lingoflow.app.domain.model.llm.LlmProviderId.DEEPSEEK,
                llmProviders = mapOf(
                    com.lingoflow.app.domain.model.llm.LlmProviderId.DEEPSEEK to
                        com.lingoflow.app.domain.model.settings.ProviderConfig(
                            providerId = com.lingoflow.app.domain.model.llm.LlmProviderId.DEEPSEEK,
                            apiKey = "sk-test",
                            baseUrl = null,
                            model = "deepseek-chat"
                        )
                ),
                dictionaryApiKey = ""
            )
        )
        val lookup = com.lingoflow.app.domain.usecase.LookupWordUseCase(settings) { provider }
        val repository = FakeDictionaryRepository(Result.success(listOf(sampleEntry)))
        val viewModel = createViewModel(repository, lookup = lookup)

        viewModel.lookUp("hello")
        advanceUntilIdle()

        val info = viewModel.lookupInfo.value
        assertTrue(info != null)
        assertEquals(listOf("你好", "喂"), info!!.entries[0].meanings)
        assertFalse(viewModel.lookupInfoUnavailable.value)
    }

    @Test
    fun `lookup info failure flips the unavailable flag without breaking lookup`() = runTest {
        val repository = FakeDictionaryRepository(Result.success(listOf(sampleEntry)))
        val viewModel = createViewModel(repository)

        viewModel.lookUp("hello")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is DictionaryUiState.Success)
        assertTrue(viewModel.lookupInfoUnavailable.value)
    }

    @Test
    fun `unusable stream falls back to the one-shot json lookup`() = runTest {
        // Stream returns garbage, but the one-shot JSON path answers fine:
        // the card must still show Chinese glosses.
        val provider = com.lingoflow.app.data.llm.FakeLlmProvider(
            chatResult = com.lingoflow.app.domain.model.llm.ChatResponse(
                content = """{"word": "engage", "entries": [{"partOfSpeech": "vt.", "meanings": ["参与", "吸引"]}], "example": "Engage the reader.", "exampleTranslation": "吸引读者。"}""",
                finishReason = "stop",
                usage = null
            ),
            streamFlow = kotlinx.coroutines.flow.flowOf("complete nonsense\n")
        )
        val settings = com.lingoflow.app.data.repository.FakeSettingsRepository(
            com.lingoflow.app.domain.model.settings.AppSettings(
                activeLlmProviderId = com.lingoflow.app.domain.model.llm.LlmProviderId.DEEPSEEK,
                llmProviders = mapOf(
                    com.lingoflow.app.domain.model.llm.LlmProviderId.DEEPSEEK to
                        com.lingoflow.app.domain.model.settings.ProviderConfig(
                            providerId = com.lingoflow.app.domain.model.llm.LlmProviderId.DEEPSEEK,
                            apiKey = "sk-test",
                            baseUrl = null,
                            model = "deepseek-chat"
                        )
                ),
                dictionaryApiKey = ""
            )
        )
        val lookup = com.lingoflow.app.domain.usecase.LookupWordUseCase(settings) { provider }
        val repository = FakeDictionaryRepository(Result.success(listOf(sampleEntry)))
        val viewModel = createViewModel(repository, lookup = lookup)

        viewModel.lookUp("engage")
        advanceUntilIdle()

        val info = viewModel.lookupInfo.value
        assertTrue(info != null)
        assertEquals(listOf("参与", "吸引"), info!!.entries[0].meanings)
        assertFalse(viewModel.lookupInfoUnavailable.value)
    }
}
