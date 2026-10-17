package com.padam.tv.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** My List and Recently viewed, stored on the device. */
class Library(context: Context) {
    private val prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(Movie.serializer())

    private val _myList = MutableStateFlow(read(KEY_LIST))
    val myList: StateFlow<List<Movie>> = _myList

    private val _recent = MutableStateFlow(read(KEY_RECENT))
    val recent: StateFlow<List<Movie>> = _recent

    fun isInList(id: Int) = _myList.value.any { it.id == id }

    /** Adds the film to My List, or removes it if it is already there. Returns whether it is now in the list. */
    fun toggle(movie: Movie): Boolean {
        val inList = isInList(movie.id)
        _myList.value = if (inList) _myList.value.filterNot { it.id == movie.id } else (listOf(movie) + _myList.value).take(200)
        write(KEY_LIST, _myList.value)
        return !inList
    }

    fun markViewed(movie: Movie) {
        _recent.value = (listOf(movie) + _recent.value.filterNot { it.id == movie.id }).take(20)
        write(KEY_RECENT, _recent.value)
    }

    private fun read(key: String): List<Movie> =
        prefs.getString(key, null)?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }.orEmpty()

    private fun write(key: String, movies: List<Movie>) {
        prefs.edit().putString(key, json.encodeToString(serializer, movies)).apply()
    }

    private companion object {
        const val KEY_LIST = "my_list"
        const val KEY_RECENT = "recent"
    }
}
