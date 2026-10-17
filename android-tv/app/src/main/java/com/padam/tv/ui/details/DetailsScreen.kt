package com.padam.tv.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.padam.tv.Graph
import com.padam.tv.data.CountryProviders
import com.padam.tv.data.MovieDetails
import com.padam.tv.data.Provider
import com.padam.tv.data.TmdbImages
import com.padam.tv.ui.components.Backdrop
import com.padam.tv.ui.components.CenteredMessage
import com.padam.tv.ui.components.MovieCard
import com.padam.tv.util.Launchers

@Composable
fun DetailsScreen(
    movieId: Int,
    onMovieClick: (Int) -> Unit,
    viewModel: DetailsViewModel = viewModel(key = "details-$movieId") { DetailsViewModel(Graph.repository, movieId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        DetailsState.Loading -> CenteredMessage("Loading…")
        is DetailsState.Error -> CenteredMessage(s.message, actionLabel = "Retry", onAction = viewModel::load)
        is DetailsState.Ready -> DetailsContent(s.movie, s.providers, onMovieClick)
    }
}

@Composable
private fun DetailsContent(
    movie: MovieDetails,
    providers: CountryProviders?,
    onMovieClick: (Int) -> Unit,
) {
    val context = LocalContext.current
    val primaryAction = remember { FocusRequester() }
    val streaming = providers?.let { (it.flatrate + it.free + it.ads).distinctBy { p -> p.id } }.orEmpty()
    val rentOrBuy = providers?.let { (it.rent + it.buy).distinctBy { p -> p.id } }.orEmpty()
        .filter { p -> streaming.none { it.id == p.id } }
    val videos = movie.youTubeVideos

    Box(Modifier.fillMaxSize()) {
        Backdrop(movie.backdropPath)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    AsyncImage(
                        model = TmdbImages.poster(movie.posterPath),
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(200.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)),
                    )
                    Column(Modifier.width(620.dp)) {
                        Text(movie.title, style = MaterialTheme.typography.displaySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (movie.originalTitle.isNotBlank() && movie.originalTitle != movie.title) {
                            Text(movie.originalTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            listOfNotNull(
                                movie.year.ifBlank { null },
                                movie.runtime?.takeIf { it > 0 }?.let { "${it / 60}h ${it % 60}m" },
                                if (movie.voteAverage > 0) "★ %.1f".format(movie.voteAverage) else null,
                                movie.genres.take(3).joinToString(", ") { it.name }.ifBlank { null },
                            ).joinToString("  ·  "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        movie.tagline?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(movie.overview, style = MaterialTheme.typography.bodyMedium, maxLines = 6, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            listOfNotNull(
                                movie.director?.let { "Director: $it" },
                                movie.music?.let { "Music: $it" },
                            ).joinToString("     "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val cast = movie.credits?.cast.orEmpty().take(6).joinToString(", ") { it.name }
                        if (cast.isNotBlank()) {
                            Text("Cast: $cast", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            videos.firstOrNull()?.let { trailer ->
                                Button(
                                    onClick = { Launchers.playYouTube(context, trailer.key) },
                                    modifier = Modifier.focusRequester(primaryAction),
                                ) { Text("Play trailer") }
                            }
                            streaming.take(3).forEachIndexed { i, p ->
                                ProviderButton(
                                    provider = p,
                                    label = "Watch on ${p.name}",
                                    onClick = { Launchers.openProvider(context, p.name, providers?.link) },
                                    modifier = if (videos.isEmpty() && i == 0) Modifier.focusRequester(primaryAction) else Modifier,
                                )
                            }
                        }
                        if (streaming.isEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                if (rentOrBuy.isEmpty()) "Not streaming in your region yet." else "Rent or buy below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (rentOrBuy.isNotEmpty()) {
                item {
                    Section("Rent or buy") {
                        items(rentOrBuy, key = { it.id }) { p ->
                            ProviderButton(p, p.name, onClick = { Launchers.openProvider(context, p.name, providers?.link) })
                        }
                    }
                }
            }
            if (videos.size > 1) {
                item {
                    Section("Trailers, songs and clips") {
                        items(videos.drop(1), key = { it.key }) { v ->
                            OutlinedButton(onClick = { Launchers.playYouTube(context, v.key) }) {
                                Text(v.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(260.dp))
                            }
                        }
                    }
                }
            }
            val more = movie.recommendations?.results.orEmpty()
                .filter { it.originalLanguage == "ta" && it.posterPath != null }
            if (more.isNotEmpty()) {
                item {
                    Section("More like this") {
                        items(more, key = { it.id }) { m ->
                            MovieCard(m, onClick = { onMovieClick(m.id) }, width = 120.dp)
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(movie.id) { runCatching { primaryAction.requestFocus() } }
}

@Composable
private fun Section(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun ProviderButton(
    provider: Provider,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AsyncImage(
                model = TmdbImages.logo(provider.logoPath),
                contentDescription = null,
                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)),
            )
            Text(label)
        }
    }
}
