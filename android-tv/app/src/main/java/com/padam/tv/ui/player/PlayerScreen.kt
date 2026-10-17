package com.padam.tv.ui.player

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Text
import com.padam.tv.util.Launchers
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay

/**
 * Full-screen YouTube player inside Padam. The remote drives it: OK or Play/Pause toggles, left/right
 * skip 10 seconds, Back leaves. If the uploader doesn't allow playback in other apps, it hands the
 * video to the YouTube app instead.
 */
@Composable
fun PlayerScreen(videoKey: String, onExit: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val focus = remember { FocusRequester() }
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var second by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(0f) }
    var hint by remember { mutableStateOf<String?>(null) }
    var hintId by remember { mutableIntStateOf(0) }

    fun flash(text: String) {
        hint = text
        hintId++
    }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(hintId) {
        delay(1500)
        hint = null
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val p = player ?: return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (playing) p.pause() else p.play()
                        flash(if (playing) "❚❚  Paused" else "▶  Playing")
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> { p.play(); true }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> { p.pause(); true }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        val to = if (duration > 0) minOf(second + 10f, duration - 1f) else second + 10f
                        p.seekTo(to)
                        flash("+10s  ·  ${clock(to)}")
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        val to = maxOf(second - 10f, 0f)
                        p.seekTo(to)
                        flash("−10s  ·  ${clock(to)}")
                        true
                    }
                    else -> false
                }
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                YouTubePlayerView(ctx).apply {
                    enableAutomaticInitialization = false
                    lifecycle.addObserver(this)
                    val options = IFramePlayerOptions.Builder(ctx).controls(1).rel(0).ivLoadPolicy(3).fullscreen(0).build()
                    initialize(
                        object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                player = youTubePlayer
                                youTubePlayer.loadVideo(videoKey, 0f)
                            }

                            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                                playing = state == PlayerConstants.PlayerState.PLAYING
                                if (state == PlayerConstants.PlayerState.ENDED) onExit()
                            }

                            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, at: Float) {
                                second = at
                            }

                            override fun onVideoDuration(youTubePlayer: YouTubePlayer, length: Float) {
                                duration = length
                            }

                            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                                // Some uploaders only allow playback on YouTube itself.
                                Launchers.playYouTube(context, videoKey)
                                onExit()
                            }
                        },
                        true,
                        options,
                    )
                }
            },
            onRelease = { view ->
                lifecycle.removeObserver(view)
                view.release()
            },
        )
        AnimatedVisibility(
            visible = hint != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(28.dp),
        ) {
            Text(
                hint.orEmpty(),
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape).padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
    DisposableEffect(Unit) { onDispose { player = null } }
}

private fun clock(seconds: Float): String {
    val s = seconds.toInt()
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}
