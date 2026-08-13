package com.coppersmith.music1chat.playback

// Music1Chat coordinated release
// File: PlaybackService.kt
// Release: 2026-08-06 v06
// Coordinated with RideLogger diagnostics and Assistant transport controls.

import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
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
import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.cast.CastPlayer
import com.coppersmith.music1chat.cast.CastManager
import com.google.android.gms.cast.framework.CastContext
import android.os.Looper
import androidx.media3.common.MimeTypes


@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var assistantPlayer: AssistantCommandPlayer
    private var castPlayer: CastPlayer? = null
    private lateinit var castManager: CastManager
    private lateinit var appPreferences: AppPreferences

    private val currentPlayer: Player
        get() = mediaSession?.player ?: exoPlayer

    private val playbackScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var retryJob: Job? = null
    private var bufferingWatchdogJob: Job? = null
    private var recoveryJob: Job? = null
    private var retryCount = 0
    private var playbackGeneration = 0L
    private var playbackRequested = false
    private var currentItemHasPlayed = false
    private var bufferingReconnectAttempted = false
    private var lastAttemptedMediaItems: List<MediaItem> = emptyList()

    private fun beginPlaybackAttempt() {
        playbackGeneration++
        retryCount = 0
        currentItemHasPlayed = false
        bufferingReconnectAttempted = false
        cancelRetry()
        cancelBufferingWatchdog()
    }

    /**
     * Advertises standard next/previous transport commands to system media
     * controllers (including Google Assistant/Gemini) and maps them to the
     * same command bus already used by Bluetooth controls.
     *
     * Play, pause and stop continue to be handled normally by ExoPlayer.
     */
    private inner class AssistantCommandPlayer(
        player: Player
    ) : ForwardingPlayer(player) {

        override fun getAvailableCommands(): Player.Commands {
            return super.getAvailableCommands()
                .buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()
        }

        override fun isCommandAvailable(command: Int): Boolean {
            return when (command) {
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
                else -> super.isCommandAvailable(command)
            }
        }

        override fun hasNextMediaItem(): Boolean = true

        override fun hasPreviousMediaItem(): Boolean = true

        override fun seekToNext() {
            RideLogger.log("ASSISTANT_COMMAND command=NEXT_STATION source=seekToNext")
            MediaButtonCommandBus.send(MediaButtonCommand.NEXT_STATION)
        }

        override fun seekToNextMediaItem() {
            RideLogger.log("ASSISTANT_COMMAND command=NEXT_STATION source=seekToNextMediaItem")
            MediaButtonCommandBus.send(MediaButtonCommand.NEXT_STATION)
        }

        override fun seekToPrevious() {
            val command =
                if (appPreferences.loadVoicePreviousMeansNextCategory()) {
                    MediaButtonCommand.NEXT_CATEGORY
                } else {
                    MediaButtonCommand.PREVIOUS_STATION
                }

            RideLogger.log(
                "ASSISTANT_COMMAND command=$command source=seekToPrevious"
            )
            MediaButtonCommandBus.send(command)
        }

        override fun seekToPreviousMediaItem() {
            val command =
                if (appPreferences.loadVoicePreviousMeansNextCategory()) {
                    MediaButtonCommand.NEXT_CATEGORY
                } else {
                    MediaButtonCommand.PREVIOUS_STATION
                }

            RideLogger.log(
                "ASSISTANT_COMMAND command=$command source=seekToPreviousMediaItem"
            )
            MediaButtonCommandBus.send(command)
        }
    }

    private val mediaSessionCallback =
        object : MediaSession.Callback {
            override fun onMediaButtonEvent(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                intent: Intent
            ): Boolean {
                val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false
                if (keyEvent.action != KeyEvent.ACTION_DOWN || keyEvent.repeatCount != 0) return true

                val command = when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    KeyEvent.KEYCODE_MEDIA_PLAY,
                    KeyEvent.KEYCODE_MEDIA_PAUSE,
                    KeyEvent.KEYCODE_MEDIA_STOP -> MediaButtonCommand.TOGGLE_PLAYBACK
                    KeyEvent.KEYCODE_MEDIA_NEXT -> MediaButtonCommand.NEXT_STATION
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MediaButtonCommand.NEXT_CATEGORY
                    else -> null
                }

                if (command == null) return false
                MediaButtonCommandBus.send(command)
                return true
            }
        }

    private val playerListener = object : Player.Listener {

        override fun onEvents(player: Player, events: Player.Events) {
            if (player === castPlayer) {
                if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) || events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                    val isActuallyPlaying = player.isPlaying
                    val isBuffering = player.playbackState == Player.STATE_BUFFERING

                    if (!isActuallyPlaying && !isBuffering && playbackRequested) {
                        scheduleRecovery()
                    } else {
                        recoveryJob?.cancel()
                    }
                }

                if (events.contains(Player.EVENT_PLAYER_ERROR)) {
                    returnToLocalPlayer()
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateName = when (playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> playbackState.toString()
            }

            RideLogger.log("PLAYER_STATE state=$stateName station='${currentStationName()}' isRemote=${currentPlayer === castPlayer}")

            if (playbackState == Player.STATE_BUFFERING && playbackRequested && !bufferingReconnectAttempted) {
                scheduleBufferingWatchdog()
            } else if (playbackState != Player.STATE_BUFFERING) {
                cancelBufferingWatchdog()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            beginPlaybackAttempt()
            RideLogger.log("PLAYER_TRANSITION reason=$reason station='${currentStationName()}' generation=$playbackGeneration")
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            playbackRequested = playWhenReady
            RideLogger.log("PLAYER_PLAY_WHEN_READY value=$playWhenReady reason=$reason station='${currentStationName()}'")
            if (!playWhenReady) {
                playbackGeneration++
                retryCount = 0
                currentItemHasPlayed = false
                cancelRetry()
                cancelBufferingWatchdog()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            RideLogger.log("PLAYER_IS_PLAYING value=$isPlaying station='${currentStationName()}' generation=$playbackGeneration")
            if (isPlaying) {
                retryCount = 0
                currentItemHasPlayed = true
                bufferingReconnectAttempted = false
                cancelRetry()
                cancelBufferingWatchdog()
            }
        }

        override fun onMetadata(metadata: Metadata) {
            for (index in 0 until metadata.length()) {
                val entry = metadata[index]
                if (entry !is IcyInfo) continue

                val streamTitle = entry.title?.trim().orEmpty()
                if (streamTitle.isBlank()) continue

                val separatorIndex = streamTitle.indexOf(" - ")
                val artist = if (separatorIndex > 0) streamTitle.substring(0, separatorIndex).trim() else ""
                val title = if (separatorIndex > 0) streamTitle.substring(separatorIndex + 3).trim() else streamTitle

                val currentItem = currentPlayer.currentMediaItem ?: continue
                val existingMetadata = currentItem.mediaMetadata

                if (existingMetadata.title?.toString() == title && existingMetadata.artist?.toString() == artist) continue

                val updatedMetadata = existingMetadata.buildUpon()
                        .setTitle(title)
                        .setArtist(artist)
                        .setSubtitle(existingMetadata.station?.toString() ?: existingMetadata.title?.toString())
                        .setStation(existingMetadata.station ?: existingMetadata.title)
                        .setArtworkUri(existingMetadata.artworkUri)
                        .build()

                val updatedItem = currentItem.buildUpon()
                        .setMediaMetadata(updatedMetadata)
                        .build()
                
                // Force an update to the current player (TV or Phone)
                currentPlayer.replaceMediaItem(currentPlayer.currentMediaItemIndex, updatedItem)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            RideLogger.log("PLAYER_ERROR code='${error.errorCodeName}' message='${error.message.orEmpty()}' station='${currentStationName()}'")
            if (currentPlayer === castPlayer) {
                returnToLocalPlayer()
                return
            }
            if (isTemporaryNetworkFailure(error) && playbackRequested) {
                scheduleNetworkRetry()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        appPreferences = AppPreferences(applicationContext)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setConnectTimeoutMs(HTTP_CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(HTTP_READ_TIMEOUT_MS)
            .setAllowCrossProtocolRedirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                setAudioAttributes(audioAttributes, true)
                setWakeMode(C.WAKE_MODE_NETWORK)
            }

        exoPlayer.addListener(playerListener)

        assistantPlayer = AssistantCommandPlayer(exoPlayer)

        mediaSession = MediaSession.Builder(this, assistantPlayer)
            .setId("Music1ChatSession")
            .setCallback(mediaSessionCallback)
            .build()

        playbackScope.launch {
            try {
                CastContext.getSharedInstance(applicationContext)
                castPlayer = CastPlayer.Builder(applicationContext).build()
                castPlayer?.addListener(playerListener)

                castManager = CastManager(applicationContext) { _, isConnected ->
                    if (isConnected) beginCastHandoff() else returnToLocalPlayer()
                }
                castManager.register()
            } catch (e: Exception) {
                Log.e("PlaybackService", "Cast init failed", e)
            }
        }
    }

    private fun beginCastHandoff() {
        val remotePlayer = castPlayer ?: return
        RideLogger.log("CAST_STARTING station='${currentStationName()}'")

        lastAttemptedMediaItems = copyMediaItems(exoPlayer)
        if (lastAttemptedMediaItems.isEmpty()) return

        val currentIndex = exoPlayer.currentMediaItemIndex.coerceIn(0, lastAttemptedMediaItems.lastIndex)
        val currentPosition = exoPlayer.currentPosition
        val shouldPlay = exoPlayer.playWhenReady || exoPlayer.isPlaying

        try {
            exoPlayer.stop()
            mediaSession?.player = remotePlayer

            remotePlayer.stop()
            remotePlayer.clearMediaItems()
            remotePlayer.setMediaItems(lastAttemptedMediaItems, currentIndex, currentPosition)
            remotePlayer.prepare()

            if (shouldPlay) {
                remotePlayer.play()
            }
            RideLogger.log("CAST_SWITCH_COMPLETE station='${currentStationName()}'")

        } catch (error: Exception) {
            RideLogger.log("CAST_HANDOFF_EXCEPTION message='${error.message}'")
            returnToLocalPlayer()
        }
    }

    private fun scheduleRecovery() {
        recoveryJob?.cancel()
        recoveryJob = playbackScope.launch {
            delay(5000)
            if (currentPlayer === castPlayer && !castPlayer!!.isPlaying && playbackRequested) {
                RideLogger.log("CAST_RECOVERY_TRIGGERED reason='silence timeout'")
                returnToLocalPlayer()
            }
        }
    }

    private fun returnToLocalPlayer() {
        recoveryJob?.cancel()

        if (::castManager.isInitialized) {
            castManager.stopCasting()
        }

        val remotePlayer = castPlayer
        mediaSession?.player = assistantPlayer

        val itemsToRestore = if (remotePlayer != null && remotePlayer.mediaItemCount > 0) {
            copyMediaItems(remotePlayer)
        } else {
            lastAttemptedMediaItems
        }

        val shouldPlay = playbackRequested || (remotePlayer?.isPlaying ?: false)
        val currentIndex = remotePlayer?.currentMediaItemIndex?.coerceIn(0, itemsToRestore.size - 1) ?: 0
        val currentPosition = remotePlayer?.currentPosition ?: 0

        safelyResetCastPlayer(remotePlayer)

        beginPlaybackAttempt()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        if (itemsToRestore.isNotEmpty()) {
            exoPlayer.setMediaItems(itemsToRestore, currentIndex, currentPosition)
            exoPlayer.prepare()
            if (shouldPlay) exoPlayer.play()
        }
        RideLogger.log("CAST_RESTORED_LOCAL station='${currentStationName()}'")
    }

    private fun copyMediaItems(sourcePlayer: Player): List<MediaItem> {
        return buildList {
            for (i in 0 until sourcePlayer.mediaItemCount) {
                val item = sourcePlayer.getMediaItemAt(i)
                val uri = item.localConfiguration?.uri?.toString().orEmpty()
                
                // Ensure we carry over the full metadata (title, subtitle, artist, etc)
                // so the TV receiver can display and scroll them correctly.
                val metadata = item.mediaMetadata.buildUpon()
                    .setIsPlayable(true)
                    .build()

                add(item.buildUpon()
                    .setMimeType(inferMimeType(uri))
                    .setMediaMetadata(metadata)
                    .build())
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun inferMimeType(url: String): String {
        val cleanUrl = url.lowercase()
        return when {
            cleanUrl.contains(".m3u8") || cleanUrl.contains("m3u") -> MimeTypes.APPLICATION_M3U8
            cleanUrl.contains(".mp3") -> MimeTypes.AUDIO_MPEG
            cleanUrl.contains(".aac") -> MimeTypes.AUDIO_AAC
            else -> MimeTypes.AUDIO_MPEG // Default to MPEG
        }
    }

    private fun safelyResetCastPlayer(remotePlayer: CastPlayer?) {
        try { remotePlayer?.stop() } catch (_: Exception) {}
        try { remotePlayer?.clearMediaItems() } catch (_: Exception) {}
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        playbackRequested = false
        RideLogger.log("SERVICE_TASK_REMOVED")
        if (::exoPlayer.isInitialized) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
        if (::castManager.isInitialized) castManager.stopCasting()
        castPlayer?.stop()
        stopSelf()
    }

    private fun scheduleNetworkRetry() {
        if (!playbackRequested) return
        retryCount++
        val delayMs = if (retryCount == 1) 2000L else if (retryCount == 2) 4000L else 30000L
        cancelRetry()
        retryJob = playbackScope.launch {
            delay(delayMs)
            if (playbackRequested) {
                currentPlayer.prepare()
                currentPlayer.play()
            }
        }
    }

    private fun scheduleBufferingWatchdog() {
        cancelBufferingWatchdog()
        bufferingWatchdogJob = playbackScope.launch {
            delay(15000)
            if (playbackRequested && currentPlayer.playbackState == Player.STATE_BUFFERING) {
                bufferingReconnectAttempted = true
                RideLogger.log("AUTO_RECONNECT station='${currentStationName()}'")
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

    private fun isTemporaryNetworkFailure(error: PlaybackException): Boolean {
        return error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
    }

    private fun currentStationName(): String {
        val metadata = currentPlayer.currentMediaItem?.mediaMetadata
        return metadata?.station?.toString()?.takeIf { it.isNotBlank() }
            ?: metadata?.title?.toString()?.takeIf { it.isNotBlank() }
            ?: "current station"
    }

    override fun onDestroy() {
        playbackRequested = false
        RideLogger.log("SERVICE_DESTROYED")
        cancelRetry()
        cancelBufferingWatchdog()
        playbackScope.cancel()
        if (::castManager.isInitialized) castManager.unregister()
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
    }
}