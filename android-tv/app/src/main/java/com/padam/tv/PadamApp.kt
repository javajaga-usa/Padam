package com.padam.tv

import android.content.Context
import com.padam.tv.data.Library
import com.padam.tv.data.MovieRepository
import com.padam.tv.data.TmdbApi
import com.padam.tv.data.YouTubeFinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Minimal service locator; the app is small enough not to need a DI framework. */
object Graph {
    private lateinit var appContext: Context
    private val prefs by lazy { appContext.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    /** The key typed on the TV wins over the one baked in at build time, so a key can be added without rebuilding. */
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    lateinit var library: Library
        private set

    private var repo: MovieRepository? = null
    private var repoKey: String? = null

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        library = Library(appContext)
        _youTubeInApp.value = prefs.getBoolean("youtube_in_app", true)
        _recentSearches.value = prefs.getString("recent_searches", null).orEmpty().split('\n').filter { it.isNotBlank() }
        _apiKey.value = prefs.getString("tmdb_key", null)?.takeIf { it.isNotBlank() } ?: BuildConfig.TMDB_API_KEY
    }

    /** Whether YouTube videos play inside Padam (true) or in the YouTube app, where YouTube Premium removes ads. */
    private val _youTubeInApp = MutableStateFlow(true)
    val youTubeInApp: StateFlow<Boolean> = _youTubeInApp

    fun setYouTubeInApp(inApp: Boolean) {
        prefs.edit().putBoolean("youtube_in_app", inApp).apply()
        _youTubeInApp.value = inApp
    }

    /** The last few searches, newest first, offered again on the search screen. */
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches

    fun rememberSearch(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        val list = (listOf(q) + _recentSearches.value.filterNot { it.equals(q, ignoreCase = true) }).take(6)
        prefs.edit().putString("recent_searches", list.joinToString("\n")).apply()
        _recentSearches.value = list
    }

    fun setApiKey(key: String) {
        prefs.edit().putString("tmdb_key", key.trim()).apply()
        _apiKey.value = key.trim()
    }

    val repository: MovieRepository
        @Synchronized get() {
            val key = _apiKey.value
            repo?.takeIf { repoKey == key }?.let { return it }
            return MovieRepository(
                api = TmdbApi.create(key, BuildConfig.TMDB_LANGUAGE),
                region = BuildConfig.TMDB_REGION,
            ).also { repo = it; repoKey = key }
        }

    /** Finds full films that rights holders put on YouTube; null when no YouTube key was configured. */
    val youTube: YouTubeFinder? by lazy {
        BuildConfig.YOUTUBE_API_KEY.takeIf { it.isNotBlank() }?.let { YouTubeFinder(it) }
    }
}
