package com.padam.tv.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.padam.tv.data.Movie
import com.padam.tv.data.TmdbImages
import com.padam.tv.ui.components.CardShape
import com.padam.tv.ui.components.MetaRow
import com.padam.tv.ui.components.MovieCard
import com.padam.tv.ui.components.PosterFallback
import com.padam.tv.ui.components.PosterWidth
import com.padam.tv.ui.theme.Accent
import com.padam.tv.ui.theme.Background
import com.padam.tv.ui.theme.Eyebrow
import com.padam.tv.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private val BannerShape = RoundedCornerShape(16.dp)

/**
 * A wide banner that rotates through a few featured films on its own. Left and right step through
 * them while it has focus; up and down leave it as usual.
 */
@Composable
fun Spotlight(
    films: List<Movie>,
    onClick: (Movie) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(index) {
        delay(7_000)
        index = (index + 1) % films.size
    }
    val movie = films[index]

    Card(
        onClick = { onClick(movie) },
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .onFocusChanged { onFocusChange(it.isFocused) }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionRight -> { index = (index + 1) % films.size; true }
                    Key.DirectionLeft -> { index = (index - 1 + films.size) % films.size; true }
                    else -> false
                }
            },
        shape = CardDefaults.shape(BannerShape),
        scale = CardDefaults.scale(focusedScale = 1.01f),
        border = CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Accent), shape = BannerShape)),
    ) {
        Box(Modifier.fillMaxSize()) {
            Crossfade(movie, animationSpec = tween(700), label = "spotlight") { m ->
                Box(Modifier.fillMaxSize()) {
                    PosterFallback(m.id, "", "")
                    AsyncImage(
                        model = TmdbImages.backdrop(m.backdropPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(0f to Background.copy(alpha = 0.92f), 0.45f to Background.copy(alpha = 0.55f), 0.8f to Color.Transparent),
                ),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(28.dp).width(560.dp)) {
                Text("FEATURED", style = Eyebrow)
                Spacer(Modifier.height(6.dp))
                Text(movie.title, style = MaterialTheme.typography.displaySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                MetaRow(movie.voteAverage, listOfNotNull(movie.year.ifBlank { null }), movie.genreNames)
                Spacer(Modifier.height(10.dp))
                Text(
                    movie.overview.ifBlank { "No synopsis yet." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                Modifier.align(Alignment.BottomEnd).padding(28.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                films.indices.forEach { i ->
                    val w by animateDpAsState(if (i == index) 22.dp else 8.dp, label = "dot")
                    Box(
                        Modifier.width(w).height(8.dp)
                            .background(if (i == index) Accent else Color.White.copy(alpha = 0.35f), CircleShape),
                    )
                }
            }
        }
    }
}

/** A poster with its rank drawn large beside it, for the Top 10 row. */
@Composable
fun RankedCard(rank: Int, movie: Movie, onClick: () -> Unit, onFocused: () -> Unit) {
    Box(Modifier.width(PosterWidth + 58.dp), contentAlignment = Alignment.BottomEnd) {
        Text(
            rank.toString(),
            fontSize = 130.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.16f),
            lineHeight = 130.sp,
            modifier = Modifier.align(Alignment.BottomStart).offset(x = (-6).dp, y = 22.dp),
        )
        MovieCard(movie = movie, onClick = onClick, onFocused = onFocused)
    }
}

/** A landscape card with the backdrop and title, for rows people come back to. */
@Composable
fun WideCard(movie: Movie, onClick: () -> Unit, onFocused: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(300.dp).aspectRatio(16f / 9f).onFocusChanged { if (it.isFocused) onFocused() },
        shape = CardDefaults.shape(CardShape),
        scale = CardDefaults.scale(focusedScale = 1.06f),
        border = CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Accent), shape = CardShape)),
    ) {
        Box(Modifier.fillMaxSize()) {
            PosterFallback(movie.id, "", "")
            AsyncImage(
                model = TmdbImages.backdrop(movie.backdropPath) ?: TmdbImages.poster(movie.posterPath),
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))
            Text(
                movie.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            )
        }
    }
}
