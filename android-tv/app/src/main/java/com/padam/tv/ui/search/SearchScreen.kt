package com.padam.tv.ui.search

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.FilterChip
import com.padam.tv.data.Movie
import com.padam.tv.ui.components.SecondaryButton
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
    val context = LocalContext.current
    val recent by Graph.recentSearches.collectAsState()
    var decade by remember { mutableStateOf(Decade.All) }
    var sort by remember { mutableStateOf(Sort.BestMatch) }
    var voiceMissing by remember { mutableStateOf(false) }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let {
            viewModel.onQueryChange(it)
            Graph.rememberSearch(it)
        }
    }
    val startVoice = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a film title")
        try { voice.launch(intent) } catch (_: ActivityNotFoundException) { voiceMissing = true }
    }
    val open = { movie: Movie ->
        Graph.rememberSearch(state.query)
        onMovieClick(movie.id)
    }

    val showingSuggestions = state.query.isBlank()
    val found = if (showingSuggestions) state.suggestions else state.results
    val movies = sort.apply(found.filter { decade.matches(it.year) })
    val q = state.query.trim()
    val status = when {
        showingSuggestions -> if (state.suggestions.isEmpty()) "Type a film title, for example Vikram, Jailer or Nayakan." else "Popular right now"
        state.searching -> "Searching…"
        state.error -> "Search failed. Check the internet connection."
        state.results.isEmpty() -> "No Tamil films match “$q”. Try a shorter title."
        movies.isEmpty() -> "None of the ${state.results.size} matches are from ${decade.label}."
        else -> "${state.results.size} ${if (state.results.size == 1) "film" else "films"} for “$q”"
    }

    Column(Modifier.fillMaxSize().padding(start = 56.dp, end = 56.dp, top = 20.dp)) {
        TopBar(Tab.Search, onHome = onHome, onSearch = {})
        Row(Modifier.padding(top = 28.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .widthIn(max = 760.dp)
                .weight(1f, fill = false)
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
            Spacer(Modifier.width(14.dp))
            SecondaryButton("🎤  Voice", startVoice)
        }
        if (voiceMissing) {
            Text("Voice search isn't available on this TV. Use the remote's mic button or type instead.", color = TextFaint, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (showingSuggestions && recent.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                item { Text("Recent", color = TextFaint, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, end = 4.dp)) }
                rowItems(recent) { r -> FilterChip(selected = false, onClick = { viewModel.onQueryChange(r) }) { Text(r) } }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
            rowItems(Decade.entries) { d -> FilterChip(selected = decade == d, onClick = { decade = d }) { Text(d.label) } }
            item { Spacer(Modifier.width(24.dp)) }
            rowItems(Sort.entries) { s -> FilterChip(selected = sort == s, onClick = { sort = s }) { Text(s.label) } }
        }
        Text(status, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(PosterWidth),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp, start = 6.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(movies, key = { it.id }) { movie ->
                MovieCard(movie, onClick = { open(movie) })
            }
        }
    }
}

private enum class Decade(val label: String, val from: Int, val to: Int) {
    All("All years", 0, 9999),
    D2020("2020s", 2020, 2029),
    D2010("2010s", 2010, 2019),
    D2000("2000s", 2000, 2009),
    D1990("1990s", 1990, 1999),
    Older("Before 1990", 0, 1989);

    fun matches(year: String): Boolean {
        if (this == All) return true
        val y = year.toIntOrNull() ?: return false
        return y in from..to
    }
}

private enum class Sort(val label: String) {
    BestMatch("Best match"),
    Newest("Newest"),
    TopRated("Top rated");

    fun apply(movies: List<Movie>): List<Movie> = when (this) {
        BestMatch -> movies
        Newest -> movies.sortedByDescending { it.releaseDate.orEmpty() }
        TopRated -> movies.sortedByDescending { it.voteAverage }
    }
}
