package com.padam.tv.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.padam.tv.data.TmdbImages
import com.padam.tv.ui.theme.Background

/** Backdrop in the top-right that fades into the background so text on the left stays readable. */
@Composable
fun Backdrop(path: String?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        Crossfade(
            targetState = path,
            animationSpec = tween(600),
            label = "backdrop",
            modifier = Modifier.align(Alignment.TopEnd).fillMaxWidth(0.72f).fillMaxHeight(0.78f),
        ) { p ->
            AsyncImage(
                model = TmdbImages.backdrop(p),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0.26f to Background, 0.48f to Background.copy(alpha = 0.82f), 1f to Background.copy(alpha = 0.15f),
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.35f to Color.Transparent, 0.7f to Background.copy(alpha = 0.85f), 0.82f to Background),
            ),
        )
    }
}
