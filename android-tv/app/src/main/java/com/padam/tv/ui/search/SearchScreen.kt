package com.padam.tv.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.padam.tv.Graph
import com.padam.tv.ui.components.MovieCard
import com.padam.tv.ui.components.PosterWidth
import com.padam.tv.ui.components.Tab
import com.padam.tv.ui.components.TopBar
import com.padam.tv.ui.theme.Accent
import com.padam.tv.ui.theme.Line
import com.padam.tv.ui.theme.Surface1
import com.padam.tv.ui.theme.Surface2
import com.padam.tv.ui.theme.TextFaint
import com.padam.tv.ui.theme.TextPrimary
import com.padam.tv.ui.theme.TextSecondary

@Composable
fun SearchScreen(
    onMovieClick: (Int) -> Unit,
    onHome: () -> Unit,
    viewModel: SearchViewModel = viewModel { SearchViewModel(Graph.repository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val field = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var fieldFocused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { field.requestFocus() } }

    val showingSuggestions = state.query.isBlank()
    val movies = if (showingSuggestions) state.suggestions else state.results
    val q = state.query.trim()
    val status = when {
        showingSuggestions -> if (state.suggestions.isEmpty()) "Type a film title, for example Vikram, Jailer or Nayakan." else "Popular right now"
        state.searching -> "Searching…"
        state.error -> "Search failed. Check the internet connection."
        state.results.isEmpty() -> "No Tamil films match “$q”. Try a shorter title."
        else -> "${state.results.size} ${if (state.results.size == 1) "film" else "films"} for “$q”"
    }

    Column(Modifier.fillMaxSize().padding(start = 56.dp, end = 56.dp, top = 20.dp)) {
        TopBar(Tab.Search, onHome = onHome, onSearch = {})
        Row(
            Modifier
                .padding(top = 28.dp, bottom = 14.dp)
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .background(if (fieldFocused) Surface2 else Surface1, CircleShape)
                .border(2.dp, if (fieldFocused) Accent else Line, CircleShape)
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⌕", color = TextSecondary, fontSize = 24.sp, modifier = Modifier.padding(end = 14.dp))
            Box(Modifier.fillMaxWidth()) {
                if (state.query.isEmpty()) {
                    Text("Search Tamil films", color = TextFaint, style = MaterialTheme.typography.titleLarge)
                }
                BasicTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(color = TextPrimary),
                    cursorBrush = SolidColor(Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(field)
                        .onFocusChanged { fieldFocused = it.isFocused },
                )
            }
        }
        Text(status, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(PosterWidth),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp, start = 6.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(movies, key = { it.id }) { movie ->
                MovieCard(movie, onClick = { onMovieClick(movie.id) })
            }
        }
    }
}
