package com.coppersmith.music1chat

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class RadioPlayer(
    context: Context
) {
    private val player: ExoPlayer = ExoPlayer.Builder(
        context.applicationContext
    ).build()

    var isPlaying by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player.setAudioAttributes(
            audioAttributes,
            true
        )

        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(
                    playing: Boolean
                ) {
                    isPlaying = playing
                }

                override fun onPlayerError(
                    error: PlaybackException
                ) {
                    isPlaying = false
                    errorMessage =
                        error.message ?: "Unable to play this station."
                }
            }
        )
    }

    fun play(station: Station) {
        errorMessage = null

        player.setMediaItem(
            MediaItem.fromUri(station.location)
        )

        player.prepare()
        player.play()
    }

    fun stop() {
        player.stop()
        isPlaying = false
    }

    fun release() {
        player.release()
    }

    companion object {
        const val TEST_STREAM_URL =
            "https://ice5.somafm.com/groovesalad-128-mp3"
    }
}