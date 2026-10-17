package com.padam.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padam.tv.data.Movie
import com.padam.tv.data.MovieRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeRow(val title: String, val movies: List<Movie>)

sealed interface HomeState {
    data object Loading : HomeState
    data class Ready(val rows: List<HomeRow>) : HomeState
    data class Error(val message: String) : HomeState
}

class HomeViewModel(private val repository: MovieRepository) : ViewModel() {
    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state: StateFlow<HomeState> = _state

    init { load() }

    fun load() {
        _state.value = HomeState.Loading
        viewModelScope.launch {
            val rows = repository.shelves()
                .map { shelf -> async { runCatching { HomeRow(shelf.title, repository.shelf(shelf)) }.getOrNull() } }
                .awaitAll()
                .filterNotNull()
                .filter { it.movies.isNotEmpty() }
            _state.value = if (rows.isEmpty()) {
                HomeState.Error("Couldn't load movies. Check the internet connection and TMDB key.")
            } else {
                HomeState.Ready(rows)
            }
        }
    }
}
