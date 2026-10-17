package com.padam.tv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.padam.tv.ui.theme.Accent
import com.padam.tv.ui.theme.AccentInk
import com.padam.tv.ui.theme.Line
import com.padam.tv.ui.theme.Surface1
import com.padam.tv.ui.theme.Surface2
import com.padam.tv.ui.theme.TextFaint
import com.padam.tv.ui.theme.TextPrimary
import com.padam.tv.ui.theme.TextSecondary

@Composable
fun RatingBadge(rating: Double) {
    Text(
        "★ %.1f".format(rating),
        color = AccentInk,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.background(Accent, RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
fun Chip(label: String) {
    Text(
        label,
        color = TextSecondary,
        fontSize = 13.sp,
        modifier = Modifier.border(1.dp, Line, CircleShape).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Rating badge, year · runtime, and genre chips on one line. */
@Composable
fun MetaRow(rating: Double, parts: List<String>, genres: List<String>) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (rating > 0) RatingBadge(rating)
        if (parts.isNotEmpty()) {
            Text(parts.joinToString("  ·  "), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        genres.forEach { Chip(it) }
    }
}

/** A focusable pill used for the top navigation. */
@Composable
fun NavPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.07f) else Color.Transparent,
            contentColor = if (selected) TextPrimary else TextSecondary,
            focusedContainerColor = Color.White.copy(alpha = 0.1f),
            focusedContentColor = TextPrimary,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, Accent), shape = CircleShape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
    ) {
        Text(label, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp))
    }
}

@Composable
fun BrandMark(size: Dp = 30.dp) {
    Box(
        Modifier.size(size).background(Accent, RoundedCornerShape(size * 0.25f)),
        contentAlignment = Alignment.Center,
    ) {
        Text("▶", color = AccentInk, fontSize = (size.value * 0.45f).sp)
    }
}

/** Shimmering placeholder block shown while content loads. */
@Composable
fun Skeleton(modifier: Modifier, shape: Shape = RoundedCornerShape(8.dp)) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1600f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer-x",
    )
    Box(
        modifier.clip(shape).background(
            Brush.linearGradient(
                colors = listOf(Surface1, Surface2, Surface1),
                start = Offset(x, 0f),
                end = Offset(x + 600f, 0f),
            ),
        ),
    )
}

/** Circle with a person's initials, used when TMDB has no photo. */
@Composable
fun Initials(name: String, size: Dp) {
    Box(Modifier.size(size).background(Surface2, CircleShape), contentAlignment = Alignment.Center) {
        Text(
            name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() },
            color = TextSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.28f).sp,
        )
    }
}

@Composable
fun SectionTitle(title: String, note: String? = null, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (note != null) Text(note, color = TextFaint, fontSize = 12.sp, modifier = Modifier.padding(bottom = 2.dp))
    }
}

val PosterWidth = 136.dp
val CardShape = RoundedCornerShape(10.dp)

