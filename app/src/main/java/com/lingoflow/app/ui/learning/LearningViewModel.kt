package com.lingoflow.app.ui.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingoflow.app.domain.model.history.TranslationHistoryItem
import com.lingoflow.app.domain.repository.FavoritesRepository
import com.lingoflow.app.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Learning tab data: favorite dictionary words plus favorite translation
 * records. Both hearts across the app write into these two repositories, so
 * everything the user favorites shows up here.
 */
@HiltViewModel
class LearningViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val favoriteWords: StateFlow<List<String>> = favoritesRepository.getFavorites()
        .map { it.sorted() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Favorited translation records, newest first. */
    val favoriteTranslations: StateFlow<List<TranslationHistoryItem>> =
        historyRepository.getAllHistory()
            .map { list -> list.filter { it.isFavorite } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun remove(word: String) {
        viewModelScope.launch { favoritesRepository.removeFavorite(word) }
    }

    /** Unfavorites a translation record (toggles its flag off). */
    fun removeTranslation(id: String) {
        viewModelScope.launch { historyRepository.toggleFavorite(id) }
    }
}
