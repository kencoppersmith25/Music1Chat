package com.coppersmith.music1chat

// Music1Chat coordinated release
// File: RadioPlayer.kt
// Release: 2026-07-18 v01
// DROP-IN REPLACEMENT
// Change: captures live Media3 title/artist metadata for the now-playing card.
// Matched file: MainScreen.kt 2026-07-18 v01

// HARD STARTUP WATCHDOG FIX V1

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.coppersmith.music1chat.models.Station
import com.coppersmith.music1chat.persistence.AppPreferences
import com.coppersmith.music1chat.playback.PlaybackService
import com.coppersmith.music1chat.resolver.ResolutionResult
import com.coppersmith.music1chat.resolver.StreamResolver
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RadioPlayer(
    context: Context,
    private val streamResolver: StreamResolver = StreamResolver()
) {
    enum class PlaybackSource {
        NAVIGATION,
        SEARCH
    }

    private data class PlaybackRequest(
        val generation: Long,
        val station: Station,
        val source: PlaybackSource,
        val mediaId: String
    )

    private val applicationContext =
        context.applicationContext

    private val appPreferences =
        AppPreferences(
            applicationContext
        )

    private val resolverScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

    private val stationsBeingResolved =
        mutableSetOf<Long>()

    private var startupWatchdogJob: Job? = null

    private var nextGeneration = 0L

    private var activeRequest: PlaybackRequest? = null

    private var pendingRequest: PlaybackRequest? = null
    private var playbackStartTime = 0L
    private var playbackNavGeneration = 0L
    private var controller: MediaController? = null

    private val controllerFuture: ListenableFuture<MediaController>

    var onStationFailed: ((Station) -> Unit)? = null

    var onStationResolved: ((Station, ResolutionResult) -> Unit)? = null

    var isPlaying by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var nowPlayingTitle by mutableStateOf("")
        private set

    var nowPlayingArtist by mutableStateOf("")
        private set

    var nowPlayingText by mutableStateOf("")
        private set

    val activeStation: Station?
        get() = activeRequest?.station

    val activePlaybackSource: PlaybackSource?
        get() = activeRequest?.source

    init {
        val sessionToken = SessionToken(
            applicationContext,
            ComponentName(
                applicationContext,
                PlaybackService::class.java
            )
        )

        controllerFuture = MediaController.Builder(
            applicationContext,
            sessionToken
        ).buildAsync()

        controllerFuture.addListener(
            {
                try {
                    val connectedController = controllerFuture.get()
                    controller = connectedController
                    connectedController.addListener(playerListener)
                    isPlaying = connectedController.isPlaying

                    Log.d(
                        "KenCheck",
                        "MediaController connected to PlaybackService"
                    )

                    pendingRequest?.let { request ->
                        if (requestIsStillActive(request)) {
                            pendingRequest = null
                            startPlayback(
                                request = request,
                                mediaController = connectedController
                            )
                        }
                    }
                } catch (exception: Exception) {
                    Log.e(
                        "KenCheck",
                        "Unable to connect to PlaybackService",
                        exception
                    )
                    errorMessage = "Unable to connect to the playback service."
                }
            },
            ContextCompat.getMainExecutor(applicationContext)
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val request = matchingActiveRequest() ?: return

            val title = mediaMetadata.title?.toString()?.trim().orEmpty()
            val artist = mediaMetadata.artist?.toString()?.trim().orEmpty()

            nowPlayingTitle = title.takeUnless {
                it.equals(request.station.name, ignoreCase = true)
            }.orEmpty()

            nowPlayingArtist = artist
            nowPlayingText = formatNowPlayingText(
                stationName = request.station.name,
                title = title,
                artist = artist
            )

            Log.d(
                "KenCheck",
                "Now-playing metadata for ${request.station.name}: " +
                        "title=$title, artist=$artist, display=$nowPlayingText"
            )

            if (nowPlayingTitle.isNotBlank() || artist.isNotBlank() || title.isNotBlank()) {
                Log.d(
                    "M1Metadata",
                    "PlaybackService received: title='$title', artist='$artist', displayTitle='$nowPlayingText'"
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val request = activeRequest ?: return
            val transitionedMediaId = mediaItem?.mediaId.orEmpty()

            if (transitionedMediaId.isNotBlank() && transitionedMediaId != request.mediaId) {
                Log.d(
                    "KenCheck",
                    "Ignoring transition for stale media item $transitionedMediaId; active=${request.mediaId}"
                )
                return
            }

            Log.d(
                "KenCheck",
                "Controller transitioned to ${request.station.name}: " +
                        "generation=${request.generation}, source=${request.source}, reason=$reason"
            )
        }

        override fun onIsPlayingChanged(playing: Boolean) {
            val request = matchingActiveRequest() ?: run {
                if (playing) {
                    Log.d(
                        "KenCheck",
                        "Ignoring stale isPlaying=true callback."
                    )
                }
                return
            }

            isPlaying = playing

            if (playing) {
                if (request.generation == playbackNavGeneration) {
                    val elapsed =
                        System.currentTimeMillis() - playbackStartTime

                    Log.d(
                        "KenRide",
                        "[${request.generation}] PLAYING ${request.station.name} in ${elapsed} ms"
                    )
                }

                cancelStartupWatchdog()
                errorMessage = null
                request.station.failedThisSession = false

                Log.d(
                    "KenCheck",
                    "Controller reports playback active: ${request.station.name}, " +
                            "generation=${request.generation}, source=${request.source}"
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val request = matchingActiveRequest()
            if (request == null) {
                Log.w(
                    "KenCheck",
                    "Ignoring stale playback error: ${error.errorCodeName}: ${error.message}"
                )
                return
            }

            Log.e(
                "KenCheck",
                "Controller playback error for ${request.station.name}: " +
                        "generation=${request.generation}, source=${request.source}, " +
                        "${error.errorCodeName}: ${error.message}",
                error
            )

            isPlaying = false
            val failedStation = request.station

            if (isTemporaryNetworkFailure(error)) {
                failedStation.failedThisSession = false
                errorMessage = "Network connection lost while playing ${failedStation.name}. " +
                        "Music1Chat will keep trying in the background."
                return
            }

            errorMessage = "Unable to play ${failedStation.name}. ${error.errorCodeName}: " +
                    (error.message ?: "Unknown playback error.")

            failedStation.failedThisSession = true

            if (!requestIsStillActive(request)) {
                Log.w(
                    "KenCheck",
                    "Playback request changed while handling error; not failing ${failedStation.name}."
                )
                return
            }

            onStationFailed?.invoke(failedStation)
            resolveFailedStationInBackground(failedStation)
        }
    }

    fun play(station: Station) {
        play(station = station, source = PlaybackSource.NAVIGATION)
    }

    fun play(station: Station, source: PlaybackSource) {
        val request = createPlaybackRequest(station = station, source = source)
        cancelStartupWatchdog()

        activeRequest = request
        pendingRequest = null
        errorMessage = null
        nowPlayingTitle = ""
        nowPlayingArtist = ""
        nowPlayingText = ""
        isPlaying = false

        val connectedController = controller
        if (connectedController == null) {
            pendingRequest = request
            Log.d(
                "KenCheck",
                "Playback queued while controller connects: ${station.name}, " +
                        "generation=${request.generation}, source=${request.source}"
            )
            return
        }

        startPlayback(request = request, mediaController = connectedController)
    }

    private fun createPlaybackRequest(station: Station, source: PlaybackSource): PlaybackRequest {
        nextGeneration++
        val generation = nextGeneration
        return PlaybackRequest(
            generation = generation,
            station = station,
            source = source,
            mediaId = buildMediaId(stationId = station.id, generation = generation)
        )
    }

    private fun startPlayback(request: PlaybackRequest, mediaController: MediaController) {
        if (!requestIsStillActive(request)) {
            Log.d(
                "KenCheck",
                "Not starting stale playback request for ${request.station.name}, generation=${request.generation}"
            )
            return
        }

        val station = request.station
        val playbackUrl = preferredPlaybackUrl(station)

        Log.d(
            "KenCheck",
            "Playing ${station.name} through PlaybackService: " +
                    "generation=${request.generation}, source=${request.source}, mediaId=${request.mediaId}, " +
                    "verified=${station.streamVerified}, resolvedUrl=${station.resolvedStreamUrl}, playbackUrl=$playbackUrl"
        )

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setSubtitle(station.genre) // Add a subtitle for the TV
            .setGenre(station.genre)
            .setStation(station.name)
            .setArtworkUri(
                if (station.logoUrl.isNotBlank()) {
                    Uri.parse(station.logoUrl)
                } else null
            )
            .setIsPlayable(true)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(request.mediaId)
            .setUri(playbackUrl)
            .setMediaMetadata(mediaMetadata)
            .build()

        playbackStartTime = System.currentTimeMillis()
        playbackNavGeneration = request.generation

        Log.d(
            "KenRide",
            "[${request.generation}] START ${station.name}"
        )

        mediaController.stop()
        mediaController.clearMediaItems()
        mediaController.setMediaItem(mediaItem)
        mediaController.prepare()
        mediaController.play()

        startStartupWatchdog(request = request, mediaController = mediaController)
    }

    private fun startStartupWatchdog(request: PlaybackRequest, mediaController: MediaController) {
        cancelStartupWatchdog()
        startupWatchdogJob = resolverScope.launch {
            delay(STARTUP_TIMEOUT_MILLISECONDS)
            withContext(Dispatchers.Main) {
                if (!requestIsStillActive(request) || isPlaying) {
                    return@withContext
                }

                val currentMediaId = mediaController.currentMediaItem?.mediaId.orEmpty()
                if (currentMediaId != request.mediaId) {
                    return@withContext
                }

                val failedStation = request.station
                Log.w(
                    "KenCheck",
                    "Startup watchdog timed out for ${failedStation.name} after ${STARTUP_TIMEOUT_MILLISECONDS}ms."
                )

                mediaController.stop()
                mediaController.clearMediaItems()

                failedStation.failedThisSession = true
                errorMessage = "Unable to start ${failedStation.name}. Trying the next station."

                if (!requestIsStillActive(request)) {
                    return@withContext
                }

                onStationFailed?.invoke(failedStation)
                resolveFailedStationInBackground(failedStation)
            }
        }
    }

    private fun cancelStartupWatchdog() {
        startupWatchdogJob?.cancel()
        startupWatchdogJob = null
    }

    private fun matchingActiveRequest(): PlaybackRequest? {
        val request = activeRequest ?: return null
        val currentMediaId = controller?.currentMediaItem?.mediaId.orEmpty()
        return request.takeIf { currentMediaId == request.mediaId }
    }

    private fun requestIsStillActive(request: PlaybackRequest): Boolean {
        return activeRequest?.generation == request.generation &&
                activeRequest?.mediaId == request.mediaId
    }

    private fun formatNowPlayingText(stationName: String, title: String, artist: String): String {
        val cleanTitle = title.takeUnless {
            it.equals(stationName, ignoreCase = true)
        }.orEmpty()

        return when {
            artist.isNotBlank() && cleanTitle.isNotBlank() -> "$artist — $cleanTitle"
            cleanTitle.isNotBlank() -> cleanTitle
            artist.isNotBlank() -> artist
            else -> ""
        }
    }

    private fun buildMediaId(stationId: Long, generation: Long): String {
        return "$stationId:$generation"
    }

    private fun preferredPlaybackUrl(station: Station): String {
        val savedResolvedUrl = station.resolvedStreamUrl.trim()
        return if (station.streamVerified && savedResolvedUrl.isNotBlank()) {
            savedResolvedUrl
        } else {
            station.streamUrl
        }
    }

    private fun isTemporaryNetworkFailure(error: PlaybackException): Boolean {
        return error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
    }

    private fun resolveFailedStationInBackground(station: Station) {
        synchronized(stationsBeingResolved) {
            if (!stationsBeingResolved.add(station.id)) {
                return
            }
        }

        resolverScope.launch {
            try {
                val result = streamResolver.resolve(station)
                Log.d(
                    "KenCheck",
                    "Resolver result for ${station.name}: success=${result.success}, " +
                            "verified=${result.verified}, resolvedUrl=${result.resolvedUrl}, error=${result.errorMessage}"
                )

                val resolvedUrl = result.resolvedUrl?.trim().orEmpty()
                if (result.success && result.verified && resolvedUrl.isNotBlank()) {
                    station.resolvedStreamUrl = resolvedUrl
                    station.streamVerified = true
                    station.lastVerified = System.currentTimeMillis()
                    station.failedThisSession = false
                    appPreferences.saveStationRepair(station)

                    Log.d(
                        "KenCheck",
                        "Saved repair for ${station.name}: resolvedUrl=${station.resolvedStreamUrl}, " +
                                "verified=${station.streamVerified}, lastVerified=${station.lastVerified}"
                    )

                    withContext(Dispatchers.Main) {
                        onStationResolved?.invoke(station, result)
                    }
                }
            } finally {
                synchronized(stationsBeingResolved) {
                    stationsBeingResolved.remove(station.id)
                }
            }
        }
    }

    fun stop() {
        cancelStartupWatchdog()
        nextGeneration++

        activeRequest = null
        pendingRequest = null
        errorMessage = null

        controller?.run {
            stop()
            clearMediaItems()
        }

        isPlaying = false
        Log.d("KenCheck", "RadioPlayer stopped; generation invalidated.")
    }

    fun release() {
        cancelStartupWatchdog()
        nextGeneration++

        pendingRequest = null
        activeRequest = null
        onStationFailed = null
        onStationResolved = null

        resolverScope.cancel()
        controller?.removeListener(playerListener)
        controller = null

        MediaController.releaseFuture(controllerFuture)
    }

    companion object {
        private const val STARTUP_TIMEOUT_MILLISECONDS = 3_500L
        const val TEST_STREAM_URL = "https://ice5.somafm.com/groovesalad-128-mp3"
    }
}
