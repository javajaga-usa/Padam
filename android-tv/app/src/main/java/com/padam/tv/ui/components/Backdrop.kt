package com.padam.tv.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.padam.tv.data.TmdbImages
import com.padam.tv.ui.theme.Background

/** Full-screen backdrop that fades into the background so text on top stays readable. */
@Composable
fun Backdrop(path: String?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        Crossfade(targetState = path, label = "backdrop") { p ->
            AsyncImage(
                model = TmdbImages.backdrop(p),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Background, 0.45f to Background.copy(alpha = 0.85f), 1f to Color.Transparent,
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.3f to Color.Transparent, 1f to Background),
            ),
        )
    }
}
