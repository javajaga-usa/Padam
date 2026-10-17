package com.padam.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.padam.tv.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** How any screen's top bar reaches Settings, provided once by the navigation host. */
val LocalOpenSettings = staticCompositionLocalOf<() -> Unit> { {} }

enum class Tab { Home, Search, Settings, None }

@Composable
fun TopBar(
    selected: Tab,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onSettings: () -> Unit = LocalOpenSettings.current,
) {
    Row(
        modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark()
        Text("Padam", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.padding(start = 10.dp))
        Spacer(Modifier.width(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NavPill("Home", selected == Tab.Home, onHome)
            NavPill("Search", selected == Tab.Search, onSearch)
            NavPill("Settings", selected == Tab.Settings, onSettings)
        }
        Spacer(Modifier.weight(1f))
        Clock()
    }
}

@Composable
private fun Clock() {
    val format = remember { DateTimeFormatter.ofPattern("h:mm a") }
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(15_000)
        }
    }
    Text(now.format(format), color = TextSecondary, fontSize = 17.sp)
}
