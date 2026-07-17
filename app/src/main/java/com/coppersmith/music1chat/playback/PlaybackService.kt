package com.coppersmith.music1chat.playback

import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
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
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var retryJob: Job? = null
    private var retryCount = 0
    private var playbackGeneration = 0L
    private var playbackRequested = false

    private val mediaSessionCallback =
        object : MediaSession.Callback {

            override fun onMediaButtonEvent(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                intent: Intent
            ): Boolean {
                val keyEvent =
                    intent.getParcelableExtra<KeyEvent>(
                        Intent.EXTRA_KEY_EVENT
                    ) ?: return false

                /*
                 * Execute only once per physical press.
                 * ACTION_UP and held-button repeat events are ignored.
                 */
                if (
                    keyEvent.action != KeyEvent.ACTION_DOWN ||
                    keyEvent.repeatCount != 0
                ) {
                    return true
                }

                val command =
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_PLAY,
                        KeyEvent.KEYCODE_MEDIA_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_STOP ->
                            MediaButtonCommand.TOGGLE_PLAYBACK

                        KeyEvent.KEYCODE_MEDIA_NEXT ->
                            MediaButtonCommand.NEXT_STATION

                        /*
                         * Phase-one trail mapping:
                         * the headset Back/Previous button advances to
                         * the next navigation-enabled category.
                         */
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS ->
                            MediaButtonCommand.NEXT_CATEGORY

                        else -> null
                    }

                if (command == null) {
                    Log.d(
                        "KenCheck",
                        "Unhandled media button keyCode=${keyEvent.keyCode}"
                    )

                    return false
                }

                Log.d(
                    "KenCheck",
                    "Media button keyCode=${keyEvent.keyCode} -> $command"
                )

                MediaButtonCommandBus.send(command)
                return true
            }
        }

    override fun onCreate() {
        super.onCreate()

        appPreferences = AppPreferences(applicationContext)

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

        val httpDataSourceFactory =
            DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .setConnectTimeoutMs(HTTP_CONNECT_TIMEOUT_MS)
                .setReadTimeoutMs(HTTP_READ_TIMEOUT_MS)
                .setAllowCrossProtocolRedirects(true)

        val dataSourceFactory =
            DefaultDataSource.Factory(
                this,
                httpDataSourceFactory
            )

        val mediaSourceFactory =
            DefaultMediaSourceFactory(dataSourceFactory)

        player =
            ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    setAudioAttributes(audioAttributes, true)
                    setWakeMode(C.WAKE_MODE_NETWORK)
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
                    playbackRequested = playWhenReady

                    if (!playWhenReady) {
                        playbackGeneration++
                        retryCount = 0
                        cancelRetry()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        retryCount = 0
                        cancelRetry()

                        Log.d(
                            "KenCheck",
                            "Playback service active: ${currentStationName()}"
                        )
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(
                        "KenCheck",
                        "Playback service error for ${currentStationName()}: " +
                                "${error.errorCodeName}: ${error.message}",
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
            MediaSession.Builder(this, player)
                .setCallback(mediaSessionCallback)
                .build()

        Log.d(
            "KenCheck",
            "PlaybackService created with ${HTTP_CONNECT_TIMEOUT_MS}ms " +
                    "connect timeout and ${HTTP_READ_TIMEOUT_MS}ms read timeout."
        )
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
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
            appPreferences.saveWasPlaying(false)
        }

        stopSelf()
    }

    private fun scheduleNetworkRetry() {
        if (!playbackRequested) {
            return
        }

        retryCount++
        val retryNumber = retryCount

        val retryDelay =
            when (retryNumber) {
                1 -> 2_000L
                2 -> 4_000L
                3 -> 6_000L
                else -> BACKGROUND_RETRY_DELAY_MS
            }

        val scheduledGeneration = playbackGeneration
        val scheduledMediaId =
            player.currentMediaItem?.mediaId.orEmpty()

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
                    player.currentMediaItem?.mediaId.orEmpty() ==
                            scheduledMediaId

                val requestIsStillCurrent =
                    playbackGeneration == scheduledGeneration

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
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode ==
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
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
        Log.d("KenCheck", "PlaybackService destroyed")

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
        private const val USER_AGENT = "Music1Chat/1.0"
        private const val HTTP_CONNECT_TIMEOUT_MS = 4_000
        private const val HTTP_READ_TIMEOUT_MS = 4_000
        private const val BACKGROUND_RETRY_DELAY_MS = 30_000L
    }
}