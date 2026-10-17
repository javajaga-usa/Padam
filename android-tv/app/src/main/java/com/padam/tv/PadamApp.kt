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
        _apiKey.value = prefs.getString("tmdb_key", null)?.takeIf { it.isNotBlank() } ?: BuildConfig.TMDB_API_KEY
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
