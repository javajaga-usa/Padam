package com.padam.tv.data

import java.time.LocalDate

/** A home-screen row: a title and the TMDB discover filters that fill it. */
data class Shelf(val title: String, val params: Map<String, String>)

class MovieRepository(
    private val api: TmdbApi,
    private val region: String,
) {
    private val shelfCache = mutableMapOf<String, List<Movie>>()
    private val detailsCache = mutableMapOf<Int, MovieDetails>()

    private val tamil = mapOf(
        "with_original_language" to "ta",
        "include_adult" to "false",
    )

    fun shelves(today: LocalDate = LocalDate.now()): List<Shelf> = listOf(
        Shelf(
            "In theatres",
            tamil + mapOf(
                "sort_by" to "popularity.desc",
                "primary_release_date.gte" to today.minusDays(45).toString(),
                "primary_release_date.lte" to today.toString(),
            ),
        ),
        Shelf(
            "Streaming on your apps",
            tamil + mapOf(
                "sort_by" to "primary_release_date.desc",
                "watch_region" to region,
                "with_watch_monetization_types" to "flatrate|free",
                "primary_release_date.lte" to today.toString(),
            ),
        ),
        Shelf("Popular", tamil + mapOf("sort_by" to "popularity.desc")),
        Shelf(
            "Top rated",
            tamil + mapOf("sort_by" to "vote_average.desc", "vote_count.gte" to "150"),
        ),
        Shelf(
            "Coming soon",
            tamil + mapOf(
                "sort_by" to "popularity.desc",
                "primary_release_date.gte" to today.plusDays(1).toString(),
            ),
        ),
        Shelf("Action", tamil + genre(28)),
        Shelf("Comedy", tamil + genre(35)),
        Shelf("Thriller", tamil + genre(53)),
        Shelf("Romance", tamil + genre(10749)),
        Shelf("Family", tamil + genre(10751)),
        Shelf(
            "Classics",
            tamil + mapOf(
                "sort_by" to "vote_average.desc",
                "vote_count.gte" to "50",
                "primary_release_date.lte" to "1999-12-31",
            ),
        ),
    )

    private fun genre(id: Int) = mapOf("with_genres" to id.toString(), "sort_by" to "popularity.desc", "vote_count.gte" to "10")

    suspend fun shelf(shelf: Shelf): List<Movie> =
        shelfCache[shelf.title] ?: api.discover(shelf.params).results
            .filter { it.posterPath != null }
            .also { shelfCache[shelf.title] = it }

    /** Search limited to Tamil-language films so results stay on topic. */
    suspend fun search(query: String): List<Movie> =
        api.search(query).results.filter { it.originalLanguage == "ta" }

    suspend fun details(id: Int): MovieDetails =
        detailsCache[id] ?: api.details(id).also { detailsCache[id] = it }

    suspend fun person(id: Int): PersonDetails = api.person(id)

    fun providersFor(details: MovieDetails): CountryProviders? =
        details.watchProviders?.results?.get(region)
}
