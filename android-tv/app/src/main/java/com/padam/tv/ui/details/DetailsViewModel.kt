package com.padam.tv.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padam.tv.data.CountryProviders
import com.padam.tv.data.MovieDetails
import com.padam.tv.data.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface DetailsState {
    data object Loading : DetailsState
    data class Ready(val movie: MovieDetails, val providers: CountryProviders?) : DetailsState
    data class Error(val message: String) : DetailsState
}

class DetailsViewModel(
    private val repository: MovieRepository,
    private val movieId: Int,
) : ViewModel() {
    private val _state = MutableStateFlow<DetailsState>(DetailsState.Loading)
    val state: StateFlow<DetailsState> = _state

    init { load() }

    fun load() {
        _state.value = DetailsState.Loading
        viewModelScope.launch {
            _state.value = runCatching { repository.details(movieId) }
                .fold(
                    onSuccess = { DetailsState.Ready(it, repository.providersFor(it)) },
                    onFailure = { DetailsState.Error("Couldn't load this film.") },
                )
        }
    }
}
