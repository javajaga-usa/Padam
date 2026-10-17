package com.padam.tv.ui.person

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.padam.tv.Graph
import com.padam.tv.data.MovieRepository
import com.padam.tv.data.PersonDetails
import com.padam.tv.data.TmdbImages
import com.padam.tv.ui.components.CenteredMessage
import com.padam.tv.ui.components.Initials
import com.padam.tv.ui.components.MovieCard
import com.padam.tv.ui.components.PosterWidth
import com.padam.tv.ui.components.SectionTitle
import com.padam.tv.ui.components.Skeleton
import com.padam.tv.ui.components.Tab
import com.padam.tv.ui.components.TopBar
import com.padam.tv.ui.theme.Eyebrow
import com.padam.tv.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface PersonState {
    data object Loading : PersonState
    data class Ready(val person: PersonDetails) : PersonState
    data object Error : PersonState
}

class PersonViewModel(private val repository: MovieRepository, private val personId: Int) : ViewModel() {
    private val _state = MutableStateFlow<PersonState>(PersonState.Loading)
    val state: StateFlow<PersonState> = _state

    init { load() }

    fun load() {
        _state.value = PersonState.Loading
        viewModelScope.launch {
            _state.value = runCatching { repository.person(personId) }
                .fold(onSuccess = { PersonState.Ready(it) }, onFailure = { PersonState.Error })
        }
    }
}

@Composable
fun PersonScreen(
    personId: Int,
    onMovieClick: (Int) -> Unit,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    viewModel: PersonViewModel = viewModel(key = "person-$personId") { PersonViewModel(Graph.repository, personId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        PersonState.Loading -> Column(Modifier.fillMaxSize().padding(start = 56.dp, end = 56.dp, top = 20.dp)) {
            TopBar(Tab.None, onHome = onHome, onSearch = onSearch)
            Row(Modifier.padding(top = 30.dp), horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                Skeleton(Modifier.size(200.dp), CircleShape)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Skeleton(Modifier.size(340.dp, 40.dp))
                    Skeleton(Modifier.size(260.dp, 18.dp))
                    Skeleton(Modifier.size(600.dp, 16.dp))
                }
            }
        }
        PersonState.Error -> CenteredMessage("Couldn't load this person", "Check the internet connection and try again.", "Try again", viewModel::load)
        is PersonState.Ready -> PersonContent(s.person, onMovieClick, onHome, onSearch)
    }
}

@Composable
private fun PersonContent(person: PersonDetails, onMovieClick: (Int) -> Unit, onHome: () -> Unit, onSearch: () -> Unit) {
    val films = person.tamilFilms
    val firstFilm = remember { FocusRequester() }
    LaunchedEffect(person.id) { runCatching { firstFilm.requestFocus() } }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(PosterWidth),
        contentPadding = PaddingValues(start = 56.dp, end = 56.dp, top = 20.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { TopBar(Tab.None, onHome = onHome, onSearch = onSearch) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(36.dp), modifier = Modifier.padding(top = 10.dp)) {
                Box(Modifier.size(200.dp).shadow(24.dp, CircleShape).clip(CircleShape)) {
                    Initials(person.name, 200.dp)
                    person.profilePath?.let {
                        AsyncImage(
                            model = TmdbImages.profile(it),
                            contentDescription = person.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Column(Modifier.width(660.dp)) {
                    Text(if (films.isEmpty()) "PERSON" else "${films.size} TAMIL FILMS", style = Eyebrow)
                    Spacer(Modifier.height(6.dp))
                    Text(person.name, style = MaterialTheme.typography.displaySmall)
                    val facts = listOfNotNull(person.knownFor, person.birthday?.let { "Born $it" }, person.placeOfBirth)
                    if (facts.isNotEmpty()) {
                        Text(facts.joinToString("  ·  "), color = TextSecondary, modifier = Modifier.padding(top = 8.dp))
                    }
                    Text(
                        person.biography.ifBlank { "No biography yet." },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
        if (films.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Tamil films", "newest first") }
            items(films, key = { it.id }) { movie ->
                MovieCard(
                    movie,
                    onClick = { onMovieClick(movie.id) },
                    modifier = if (movie.id == films.first().id) Modifier.focusRequester(firstFilm) else Modifier,
                )
            }
        }
    }
}
