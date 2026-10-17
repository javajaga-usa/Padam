package com.padam.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.padam.tv.ui.theme.Accent
import com.padam.tv.ui.theme.AccentInk
import com.padam.tv.ui.theme.Surface2
import com.padam.tv.ui.theme.TextPrimary
import com.padam.tv.ui.theme.TextSecondary

@Composable
fun CenteredMessage(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val focus = remember { FocusRequester() }
    Column(
        Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(64.dp).background(Surface2, CircleShape), contentAlignment = Alignment.Center) {
            Text("!", color = Accent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
        Text(
            message,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 560.dp).padding(bottom = 24.dp),
        )
        if (actionLabel != null) {
            PrimaryButton(actionLabel, onAction, Modifier.focusRequester(focus))
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        }
    }
}

@Composable
fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.colors(
            containerColor = TextPrimary,
            contentColor = MaterialTheme.colorScheme.background,
            focusedContainerColor = Accent,
            focusedContentColor = AccentInk,
        ),
    ) { Text(label, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun SecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, highlighted: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.colors(
            containerColor = if (highlighted) Accent.copy(alpha = 0.18f) else Surface2,
            contentColor = if (highlighted) Accent else TextPrimary,
            focusedContainerColor = Accent,
            focusedContentColor = AccentInk,
        ),
    ) { Text(label, fontWeight = FontWeight.SemiBold) }
}
