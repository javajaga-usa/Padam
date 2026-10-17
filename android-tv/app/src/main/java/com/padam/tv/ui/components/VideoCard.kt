package com.padam.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.padam.tv.data.TmdbImages
import com.padam.tv.data.Video
import com.padam.tv.ui.theme.Accent
import com.padam.tv.ui.theme.AccentInk
import com.padam.tv.ui.theme.TextFaint

/** 16:9 YouTube thumbnail with a play button, for trailers, songs and clips. */
@Composable
fun VideoCard(video: Video, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier.width(240.dp)) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).onFocusChanged { focused = it.isFocused },
            shape = CardDefaults.shape(CardShape),
            scale = CardDefaults.scale(focusedScale = 1.05f),
            border = CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Accent), shape = CardShape)),
        ) {
            Box(
                Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF2A2230), Color(0xFF17171D)))),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = TmdbImages.youTubeThumb(video.key),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .size(44.dp)
                        .background(if (focused) Accent else Color.Black.copy(alpha = 0.55f), CircleShape)
                        .border(2.dp, if (focused) Accent else Color.White.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("▶", color = if (focused) AccentInk else Color.White, fontSize = 16.sp)
                }
            }
        }
        Text(video.name, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text(video.type, fontSize = 12.sp, color = TextFaint)
    }
}

