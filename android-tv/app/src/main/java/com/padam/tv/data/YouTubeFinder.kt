package com.padam.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration

/** A full film on YouTube, uploaded by a channel that licenses it. */
data class FreeVideo(val key: String, val title: String, val channel: String)

/**
 * Looks for a full-length upload of a film on YouTube and keeps it only when YouTube marks the video as
 * licensed content (uploaded by a channel that holds the rights through Content ID), it runs over an hour,
 * and its title names the film. Pirated uploads don't carry that flag, so they are skipped.
 */
class YouTubeFinder(private val apiKey: String) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<Int, FreeVideo?>()

    suspend fun find(movieId: Int, title: String, year: String): FreeVideo? {
        if (cache.containsKey(movieId)) return cache[movieId]
        val found = runCatching { search(title, year) }.getOrNull()
        cache[movieId] = found
        return found
    }

    private suspend fun search(title: String, year: String): FreeVideo? = withContext(Dispatchers.IO) {
        val searchUrl = "https://www.googleapis.com/youtube/v3/search".toHttpUrl().newBuilder()
            .addQueryParameter("part", "snippet")
            .addQueryParameter("type", "video")
            .addQueryParameter("videoDuration", "long")
            .addQueryParameter("maxResults", "10")
            .addQueryParameter("q", "$title $year Tamil full movie")
            .addQueryParameter("key", apiKey)
            .build()
        val ids = get(searchUrl.toString())["items"]?.jsonArray.orEmpty()
            .mapNotNull { it.jsonObject["id"]?.jsonObject?.get("videoId")?.jsonPrimitive?.content }
        if (ids.isEmpty()) return@withContext null

        val videosUrl = "https://www.googleapis.com/youtube/v3/videos".toHttpUrl().newBuilder()
            .addQueryParameter("part", "snippet,contentDetails")
            .addQueryParameter("id", ids.joinToString(","))
            .addQueryParameter("key", apiKey)
            .build()
        val wanted = normalize(title)
        get(videosUrl.toString())["items"]?.jsonArray.orEmpty()
            .map { it.jsonObject }
            .mapNotNull { v ->
                val details = v["contentDetails"]?.jsonObject ?: return@mapNotNull null
                val snippet = v["snippet"]?.jsonObject ?: return@mapNotNull null
                val licensed = details["licensedContent"]?.jsonPrimitive?.booleanOrNull == true
                val minutes = runCatching { Duration.parse(details["duration"]?.jsonPrimitive?.content.orEmpty()).toMinutes() }.getOrDefault(0)
                val videoTitle = snippet["title"]?.jsonPrimitive?.content.orEmpty()
                if (!licensed || minutes < 60 || !normalize(videoTitle).contains(wanted)) return@mapNotNull null
                FreeVideo(
                    key = v["id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    title = videoTitle,
                    channel = snippet["channelTitle"]?.jsonPrimitive?.content.orEmpty(),
                )
            }
            .firstOrNull()
    }

    private fun get(url: String): JsonObject =
        client.newCall(Request.Builder().url(url).build()).execute().use { res ->
            json.parseToJsonElement(res.body?.string().orEmpty()).jsonObject
        }

    private fun normalize(s: String) = s.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
}
