package com.padam.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.padam.tv.ui.details.DetailsScreen
import com.padam.tv.ui.home.HomeScreen
import com.padam.tv.ui.components.LocalOpenSettings
import com.padam.tv.ui.person.PersonScreen
import com.padam.tv.ui.player.PlayerScreen
import com.padam.tv.ui.settings.SettingsScreen
import com.padam.tv.util.Launchers
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.padam.tv.ui.search.SearchScreen
import com.padam.tv.ui.setup.KeySetupScreen
import com.padam.tv.ui.theme.Background
import com.padam.tv.ui.theme.PadamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.init(this)
        setContent {
            PadamTheme {
                Box(Modifier.fillMaxSize().background(Background)) {
                    val apiKey by Graph.apiKey.collectAsState()
                    var editingKey by remember { mutableStateOf(false) }
                    if (apiKey.isBlank() || editingKey) {
                        KeySetupScreen(initial = apiKey) {
                            Graph.setApiKey(it)
                            editingKey = false
                        }
                    } else {
                        // A new key starts navigation afresh so every screen loads with it.
                        key(apiKey) { PadamNavHost(onChangeKey = { editingKey = true }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PadamNavHost(onChangeKey: () -> Unit) {
    val nav = rememberNavController()
    val openMovie: (Int) -> Unit = { id -> nav.navigate("movie/$id") }
    val openPerson: (Int) -> Unit = { id -> nav.navigate("person/$id") }
    val goHome: () -> Unit = { nav.popBackStack("home", inclusive = false) }
    val goSearch: () -> Unit = { nav.navigate("search") { launchSingleTop = true } }
    val goSettings: () -> Unit = { nav.navigate("settings") { launchSingleTop = true } }
    val context = LocalContext.current
    val play: (String) -> Unit = { key ->
        if (Graph.youTubeInApp.value) nav.navigate("play/$key") else Launchers.playYouTube(context, key)
    }
    CompositionLocalProvider(LocalOpenSettings provides goSettings) {
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(onMovieClick = openMovie, onSearchClick = goSearch, onChangeKey = onChangeKey)
        }
        composable("search") {
            SearchScreen(onMovieClick = openMovie, onHome = goHome)
        }
        composable(
            "movie/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) { entry ->
            DetailsScreen(
                movieId = entry.arguments?.getInt("id") ?: 0,
                onMovieClick = openMovie,
                onHome = goHome,
                onSearch = goSearch,
                onPersonClick = openPerson,
                onPlay = play,
            )
        }
        composable(
            "person/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) { entry ->
            PersonScreen(
                personId = entry.arguments?.getInt("id") ?: 0,
                onMovieClick = openMovie,
                onHome = goHome,
                onSearch = goSearch,
            )
        }
        composable("settings") {
            SettingsScreen(onHome = goHome, onSearch = goSearch, onChangeKey = onChangeKey)
        }
        composable("play/{key}") { entry ->
            PlayerScreen(videoKey = entry.arguments?.getString("key").orEmpty(), onExit = { nav.popBackStack() })
        }
    }
    }
}
