package com.padam.tv.data

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface TmdbApi {
    @GET("discover/movie")
    suspend fun discover(@QueryMap params: Map<String, String>): MoviePage

    @GET("search/movie")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): MoviePage

    @GET("movie/{id}")
    suspend fun details(
        @Path("id") id: Int,
        @Query("append_to_response") append: String = "credits,videos,watch/providers,recommendations,release_dates",
    ): MovieDetails

    @GET("person/{id}")
    suspend fun person(
        @Path("id") id: Int,
        @Query("append_to_response") append: String = "movie_credits",
    ): PersonDetails

    companion object {
        private const val BASE_URL = "https://api.themoviedb.org/3/"

        /**
         * Accepts either a v3 API key (sent as `api_key`) or a v4 read access token (sent as Bearer).
         */
        fun create(apiKey: String, language: String): TmdbApi {
            val auth = Interceptor { chain ->
                val original = chain.request()
                val url = original.url.newBuilder()
                    .addQueryParameter("language", language)
                    .apply { if (apiKey.length <= 40) addQueryParameter("api_key", apiKey) }
                    .build()
                val request = original.newBuilder().url(url)
                    .apply { if (apiKey.length > 40) header("Authorization", "Bearer $apiKey") }
                    .build()
                chain.proceed(request)
            }
            val client = OkHttpClient.Builder().addInterceptor(auth).build()
            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                explicitNulls = false
            }
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TmdbApi::class.java)
        }
    }
}
