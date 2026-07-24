package com.coppersmith.music1chat.playback

// Music1Chat coordinated release
// File: PlaybackService.kt
// Release: 2026-07-23 v03
// DROP-IN REPLACEMENT
// Change: automatically reconnects the same station when previously successful
// playback remains stuck in BUFFERING for 15 seconds.

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
import com.coppersmith.music1chat.diagnostics.RideLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.cast.CastPlayer
import com.coppersmith.music1chat.cast.CastManager
import com.google.android.gms.cast.framework.CastContext



@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer
    private var castPlayer: CastPlayer? = null
    private lateinit var castManager: CastManager
    private lateinit var appPreferences: AppPreferences

    private val currentPlayer: Player
        get() = mediaSession?.player ?: exoPlayer

    private val playbackScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var retryJob: Job? = null
    private var bufferingWatchdogJob: Job? = null
    private var retryCount = 0
    private var playbackGeneration = 0L
    private var playbackRequested = false
    private var currentItemHasPlayed = false
    private var bufferingReconnectAttempted = false

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

    private val playerListener = object : Player.Listener {

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int
        ) {
            playbackGeneration++
            retryCount = 0
            currentItemHasPlayed = false
            bufferingReconnectAttempted = false
            cancelRetry()
            cancelBufferingWatchdog()

            val stationName =
                mediaItem
                    ?.mediaMetadata
                    ?.station
                    ?.toString()
                    .orEmpty()

            val mediaId =
                mediaItem
                    ?.mediaId
                    .orEmpty()

            val uri =
                mediaItem
                    ?.localConfiguration
                    ?.uri
                    ?.toString()
                    .orEmpty()

            RideLogger.log(
                "PLAYER_TRANSITION " +
                        "reason=$reason " +
                        "station='$stationName' " +
                        "mediaId='$mediaId' " +
                        "uri='$uri' " +
                        "generation=$playbackGeneration"
            )

            Log.d(
                "KenCheck",
                "Player transition reason=$reason " +
                        "station='$stationName' mediaId='$mediaId'"
            )
        }

        override fun onPlayWhenReadyChanged(
            playWhenReady: Boolean,
            reason: Int
        ) {
            playbackRequested = playWhenReady

            RideLogger.log(
                "PLAYER_PLAY_WHEN_READY " +
                        "value=$playWhenReady " +
                        "reason=$reason " +
                        "station='${currentStationName()}' " +
                        "mediaId='${currentPlayer.currentMediaItem?.mediaId.orEmpty()}' " +
                        "state=${currentPlayer.playbackState} " +
                        "generation=$playbackGeneration"
            )

            if (!playWhenReady) {
                playbackGeneration++
                retryCount = 0
                currentItemHasPlayed = false
                bufferingReconnectAttempted = false
                cancelRetry()
                cancelBufferingWatchdog()
            }
        }

        override fun onIsPlayingChanged(
            isPlaying: Boolean
        ) {
            RideLogger.log(
                "PLAYER_IS_PLAYING " +
                        "value=$isPlaying " +
                        "station='${currentStationName()}' " +
                        "mediaId='${currentPlayer.currentMediaItem?.mediaId.orEmpty()}' " +
                        "state=${currentPlayer.playbackState} " +
                        "playWhenReady=${currentPlayer.playWhenReady} " +
                        "generation=$playbackGeneration"
            )

            if (isPlaying) {
                retryCount = 0
                currentItemHasPlayed = true
                bufferingReconnectAttempted = false
                cancelRetry()
                cancelBufferingWatchdog()

                Log.d(
                    "KenCheck",
                    "Playback service active: ${currentStationName()}"
                )
            }
        }

        override fun onPlaybackStateChanged(
            playbackState: Int
        ) {
            val stateName =
                when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> playbackState.toString()
                }

            RideLogger.log(
                "PLAYER_STATE " +
                        "state=$stateName " +
                        "station='${currentStationName()}' " +
                        "mediaId='${currentPlayer.currentMediaItem?.mediaId.orEmpty()}' " +
                        "playWhenReady=${currentPlayer.playWhenReady} " +
                        "isPlaying=${currentPlayer.isPlaying} " +
                        "generation=$playbackGeneration"
            )

            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    if (
                        playbackRequested &&
                        currentItemHasPlayed &&
                        !bufferingReconnectAttempted
                    ) {
                        scheduleBufferingWatchdog()
                    }
                }

                Player.STATE_READY,
                Player.STATE_IDLE,
                Player.STATE_ENDED ->
                    cancelBufferingWatchdog()
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val title =
                mediaMetadata.title
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val artist =
                mediaMetadata.artist
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            Log.d(
                "M1Metadata",
                "PlaybackService metadata changed: " +
                        "title='$title', artist='$artist'"
            )
        }

        override fun onMetadata(metadata: Metadata) {
            for (index in 0 until metadata.length()) {
                val entry = metadata[index]

                if (entry !is IcyInfo) {
                    continue
                }

                val streamTitle =
                    entry.title
                        ?.trim()
                        .orEmpty()

                if (streamTitle.isBlank()) {
                    continue
                }

                val separatorIndex =
                    streamTitle.indexOf(" - ")

                val artist =
                    if (separatorIndex > 0) {
                        streamTitle
                            .substring(0, separatorIndex)
                            .trim()
                    } else {
                        ""
                    }

                val title =
                    if (separatorIndex > 0) {
                        streamTitle
                            .substring(separatorIndex + 3)
                            .trim()
                    } else {
                        streamTitle
                    }

                val currentItem =
                    currentPlayer.currentMediaItem
                        ?: continue

                val existingMetadata =
                    currentItem.mediaMetadata

                /*
                 * Ignore duplicate ICY entries. Many stations transmit the same
                 * metadata block repeatedly.
                 */
                if (
                    existingMetadata.title?.toString() == title &&
                    existingMetadata.artist?.toString() == artist
                ) {
                    continue
                }

                val updatedMetadata =
                    existingMetadata
                        .buildUpon()
                        .setTitle(title)
                        .setArtist(artist)
                        .build()

                val updatedItem =
                    currentItem
                        .buildUpon()
                        .setMediaMetadata(updatedMetadata)
                        .build()

                Log.d(
                    "M1Metadata",
                    "Publishing ICY metadata: " +
                            "artist='$artist', title='$title'"
                )

                currentPlayer.replaceMediaItem(
                    currentPlayer.currentMediaItemIndex,
                    updatedItem
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            RideLogger.log(
                "PLAYER_ERROR " +
                        "station='${currentStationName()}' " +
                        "mediaId='${currentPlayer.currentMediaItem?.mediaId.orEmpty()}' " +
                        "code='${error.errorCodeName}' " +
                        "message='${error.message.orEmpty()}' " +
                        "generation=$playbackGeneration"
            )
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

        exoPlayer =
            ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    setAudioAttributes(audioAttributes, true)
                    setWakeMode(C.WAKE_MODE_NETWORK)
                }

        exoPlayer.addListener(playerListener)

        mediaSession =
            MediaSession.Builder(this, exoPlayer)
                .setCallback(mediaSessionCallback)
                .build()

        try {
            val castContext = CastContext.getSharedInstance(this)
            castPlayer = CastPlayer(castContext)
            castPlayer?.addListener(playerListener)

            castManager = CastManager(this) { session, isConnected ->
                if (isConnected) {
                    switchToPlayer(castPlayer!!)
                } else {
                    switchToPlayer(exoPlayer)
                }
            }
            castManager.register()
        } catch (e: Exception) {
            Log.e("KenCheck", "Cast initialization failed", e)
        }

        Log.d(
            "KenCheck",
            "PlaybackService created with ${HTTP_CONNECT_TIMEOUT_MS}ms " +
                    "connect timeout and ${HTTP_READ_TIMEOUT_MS}ms read timeout."
        )
    }

    private fun switchToPlayer(newPlayer: Player) {
        val oldPlayer = mediaSession?.player
        if (oldPlayer == newPlayer) return

        Log.d("KenCheck", "Switching player to ${if (newPlayer is CastPlayer) "Cast" else "ExoPlayer"}")

        val playWhenReady = oldPlayer?.playWhenReady ?: false
        val mediaItems = mutableListOf<MediaItem>()
        for (i in 0 until (oldPlayer?.mediaItemCount ?: 0)) {
            mediaItems.add(oldPlayer!!.getMediaItemAt(i))
        }
        val currentItemIndex = oldPlayer?.currentMediaItemIndex ?: 0
        val currentPosition = oldPlayer?.currentPosition ?: 0

        oldPlayer?.stop()
        oldPlayer?.clearMediaItems()

        newPlayer.setMediaItems(mediaItems, currentItemIndex, currentPosition)
        newPlayer.playWhenReady = playWhenReady
        newPlayer.prepare()

        mediaSession?.player = newPlayer
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
        cancelBufferingWatchdog()

        if (::exoPlayer.isInitialized) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
        
        castPlayer?.stop()
        castPlayer?.clearMediaItems()

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
            currentPlayer.currentMediaItem?.mediaId.orEmpty()

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
                    currentPlayer.currentMediaItem?.mediaId.orEmpty() ==
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

                    currentPlayer.prepare()
                    currentPlayer.play()
                }
            }
    }

    private fun scheduleBufferingWatchdog() {
        cancelBufferingWatchdog()

        val scheduledGeneration = playbackGeneration
        val scheduledMediaId =
            currentPlayer.currentMediaItem?.mediaId.orEmpty()

        bufferingWatchdogJob =
            playbackScope.launch {
                delay(BUFFERING_RECONNECT_DELAY_MS)

                val mediaItemIsStillCurrent =
                    currentPlayer.currentMediaItem?.mediaId.orEmpty() ==
                            scheduledMediaId

                val requestIsStillCurrent =
                    playbackGeneration == scheduledGeneration

                if (
                    playbackRequested &&
                    currentItemHasPlayed &&
                    !bufferingReconnectAttempted &&
                    mediaItemIsStillCurrent &&
                    requestIsStillCurrent &&
                    currentPlayer.playbackState == Player.STATE_BUFFERING
                ) {
                    bufferingReconnectAttempted = true

                    RideLogger.log(
                        "BUFFERING_TIMEOUT " +
                                "delayMs=$BUFFERING_RECONNECT_DELAY_MS " +
                                "station='${currentStationName()}' " +
                                "mediaId='$scheduledMediaId' " +
                                "generation=$playbackGeneration"
                    )
                    RideLogger.log(
                        "AUTO_RECONNECT " +
                                "station='${currentStationName()}' " +
                                "mediaId='$scheduledMediaId' " +
                                "generation=$playbackGeneration"
                    )

                    Log.d(
                        "KenCheck",
                        "Playback remained buffered for " +
                                "${BUFFERING_RECONNECT_DELAY_MS}ms; " +
                                "reconnecting ${currentStationName()}."
                    )

                    currentPlayer.stop()
                    currentPlayer.prepare()
                    currentPlayer.play()
                }
            }
    }

    private fun cancelBufferingWatchdog() {
        bufferingWatchdogJob?.cancel()
        bufferingWatchdogJob = null
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
        val metadata =
            currentPlayer.currentMediaItem
                ?.mediaMetadata

        return metadata
            ?.station
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: metadata
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
        cancelBufferingWatchdog()
        playbackScope.cancel()

        if (::castManager.isInitialized) {
            castManager.unregister()
        }

        mediaSession?.run {
            exoPlayer.release()
            castPlayer?.release()
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
        private const val BUFFERING_RECONNECT_DELAY_MS = 15_000L
    }
}
