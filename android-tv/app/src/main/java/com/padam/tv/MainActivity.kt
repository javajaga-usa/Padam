package com.padam.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.padam.tv.ui.components.CenteredMessage
import com.padam.tv.ui.details.DetailsScreen
import com.padam.tv.ui.home.HomeScreen
import com.padam.tv.ui.search.SearchScreen
import com.padam.tv.ui.theme.Background
import com.padam.tv.ui.theme.PadamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PadamTheme {
                Box(Modifier.fillMaxSize().background(Background)) {
                    if (Graph.hasApiKey) PadamNavHost()
                    else CenteredMessage("Add TMDB_API_KEY to local.properties (or the environment) and rebuild. See README.")
                }
            }
        }
    }
}

@Composable
private fun PadamNavHost() {
    val nav = rememberNavController()
    val openMovie: (Int) -> Unit = { id -> nav.navigate("movie/$id") }
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(onMovieClick = openMovie, onSearchClick = { nav.navigate("search") })
        }
        composable("search") {
            SearchScreen(onMovieClick = openMovie)
        }
        composable(
            "movie/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) { entry ->
            DetailsScreen(movieId = entry.arguments?.getInt("id") ?: 0, onMovieClick = openMovie)
        }
    }
}
