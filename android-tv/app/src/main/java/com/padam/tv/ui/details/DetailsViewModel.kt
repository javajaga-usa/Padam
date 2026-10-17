package com.padam.tv.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.padam.tv.data.CountryProviders
import com.padam.tv.data.FreeVideo
import com.padam.tv.data.Library
import com.padam.tv.data.MovieDetails
import com.padam.tv.data.MovieRepository
import com.padam.tv.data.YouTubeFinder
import com.padam.tv.data.toMovie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface DetailsState {
    data object Loading : DetailsState
    data class Ready(
        val movie: MovieDetails,
        val providers: CountryProviders?,
        val inList: Boolean,
        val freeVideo: FreeVideo? = null,
    ) : DetailsState
    data class Error(val message: String) : DetailsState
}

class DetailsViewModel(
    private val repository: MovieRepository,
    private val library: Library,
    private val youTube: YouTubeFinder?,
    private val movieId: Int,
) : ViewModel() {
    private val _state = MutableStateFlow<DetailsState>(DetailsState.Loading)
    val state: StateFlow<DetailsState> = _state

    init { load() }

    fun load() {
        _state.value = DetailsState.Loading
        viewModelScope.launch {
            val details = runCatching { repository.details(movieId) }.getOrElse {
                _state.value = DetailsState.Error("Check the internet connection and try again.")
                return@launch
            }
            library.markViewed(details.toMovie())
            _state.value = DetailsState.Ready(details, repository.providersFor(details), library.isInList(movieId))
            youTube?.find(movieId, details.title, details.year)?.let { video ->
                (_state.value as? DetailsState.Ready)?.let { _state.value = it.copy(freeVideo = video) }
            }
        }
    }

    fun toggleMyList() {
        val ready = _state.value as? DetailsState.Ready ?: return
        _state.value = ready.copy(inList = library.toggle(ready.movie.toMovie()))
    }
}
