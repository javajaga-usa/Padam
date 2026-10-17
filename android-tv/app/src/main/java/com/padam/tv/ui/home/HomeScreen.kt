package com.padam.tv.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.padam.tv.Graph
import com.padam.tv.data.Movie
import com.padam.tv.ui.components.Backdrop
import com.padam.tv.ui.components.MovieCard
import com.padam.tv.ui.components.CenteredMessage

@Composable
fun HomeScreen(
    onMovieClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: HomeViewModel = viewModel { HomeViewModel(Graph.repository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        HomeState.Loading -> CenteredMessage("Loading…")
        is HomeState.Error -> CenteredMessage(s.message, actionLabel = "Retry", onAction = viewModel::load)
        is HomeState.Ready -> HomeContent(s.rows, onMovieClick, onSearchClick)
    }
}

@Composable
private fun HomeContent(
    rows: List<HomeRow>,
    onMovieClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(rows.first().movies.first()) }
    val firstCard = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstCard.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        Backdrop(focused.backdropPath)
        Column(Modifier.fillMaxSize()) {
            Header(focused, onSearchClick)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                itemsIndexed(rows, key = { _, row -> row.title }) { rowIndex, row ->
                    Column {
                        Text(
                            row.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 48.dp, bottom = 10.dp),
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            itemsIndexed(row.movies, key = { _, m -> m.id }) { index, movie ->
                                MovieCard(
                                    movie = movie,
                                    onClick = { onMovieClick(movie.id) },
                                    onFocused = { focused = movie },
                                    modifier = if (rowIndex == 0 && index == 0) Modifier.focusRequester(firstCard) else Modifier,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(movie: Movie, onSearchClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp, top = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Padam",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1f))
            Button(onClick = onSearchClick) { Text("Search") }
        }
        Spacer(Modifier.height(24.dp))
        Column(Modifier.width(560.dp).height(150.dp)) {
            Text(movie.title, style = MaterialTheme.typography.displaySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(
                listOfNotNull(
                    movie.year.ifBlank { null },
                    if (movie.voteAverage > 0) "★ %.1f".format(movie.voteAverage) else null,
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                movie.overview,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
