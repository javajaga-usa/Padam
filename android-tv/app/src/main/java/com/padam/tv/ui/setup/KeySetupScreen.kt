package com.padam.tv.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.padam.tv.ui.components.BrandMark
import com.padam.tv.ui.components.PrimaryButton
import com.padam.tv.ui.theme.Accent
import com.padam.tv.ui.theme.Line
import com.padam.tv.ui.theme.Surface1
import com.padam.tv.ui.theme.Surface2
import com.padam.tv.ui.theme.TextFaint
import com.padam.tv.ui.theme.TextPrimary
import com.padam.tv.ui.theme.TextSecondary

/** First-run screen: type the free TMDB key once, so the app works without rebuilding it. */
@Composable
fun KeySetupScreen(initial: String, onSave: (String) -> Unit) {
    var key by remember { mutableStateOf(initial) }
    var focused by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { field.requestFocus() } }
    val valid = key.trim().length >= 32

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.width(720.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(40.dp)
                Text("Padam", fontSize = 30.sp, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 14.dp))
            }
            Text("Connect your film database", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 36.dp))
            Text(
                "Padam gets Tamil film lists, posters and \"where to watch\" from TMDB. A key is free: create an account at " +
                    "themoviedb.org, open Settings → API, and copy the API key or the read access token.",
                color = TextSecondary,
                modifier = Modifier.padding(top = 10.dp),
            )
            Box(
                Modifier
                    .padding(top = 26.dp)
                    .fillMaxWidth()
                    .background(if (focused) Surface2 else Surface1, RoundedCornerShape(10.dp))
                    .border(2.dp, if (focused) Accent else Line, RoundedCornerShape(10.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                if (key.isEmpty()) Text("Paste or type your TMDB key", color = TextFaint, fontFamily = FontFamily.Monospace)
                BasicTextField(
                    value = key,
                    onValueChange = { key = it.trim() },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (valid) onSave(key) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(field).onFocusChanged { focused = it.isFocused },
                )
            }
            Row(
                Modifier.padding(top = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryButton("Save and start", { if (valid) onSave(key) })
                Text(
                    if (valid || key.isEmpty()) "Saved on this TV only." else "That looks too short for a TMDB key.",
                    color = TextFaint,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
