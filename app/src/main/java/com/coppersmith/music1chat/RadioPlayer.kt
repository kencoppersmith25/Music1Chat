package com.coppersmith.music1chat

// Music1Chat coordinated release
// File: RadioPlayer.kt
// Release: 2026-07-25 v04
// Coordinated with RideLogger diagnostic infrastructure.

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.coppersmith.music1chat.models.Station
import com.coppersmith.music1chat.persistence.AppPreferences
import com.coppersmith.music1chat.playback.PlaybackService
import com.coppersmith.music1chat.diagnostics.RideLogger
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

    private val applicationContext = context.applicationContext
    private val appPreferences = AppPreferences(applicationContext)
    private val resolverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stationsBeingResolved = mutableSetOf<Long>()

    private var startupWatchdogJob: Job? = null
    private var stallWatchdogJob: Job? = null
    private val retryEligibilityJobs = mutableMapOf<Long, Job>()

    private var nextGeneration = 0L
    private var activeRequest: PlaybackRequest? = null
    private var pendingRequest: PlaybackRequest? = null
    private var playbackStartTime = 0L
    private var playbackNavGeneration = 0L
    private var activeRequestHasPlayed = false
    private var controller: MediaController? = null

    private val controllerFuture: ListenableFuture<MediaController>

    var onStationFailed: ((Station) -> Unit)? = null
    var onStationResolved: ((Station, ResolutionResult) -> Unit)? = null

    var isPlaying by mutableStateOf(false)
        private set

    var playbackRequested by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var nowPlayingTitle by mutableStateOf("")
        private set

    var nowPlayingArtist by mutableStateOf("")
        private set

    var nowPlayingArtworkUri by mutableStateOf<Uri?>(null)
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
                    Log.e("RadioPlayer", "Unable to connect to PlaybackService", exception)
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

            nowPlayingArtworkUri =
                mediaMetadata.artworkUri
                    ?: request.station.logoUrl
                        .takeIf { it.isNotBlank() }
                        ?.let(Uri::parse)

            nowPlayingTitle = title.takeUnless {
                it.equals(request.station.name, ignoreCase = true)
            }.orEmpty()

            nowPlayingArtist = artist
            nowPlayingText = formatNowPlayingText(
                stationName = request.station.name,
                title = title,
                artist = artist
            )
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val request = activeRequest ?: return
            val transitionedMediaId = mediaItem?.mediaId.orEmpty()

            if (transitionedMediaId.isNotBlank() && transitionedMediaId != request.mediaId) {
                return
            }
        }

        override fun onIsPlayingChanged(playing: Boolean) {
            val request = matchingActiveRequest() ?: return

            isPlaying = playing

            RideLogger.log(
                "IS_PLAYING_CHANGED " +
                        "playing=$playing " +
                        "state=${controller?.playbackState} " +
                        "requested=$playbackRequested"
            )

            if (playing) {
                playbackRequested = true
                activeRequestHasPlayed = true
                cancelStallWatchdog()
                if (request.generation == playbackNavGeneration) {
                    val elapsed = System.currentTimeMillis() - playbackStartTime
                    RideLogger.log("STATION_PLAYING station='${request.station.name}' elapsed=$elapsed")
                }

                cancelStartupWatchdog()
                errorMessage = null
                request.station.failedThisSession = false
                cancelRetryEligibility(request.station)
            } else if (
                playbackRequested &&
                activeRequestHasPlayed &&
                controller?.playbackState == Player.STATE_BUFFERING
            ) {
                startStallWatchdog(request)
            }
        }

        override fun onPlayWhenReadyChanged(
            playWhenReady: Boolean,
            reason: Int
        ) {
            val request = matchingActiveRequest() ?: return

            playbackRequested = playWhenReady

            RideLogger.log(
                "PLAYBACK_REQUEST_SYNC " +
                        "playWhenReady=$playWhenReady " +
                        "reason=$reason " +
                        "station='${request.station.name}'"
            )
        }

        override fun onPlayerError(error: PlaybackException) {
            val request = matchingActiveRequest() ?: return

            isPlaying = false
            cancelStallWatchdog()
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
            scheduleRetryEligibility(failedStation)

            if (!requestIsStillActive(request)) {
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
        cancelStallWatchdog()

        activeRequest = request
        pendingRequest = null
        errorMessage = null
        nowPlayingTitle = ""
        nowPlayingArtist = ""
        nowPlayingArtworkUri = null
        nowPlayingText = ""
        isPlaying = false
        playbackRequested = true
        activeRequestHasPlayed = false

        val connectedController = controller
        if (connectedController == null) {
            pendingRequest = request
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
            return
        }

        val station = request.station
        val playbackUrl = preferredPlaybackUrl(station)

        // Build a detailed subtitle for the TV receiver
        val detailedSubtitle = listOf(station.genre, station.city, station.country)
            .filter { it.isNotBlank() }
            .joinToString(" • ")

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setSubtitle(detailedSubtitle)
            .setArtist(station.callLetters)
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
            .setMimeType(inferMimeType(playbackUrl))
            .setMediaMetadata(mediaMetadata)
            .build()

        playbackStartTime = System.currentTimeMillis()
        playbackNavGeneration = request.generation

        mediaController.stop()
        mediaController.clearMediaItems()
        mediaController.setMediaItem(mediaItem)
        mediaController.prepare()
        mediaController.play()

        startStartupWatchdog(request = request, mediaController = mediaController)
    }

    private fun startStallWatchdog(request: PlaybackRequest) {
        if (stallWatchdogJob?.isActive == true) {
            return
        }

        stallWatchdogJob = resolverScope.launch {
            delay(STALL_FAILURE_TIMEOUT_MILLISECONDS)
            withContext(Dispatchers.Main) {
                val mediaController = controller ?: return@withContext

                if (
                    !requestIsStillActive(request) ||
                    !playbackRequested ||
                    !activeRequestHasPlayed ||
                    mediaController.isPlaying ||
                    mediaController.playbackState != Player.STATE_BUFFERING
                ) {
                    return@withContext
                }

                val failedStation = request.station

                RideLogger.log(
                    "STATION_STALL_FAILED " +
                            "delayMs=$STALL_FAILURE_TIMEOUT_MILLISECONDS " +
                            "station='${failedStation.name}' " +
                            "mediaId='${request.mediaId}' " +
                            "generation=${request.generation}"
                )

                mediaController.stop()
                mediaController.clearMediaItems()

                playbackRequested = false
                isPlaying = false
                failedStation.failedThisSession = true
                scheduleRetryEligibility(failedStation)
                errorMessage = "${failedStation.name} stalled. Trying the next station."

                if (!requestIsStillActive(request)) {
                    return@withContext
                }

                onStationFailed?.invoke(failedStation)
                resolveFailedStationInBackground(failedStation)
            }
        }
    }

    private fun cancelStallWatchdog() {
        stallWatchdogJob?.cancel()
        stallWatchdogJob = null
    }

    private fun startStartupWatchdog(request: PlaybackRequest, mediaController: MediaController) {
        cancelStartupWatchdog()
        startupWatchdogJob = resolverScope.launch {
            delay(STARTUP_TIMEOUT_MILLISECONDS)
            withContext(Dispatchers.Main) {
                if (
                    !requestIsStillActive(request) ||
                    !playbackRequested ||
                    isPlaying ||
                    activeRequestHasPlayed
                ) {
                    return@withContext
                }

                val failedStation = request.station
                RideLogger.log(
                    "STATION_START_FAILED " +
                            "delayMs=$STARTUP_TIMEOUT_MILLISECONDS " +
                            "station='${failedStation.name}' " +
                            "mediaId='${request.mediaId}' " +
                            "generation=${request.generation} " +
                            "controllerMediaId='${mediaController.currentMediaItem?.mediaId.orEmpty()}' " +
                            "state=${mediaController.playbackState}"
                )

                mediaController.stop()
                mediaController.clearMediaItems()

                failedStation.failedThisSession = true
                scheduleRetryEligibility(failedStation)
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

    @OptIn(UnstableApi::class)
    private fun inferMimeType(url: String): String {
        val cleanUrl = url.lowercase()
        return when {
            cleanUrl.contains(".m3u8") || cleanUrl.contains("m3u") -> MimeTypes.APPLICATION_M3U8
            cleanUrl.contains(".mp3") -> MimeTypes.AUDIO_MPEG
            cleanUrl.contains(".aac") -> MimeTypes.AUDIO_AAC
            else -> MimeTypes.AUDIO_MPEG // Default to MPEG for radio streams
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

    private fun scheduleRetryEligibility(station: Station) {
        retryEligibilityJobs.remove(station.id)?.cancel()

        retryEligibilityJobs[station.id] = resolverScope.launch {
            delay(FAILED_STATION_RETRY_DELAY_MILLISECONDS)

            withContext(Dispatchers.Main) {
                station.failedThisSession = false
                retryEligibilityJobs.remove(station.id)
            }
        }
    }

    private fun cancelRetryEligibility(station: Station) {
        retryEligibilityJobs.remove(station.id)?.cancel()
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
                val resolvedUrl = result.resolvedUrl?.trim().orEmpty()
                if (result.success && result.verified && resolvedUrl.isNotBlank()) {
                    station.resolvedStreamUrl = resolvedUrl
                    station.streamVerified = true
                    station.lastVerified = System.currentTimeMillis()
                    station.failedThisSession = false
                    cancelRetryEligibility(station)
                    appPreferences.saveStationRepair(station)

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
        cancelStallWatchdog()
        nextGeneration++

        activeRequest = null
        pendingRequest = null
        errorMessage = null
        nowPlayingArtworkUri = null

        controller?.run {
            stop()
            clearMediaItems()
        }

        isPlaying = false
        playbackRequested = false
        activeRequestHasPlayed = false
    }

    fun release() {
        cancelStartupWatchdog()
        cancelStallWatchdog()
        retryEligibilityJobs.values.forEach { it.cancel() }
        retryEligibilityJobs.clear()
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
        private const val STALL_FAILURE_TIMEOUT_MILLISECONDS = 10_000L
        private const val FAILED_STATION_RETRY_DELAY_MILLISECONDS = 15 * 60 * 1000L
        const val TEST_STREAM_URL = "https://ice5.somafm.com/groovesalad-128-mp3"
    }
}
