package com.padam.tv.ui.details

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.padam.tv.Graph
import com.padam.tv.data.CountryProviders
import com.padam.tv.data.MovieDetails
import com.padam.tv.data.Provider
import com.padam.tv.data.TmdbImages
import com.padam.tv.ui.components.Backdrop
import com.padam.tv.ui.components.CardShape
import com.padam.tv.ui.components.CenteredMessage
import com.padam.tv.ui.components.Initials
import com.padam.tv.ui.components.MetaRow
import com.padam.tv.ui.components.MovieCard
import com.padam.tv.ui.components.PosterFallback
import com.padam.tv.ui.components.PrimaryButton
import com.padam.tv.ui.components.SectionTitle
import com.padam.tv.ui.components.Skeleton
import com.padam.tv.ui.components.Tab
import com.padam.tv.ui.components.TopBar
import com.padam.tv.ui.components.VideoCard
import com.padam.tv.ui.theme.Accent
import com.padam.tv.ui.theme.Surface2
import com.padam.tv.ui.theme.TextFaint
import com.padam.tv.ui.theme.TextPrimary
import com.padam.tv.util.Launchers

private val Gutter = 56.dp

@Composable
fun DetailsScreen(
    movieId: Int,
    onMovieClick: (Int) -> Unit,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    viewModel: DetailsViewModel = viewModel(key = "details-$movieId") { DetailsViewModel(Graph.repository, movieId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        DetailsState.Loading -> DetailsSkeleton(onHome, onSearch)
        is DetailsState.Error -> CenteredMessage("Couldn't load this film", s.message, actionLabel = "Try again", onAction = viewModel::load)
        is DetailsState.Ready -> DetailsContent(s.movie, s.providers, onMovieClick, onHome, onSearch)
    }
}

@Composable
private fun DetailsContent(
    movie: MovieDetails,
    providers: CountryProviders?,
    onMovieClick: (Int) -> Unit,
    onHome: () -> Unit,
    onSearch: () -> Unit,
) {
    val context = LocalContext.current
    val primaryAction = remember { FocusRequester() }
    val streaming = providers?.let { (it.flatrate + it.free + it.ads).distinctBy { p -> p.id } }.orEmpty()
    val rentOrBuy = providers?.let { (it.rent + it.buy).distinctBy { p -> p.id } }.orEmpty()
        .filter { p -> streaming.none { it.id == p.id } }
    val videos = movie.youTubeVideos
    val cast = movie.credits?.cast.orEmpty().take(12)
    val more = movie.recommendations?.results.orEmpty().filter { it.originalLanguage == "ta" && it.posterPath != null }

    Box(Modifier.fillMaxSize()) {
        Backdrop(movie.backdropPath)
        LazyColumn(
            contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 20.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            item { TopBar(Tab.None, onHome = onHome, onSearch = onSearch) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                    Box(
                        Modifier.width(220.dp).aspectRatio(2f / 3f).shadow(24.dp, CardShape).clip(CardShape),
                    ) {
                        PosterFallback(movie.id, movie.title, movie.year)
                        AsyncImage(
                            model = TmdbImages.poster(movie.posterPath),
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Column(Modifier.width(640.dp)) {
                        Text(movie.title, style = MaterialTheme.typography.displaySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (movie.originalTitle.isNotBlank() && movie.originalTitle != movie.title) {
                            Text(movie.originalTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(10.dp))
                        MetaRow(
                            movie.voteAverage,
                            listOfNotNull(
                                movie.year.ifBlank { null },
                                movie.runtime?.takeIf { it > 0 }?.let { "${it / 60}h ${it % 60}m" },
                            ),
                            movie.genres.take(3).map { it.name },
                        )
                        movie.tagline?.takeIf { it.isNotBlank() }?.let {
                            Text("“$it”", color = Accent, fontStyle = FontStyle.Italic, fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp))
                        }
                        Text(
                            movie.overview.ifBlank { "No synopsis yet." },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(40.dp), modifier = Modifier.padding(top = 14.dp)) {
                            movie.director?.let { Credit("Director", it) }
                            movie.music?.let { Credit("Music", it) }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 22.dp),
                        ) {
                            videos.firstOrNull()?.let { trailer ->
                                PrimaryButton("▶  Play trailer", { Launchers.playYouTube(context, trailer.key) }, Modifier.focusRequester(primaryAction))
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
                            Text(
                                if (rentOrBuy.isEmpty()) "Not streaming in your region yet." else "Not on a subscription app here. Rent or buy below.",
                                color = TextFaint,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }
            }
            if (rentOrBuy.isNotEmpty()) {
                section("Rent or buy") {
                    items(rentOrBuy, key = { it.id }) { p ->
                        ProviderButton(p, p.name, onClick = { Launchers.openProvider(context, p.name, providers?.link) })
                    }
                }
            }
            if (videos.size > 1) {
                section("Trailers, songs and clips") {
                    itemsIndexed(videos.drop(1), key = { i, v -> "$i-${v.key}" }) { _, v ->
                        VideoCard(v, onClick = { Launchers.playYouTube(context, v.key) })
                    }
                }
            }
            if (cast.isNotEmpty()) {
                item {
                    Column {
                        SectionTitle("Cast", modifier = Modifier.padding(bottom = 14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            cast.take(9).forEach { c -> Person(c.name, c.character.orEmpty(), c.profilePath) }
                        }
                    }
                }
            }
            if (more.isNotEmpty()) {
                section("More like this") {
                    items(more, key = { it.id }) { m -> MovieCard(m, onClick = { onMovieClick(m.id) }, width = 124.dp) }
                }
            }
        }
    }
    LaunchedEffect(movie.id) { runCatching { primaryAction.requestFocus() } }
}

private fun LazyListScope.section(title: String, content: LazyListScope.() -> Unit) {
    item(key = title) {
        Column {
            SectionTitle(title, modifier = Modifier.padding(bottom = 14.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun Credit(label: String, name: String) {
    Column {
        Text(label.uppercase(), color = TextFaint, fontSize = 11.sp, letterSpacing = 1.4.sp)
        Text(name, fontSize = 15.sp)
    }
}

@Composable
private fun Person(name: String, character: String, photo: String?) {
    Column(Modifier.width(100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(76.dp).clip(CircleShape)) {
            Initials(name, 76.dp)
            if (photo != null) {
                AsyncImage(
                    model = TmdbImages.profile(photo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(character, fontSize = 12.sp, color = TextFaint, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ProviderButton(
    provider: Provider,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = OutlinedButtonDefaults.colors(
            containerColor = Surface2,
            contentColor = TextPrimary,
            focusedContainerColor = Surface2,
            focusedContentColor = TextPrimary,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surface)) {
                AsyncImage(
                    model = TmdbImages.logo(provider.logoPath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(label)
        }
    }
}

@Composable
private fun DetailsSkeleton(onHome: () -> Unit, onSearch: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = Gutter, end = Gutter, top = 20.dp)) {
        TopBar(Tab.None, onHome = onHome, onSearch = onSearch)
        Spacer(Modifier.height(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            Skeleton(Modifier.width(220.dp).aspectRatio(2f / 3f), CardShape)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Skeleton(Modifier.size(380.dp, 40.dp))
                Skeleton(Modifier.size(280.dp, 18.dp))
                Skeleton(Modifier.size(600.dp, 16.dp))
                Skeleton(Modifier.size(560.dp, 16.dp))
                Skeleton(Modifier.size(180.dp, 44.dp), CircleShape)
            }
        }
    }
}
