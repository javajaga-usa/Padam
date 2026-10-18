package com.padam.tv.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.padam.tv.Graph
import com.padam.tv.data.Movie
import com.padam.tv.ui.components.Backdrop
import com.padam.tv.ui.components.CenteredMessage
import com.padam.tv.ui.components.MetaRow
import com.padam.tv.ui.components.MovieCard
import com.padam.tv.ui.components.PosterWidth
import com.padam.tv.ui.components.SectionTitle
import com.padam.tv.ui.components.Skeleton
import com.padam.tv.ui.components.Tab
import com.padam.tv.ui.components.TopBar
import com.padam.tv.ui.theme.Eyebrow
import kotlinx.coroutines.delay

private val Gutter = 56.dp

@Composable
fun HomeScreen(
    onMovieClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onChangeKey: () -> Unit,
    viewModel: HomeViewModel = viewModel { HomeViewModel(Graph.repository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recent by Graph.library.recent.collectAsStateWithLifecycle()
    val myList by Graph.library.myList.collectAsStateWithLifecycle()
    when (val s = state) {
        HomeState.Loading -> HomeSkeleton(onSearchClick)
        is HomeState.Error -> CenteredMessage("Couldn't load films", s.message, actionLabel = "Change TMDB key", onAction = onChangeKey)
        is HomeState.Ready -> {
            val personal = listOfNotNull(
                recent.takeIf { it.isNotEmpty() }?.let { HomeRow("Recently viewed", it) },
                myList.takeIf { it.isNotEmpty() }?.let { HomeRow("My List", it) },
            )
            val featured = s.rows.first().movies.filter { it.backdropPath != null }.take(6)
            HomeContent(personal + s.rows, featured, onMovieClick, onSearchClick)
        }
    }
}

@Composable
private fun HomeContent(
    rows: List<HomeRow>,
    featured: List<Movie>,
    onMovieClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(rows.first().movies.first() to rows.first().title) }
    // Follow focus after a short pause so holding a D-pad key doesn't reload the hero on every card.
    var hovered by remember { mutableStateOf(focused) }
    LaunchedEffect(hovered) {
        delay(140)
        focused = hovered
    }
    var inSpotlight by remember { mutableStateOf(featured.isNotEmpty()) }
    val firstCard = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstCard.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        Backdrop(if (inSpotlight) null else focused.first.backdropPath)
        Column(Modifier.fillMaxSize()) {
            TopBar(Tab.Home, onHome = {}, onSearch = onSearchClick, modifier = Modifier.padding(start = Gutter, end = Gutter, top = 20.dp))
            // The banner already shows its film, so the hero steps aside while the banner has focus.
            AnimatedVisibility(!inSpotlight, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                AnimatedContent(
                    targetState = focused,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "hero",
                ) { (movie, rowTitle) -> Hero(movie, rowTitle) }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(top = 12.dp, bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                if (featured.isNotEmpty()) {
                    item(key = "spotlight") {
                        Spotlight(
                            films = featured,
                            onClick = { onMovieClick(it.id) },
                            onFocusChange = { inSpotlight = it },
                            modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp).focusRequester(firstCard),
                        )
                    }
                }
                itemsIndexed(rows, key = { _, row -> row.title }) { rowIndex, row ->
                    val ranked = row.title == "Popular"
                    val wide = row.title == "Recently viewed"
                    val title = if (ranked) "Top 10 Tamil films" else row.title
                    val movies = if (ranked) row.movies.take(10) else row.movies
                    Column {
                        SectionTitle(title, if (ranked) "by popularity" else "${movies.size} films", Modifier.padding(start = Gutter, bottom = 12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Gutter, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(if (ranked) 6.dp else 16.dp),
                        ) {
                            itemsIndexed(movies, key = { _, m -> m.id }) { index, movie ->
                                val open = { onMovieClick(movie.id) }
                                val hover = { hovered = movie to title }
                                when {
                                    ranked -> RankedCard(index + 1, movie, open, hover)
                                    wide -> WideCard(movie, open, hover)
                                    else -> MovieCard(
                                        movie = movie,
                                        onClick = open,
                                        onFocused = hover,
                                        modifier = if (featured.isEmpty() && rowIndex == 0 && index == 0) Modifier.focusRequester(firstCard) else Modifier,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(movie: Movie, rowTitle: String) {
    Column(Modifier.padding(start = Gutter, top = 20.dp).width(620.dp).height(196.dp)) {
        Text(rowTitle.uppercase(), style = Eyebrow)
        Spacer(Modifier.height(6.dp))
        Text(movie.title, style = MaterialTheme.typography.displaySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        MetaRow(movie.voteAverage, listOfNotNull(movie.year.ifBlank { null }), movie.genreNames)
        Spacer(Modifier.height(10.dp))
        Text(
            movie.overview.ifBlank { "No synopsis yet." },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeSkeleton(onSearchClick: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar(Tab.Home, onHome = {}, onSearch = onSearchClick, modifier = Modifier.padding(start = Gutter, end = Gutter, top = 20.dp))
        Column(Modifier.padding(start = Gutter, top = 28.dp).height(196.dp)) {
            Skeleton(Modifier.size(110.dp, 14.dp))
            Spacer(Modifier.height(12.dp))
            Skeleton(Modifier.size(380.dp, 40.dp))
            Spacer(Modifier.height(14.dp))
            Skeleton(Modifier.size(260.dp, 16.dp))
            Spacer(Modifier.height(10.dp))
            Skeleton(Modifier.size(560.dp, 16.dp))
        }
        repeat(2) {
            Column(Modifier.padding(start = Gutter, bottom = 26.dp)) {
                Skeleton(Modifier.size(150.dp, 18.dp))
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(8) { Skeleton(Modifier.width(PosterWidth).aspectRatio(2f / 3f)) }
                }
            }
        }
    }
}
