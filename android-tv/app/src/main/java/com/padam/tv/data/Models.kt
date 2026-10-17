package com.padam.tv.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MoviePage(
    val page: Int = 1,
    val results: List<Movie> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
data class Movie(
    val id: Int,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("original_language") val originalLanguage: String = "",
) {
    val year: String get() = releaseDate?.take(4).orEmpty()
}

@Serializable
data class MovieDetails(
    val id: Int,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String = "",
    val overview: String = "",
    val tagline: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val genres: List<Genre> = emptyList(),
    val credits: Credits? = null,
    val videos: VideoList? = null,
    @SerialName("watch/providers") val watchProviders: WatchProviders? = null,
    val recommendations: MoviePage? = null,
) {
    val year: String get() = releaseDate?.take(4).orEmpty()
    val director: String? get() = credits?.crew?.firstOrNull { it.job == "Director" }?.name
    val music: String? get() = credits?.crew?.firstOrNull { it.job == "Original Music Composer" || it.job == "Music" }?.name

    /** YouTube videos, official trailers first. */
    val youTubeVideos: List<Video>
        get() = videos?.results.orEmpty()
            .filter { it.site == "YouTube" }
            .sortedWith(compareByDescending<Video> { it.type == "Trailer" }.thenByDescending { it.official })
}

@Serializable
data class Genre(val id: Int, val name: String)

@Serializable
data class Credits(
    val cast: List<CastMember> = emptyList(),
    val crew: List<CrewMember> = emptyList(),
)

@Serializable
data class CastMember(
    val name: String,
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
)

@Serializable
data class CrewMember(val name: String, val job: String = "")

@Serializable
data class VideoList(val results: List<Video> = emptyList())

@Serializable
data class Video(
    val key: String,
    val name: String = "",
    val site: String = "",
    val type: String = "",
    val official: Boolean = false,
)

@Serializable
data class WatchProviders(val results: Map<String, CountryProviders> = emptyMap())

@Serializable
data class CountryProviders(
    val link: String? = null,
    val flatrate: List<Provider> = emptyList(),
    val free: List<Provider> = emptyList(),
    val ads: List<Provider> = emptyList(),
    val rent: List<Provider> = emptyList(),
    val buy: List<Provider> = emptyList(),
)

@Serializable
data class Provider(
    @SerialName("provider_id") val id: Int,
    @SerialName("provider_name") val name: String,
    @SerialName("logo_path") val logoPath: String? = null,
)

object TmdbImages {
    fun poster(path: String?) = path?.let { "https://image.tmdb.org/t/p/w342$it" }
    fun backdrop(path: String?) = path?.let { "https://image.tmdb.org/t/p/w1280$it" }
    fun logo(path: String?) = path?.let { "https://image.tmdb.org/t/p/w92$it" }
    fun profile(path: String?) = path?.let { "https://image.tmdb.org/t/p/w185$it" }
}
