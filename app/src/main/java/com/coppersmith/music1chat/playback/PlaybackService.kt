package com.coppersmith.music1chat.playback

import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.coppersmith.music1chat.persistence.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    private lateinit var player: ExoPlayer

    private lateinit var appPreferences: AppPreferences

    private val playbackScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main
        )

    private var retryJob: Job? = null

    private var retryCount = 0

    private var playbackGeneration = 0L

    /*
     * This remains true while the user expects music to play.
     * Temporary network errors do not change it.
     */
    private var playbackRequested = false

    override fun onCreate() {
        super.onCreate()

        appPreferences =
            AppPreferences(
                applicationContext
            )

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(
                    C.AUDIO_CONTENT_TYPE_MUSIC
                )
                .build()

        player =
            ExoPlayer.Builder(this)
                .build()
                .apply {
                    setAudioAttributes(
                        audioAttributes,
                        true
                    )

                    setWakeMode(
                        C.WAKE_MODE_NETWORK
                    )
                }

        player.addListener(
            object : Player.Listener {

                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int
                ) {
                    playbackGeneration++
                    retryCount = 0
                    cancelRetry()
                }

                override fun onPlayWhenReadyChanged(
                    playWhenReady: Boolean,
                    reason: Int
                ) {
                    playbackRequested =
                        playWhenReady

                    if (!playWhenReady) {
                        playbackGeneration++
                        retryCount = 0
                        cancelRetry()
                    }
                }

                override fun onIsPlayingChanged(
                    isPlaying: Boolean
                ) {
                    if (isPlaying) {
                        retryCount = 0
                        cancelRetry()

                        Log.d(
                            "KenCheck",
                            "Playback service recovered: " +
                                    currentStationName()
                        )
                    }
                }

                override fun onPlayerError(
                    error: PlaybackException
                ) {
                    Log.e(
                        "KenCheck",
                        "Playback service error: " +
                                "${error.errorCodeName}: " +
                                "${error.message}",
                        error
                    )

                    if (
                        isTemporaryNetworkFailure(error) &&
                        playbackRequested
                    ) {
                        scheduleNetworkRetry()
                    }
                }
            }
        )

        mediaSession =
            MediaSession.Builder(
                this,
                player
            ).build()

        Log.d(
            "KenCheck",
            "PlaybackService created"
        )
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        return mediaSession
    }

    /*
     * The user explicitly removed Music1Chat from Recents,
     * including by pressing Close all.
     *
     * For this app, that means:
     *
     * stop playback;
     * cancel network retries;
     * remember that playback was stopped;
     * stop the playback service.
     */
    override fun onTaskRemoved(
        rootIntent: Intent?
    ) {
        Log.d(
            "KenCheck",
            "Music1Chat removed from Recents; stopping playback."
        )

        playbackRequested = false
        playbackGeneration++
        retryCount = 0

        cancelRetry()

        if (::player.isInitialized) {
            player.stop()
            player.clearMediaItems()
        }

        if (::appPreferences.isInitialized) {
            appPreferences.saveWasPlaying(
                false
            )
        }

        stopSelf()
    }

    private fun scheduleNetworkRetry() {
        if (!playbackRequested) {
            return
        }

        retryCount++

        val retryNumber =
            retryCount

        val retryDelay =
            when (retryNumber) {
                1 -> 2_000L
                2 -> 4_000L
                3 -> 6_000L
                else -> BACKGROUND_RETRY_DELAY_MS
            }

        val scheduledGeneration =
            playbackGeneration

        val scheduledMediaId =
            player.currentMediaItem
                ?.mediaId
                .orEmpty()

        Log.d(
            "KenCheck",
            "Service scheduling retry $retryNumber for " +
                    "${currentStationName()} in ${retryDelay}ms."
        )

        cancelRetry()

        retryJob =
            playbackScope.launch {
                delay(retryDelay)

                val mediaItemIsStillCurrent =
                    player.currentMediaItem
                        ?.mediaId
                        .orEmpty() ==
                            scheduledMediaId

                val requestIsStillCurrent =
                    playbackGeneration ==
                            scheduledGeneration

                if (
                    playbackRequested &&
                    mediaItemIsStillCurrent &&
                    requestIsStillCurrent
                ) {
                    Log.d(
                        "KenCheck",
                        "Service starting retry $retryNumber for " +
                                currentStationName()
                    )

                    player.prepare()
                    player.play()
                }
            }
    }

    private fun cancelRetry() {
        retryJob?.cancel()
        retryJob = null
    }

    private fun isTemporaryNetworkFailure(
        error: PlaybackException
    ): Boolean {
        return error.errorCode ==
                PlaybackException
                    .ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode ==
                PlaybackException
                    .ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
    }

    private fun currentStationName(): String {
        return player.currentMediaItem
            ?.mediaMetadata
            ?.title
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: "current station"
    }

    override fun onDestroy() {
        Log.d(
            "KenCheck",
            "PlaybackService destroyed"
        )

        playbackRequested = false
        playbackGeneration++

        cancelRetry()
        playbackScope.cancel()

        mediaSession?.run {
            player.release()
            release()
        }

        mediaSession = null

        super.onDestroy()
    }

    companion object {
        private const val BACKGROUND_RETRY_DELAY_MS =
            30_000L
    }
}