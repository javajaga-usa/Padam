package com.padam.tv

import com.padam.tv.data.MovieRepository
import com.padam.tv.data.TmdbApi

/** Minimal service locator; the app is small enough not to need a DI framework. */
object Graph {
    val hasApiKey: Boolean get() = BuildConfig.TMDB_API_KEY.isNotBlank()

    val repository: MovieRepository by lazy {
        MovieRepository(
            api = TmdbApi.create(BuildConfig.TMDB_API_KEY, BuildConfig.TMDB_LANGUAGE),
            region = BuildConfig.TMDB_REGION,
        )
    }
}
