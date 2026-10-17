package com.padam.tv.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.padam.tv.Graph
import com.padam.tv.ui.components.Tab
import com.padam.tv.ui.components.TopBar
import com.padam.tv.ui.theme.Accent
import com.padam.tv.ui.theme.Surface1
import com.padam.tv.ui.theme.Surface2
import com.padam.tv.ui.theme.TextFaint
import com.padam.tv.ui.theme.TextPrimary
import com.padam.tv.ui.theme.TextSecondary

@Composable
fun SettingsScreen(onHome: () -> Unit, onSearch: () -> Unit, onChangeKey: () -> Unit) {
    val inApp by Graph.youTubeInApp.collectAsState()
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Column(Modifier.fillMaxSize().padding(start = 56.dp, end = 56.dp, top = 20.dp)) {
        TopBar(Tab.Settings, onHome = onHome, onSearch = onSearch, onSettings = {})
        Text("Settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 30.dp, bottom = 20.dp))

        Text("Play YouTube videos in", style = MaterialTheme.typography.titleMedium)
        Text(
            "YouTube Premium removes ads only in the YouTube app, where you are signed in.",
            color = TextFaint,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Choice("Padam", "Stays in this app", selected = inApp, onClick = { Graph.setYouTubeInApp(true) }, modifier = Modifier.focusRequester(first))
            Choice("YouTube app", "Ad-free with Premium", selected = !inApp, onClick = { Graph.setYouTubeInApp(false) })
        }

        Spacer(Modifier.padding(top = 30.dp))
        Text("Film database", style = MaterialTheme.typography.titleMedium)
        Text("The TMDB key Padam uses for lists and posters.", color = TextFaint, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        Choice("Change TMDB key", "Saved on this TV", selected = false, onClick = onChangeKey)
    }
}

@Composable
private fun Choice(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        modifier = modifier.width(260.dp),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Accent.copy(alpha = 0.16f) else Surface1,
            contentColor = TextPrimary,
            focusedContainerColor = Surface2,
            focusedContentColor = TextPrimary,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(2.dp, if (selected) Accent else Surface2), shape = shape),
            focusedBorder = Border(BorderStroke(3.dp, Accent), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (selected) {
                    Spacer(Modifier.width(8.dp))
                    Text("✓", color = Accent, fontWeight = FontWeight.Bold)
                }
            }
            Text(subtitle, color = TextSecondary, fontSize = 13.sp)
        }
    }
}

