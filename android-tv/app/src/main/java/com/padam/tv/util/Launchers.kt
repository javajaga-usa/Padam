package com.padam.tv.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Hands playback off to apps that hold the rights. Padam itself never streams video.
 */
object Launchers {

    // Best-effort map from TMDB provider names to Android TV (then phone) package names.
    private val providerPackages: List<Pair<String, List<String>>> = listOf(
        "netflix" to listOf("com.netflix.ninja", "com.netflix.mediaclient"),
        "prime video" to listOf("com.amazon.amazonvideo.livingroom"),
        "hotstar" to listOf("in.startv.hotstartvonly", "in.startv.hotstar"),
        "sun nxt" to listOf("com.suntv.sunnxt"),
        "zee5" to listOf("com.graymatrix.did"),
        "sony liv" to listOf("com.sonyliv"),
        "sonyliv" to listOf("com.sonyliv"),
        "jio cinema" to listOf("com.jio.media.ondemand"),
        "youtube" to listOf("com.google.android.youtube.tv", "com.google.android.youtube"),
    )

    fun playYouTube(context: Context, videoKey: String) {
        val attempts = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoKey")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoKey")),
        )
        if (attempts.none { context.tryStart(it) }) {
            context.toast("Install YouTube to play this video")
        }
    }

    /** Opens the provider's app if installed, else its Play Store page, else the TMDB "where to watch" page. */
    fun openProvider(context: Context, providerName: String, fallbackLink: String?) {
        val pm = context.packageManager
        val packages = providerPackages
            .firstOrNull { (name, _) -> providerName.lowercase().replace("+", "").contains(name) }
            ?.second.orEmpty()

        for (pkg in packages) {
            val launch = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
            if (launch != null && context.tryStart(launch)) {
                context.toast("Search for the film in $providerName")
                return
            }
        }
        packages.firstOrNull()?.let { pkg ->
            if (context.tryStart(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))) return
        }
        if (fallbackLink != null && context.tryStart(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackLink)))) return
        context.toast("$providerName isn't available on this device")
    }

    /** Opens Einthusan's search for the film, where it plays with the viewer's own account. */
    fun openEinthusan(context: Context, title: String) {
        val url = "https://einthusan.tv/movie/results/?lang=tamil&query=" + Uri.encode(title)
        if (!context.tryStart(Intent(Intent.ACTION_VIEW, Uri.parse(url)))) {
            context.toast("Install a web browser to open Einthusan")
        }
    }

    private fun Context.tryStart(intent: Intent): Boolean = try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    private fun Context.toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
