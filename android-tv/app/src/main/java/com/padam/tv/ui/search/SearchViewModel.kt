package com.padam.tv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padam.tv.data.Movie
import com.padam.tv.data.MovieRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val results: List<Movie> = emptyList(),
    val suggestions: List<Movie> = emptyList(),
    val searching: Boolean = false,
    val error: Boolean = false,
)

@OptIn(FlowPreview::class)
class SearchViewModel(private val repository: MovieRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state
    private val query = MutableStateFlow("")

    init {
        // Popular films fill the screen until the person types something.
        viewModelScope.launch {
            val popular = repository.shelves().firstOrNull { it.title == "Popular" }
            if (popular != null) {
                val movies = runCatching { repository.shelf(popular) }.getOrDefault(emptyList())
                _state.value = _state.value.copy(suggestions = movies.take(14))
            }
        }
        query.debounce(350).distinctUntilChanged().onEach { q ->
            if (q.isBlank()) {
                _state.value = _state.value.copy(results = emptyList(), searching = false, error = false)
                return@onEach
            }
            _state.value = _state.value.copy(searching = true, error = false)
            val result = runCatching { repository.search(q.trim()) }
            _state.value = _state.value.copy(
                results = result.getOrDefault(emptyList()),
                searching = false,
                error = result.isFailure,
            )
        }.launchIn(viewModelScope)
    }

    fun onQueryChange(value: String) {
        _state.value = _state.value.copy(query = value)
        query.value = value
    }
}
