package com.padam.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.padam.tv.data.Movie
import com.padam.tv.data.TmdbImages
import com.padam.tv.ui.theme.Accent

@Composable
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = PosterWidth,
    onFocused: () -> Unit = {},
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .onFocusChanged { if (it.isFocused) onFocused() },
        shape = CardDefaults.shape(CardShape),
        scale = CardDefaults.scale(focusedScale = 1.09f),
        border = CardDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Accent), shape = CardShape),
        ),
        glow = CardDefaults.glow(focusedGlow = Glow(elevationColor = Accent.copy(alpha = 0.35f), elevation = 12.dp)),
    ) {
        Box(Modifier.fillMaxSize()) {
            PosterFallback(movie.id, movie.title, movie.year)
            AsyncImage(
                model = TmdbImages.poster(movie.posterPath),
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** A film-specific colour gradient with the title, shown until (or instead of) the poster. */
@Composable
fun PosterFallback(id: Int, title: String, year: String) {
    val hue = (id * 47 % 360).toFloat()
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(Color.hsl(hue, 0.35f, 0.26f), Color.hsl((hue + 40f) % 360f, 0.30f, 0.12f)),
            ),
        ),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (year.isNotBlank()) Text(year, fontSize = 12.sp, color = Color.White.copy(alpha = 0.65f))
        }
    }
}
