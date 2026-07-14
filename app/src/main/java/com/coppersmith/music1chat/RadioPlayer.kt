package com.coppersmith.music1chat

import android.content.ComponentName
import android.content.Context
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RadioPlayer(
    context: Context,
    private val streamResolver: StreamResolver = StreamResolver()
) {
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

    private var currentStation: Station? = null

    private var pendingStation: Station? = null

    private var controller: MediaController? = null

    private val controllerFuture:
            ListenableFuture<MediaController>

    var onStationFailed:
            ((Station) -> Unit)? = null

    var onStationResolved:
            ((Station, ResolutionResult) -> Unit)? = null

    var isPlaying by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val playerListener =
        object : Player.Listener {

            override fun onIsPlayingChanged(
                playing: Boolean
            ) {
                isPlaying = playing

                if (playing) {
                    errorMessage = null

                    Log.d(
                        "KenCheck",
                        "Controller reports playback active: " +
                                "${currentStation?.name ?: "current station"}"
                    )
                }
            }

            override fun onPlayerError(
                error: PlaybackException
            ) {
                Log.e(
                    "KenCheck",
                    "Controller playback error: " +
                            "${error.errorCodeName}: " +
                            "${error.message}",
                    error
                )

                isPlaying = false

                val failedStation =
                    currentStation

                if (failedStation == null) {
                    errorMessage =
                        "${error.errorCodeName}: " +
                                (
                                        error.message
                                            ?: "Unable to play this station."
                                        )

                    return
                }

                /*
                 * PlaybackService owns temporary network retries.
                 * The station remains selected and eligible.
                 */
                if (isTemporaryNetworkFailure(error)) {
                    failedStation.failedThisSession = false

                    errorMessage =
                        "Network connection lost while playing " +
                                "${failedStation.name}. " +
                                "Music1Chat will keep trying in the background."

                    return
                }

                errorMessage =
                    "Unable to play ${failedStation.name}. " +
                            "${error.errorCodeName}: " +
                            (
                                    error.message
                                        ?: "Unknown playback error."
                                    )

                failedStation.failedThisSession = true

                /*
                 * Preserve the existing behavior for a genuinely
                 * bad station: navigate away immediately.
                 */
                onStationFailed?.invoke(
                    failedStation
                )

                /*
                 * Then investigate and save a repaired URL.
                 */
                resolveFailedStationInBackground(
                    failedStation
                )
            }
        }

    init {
        val sessionToken =
            SessionToken(
                applicationContext,
                ComponentName(
                    applicationContext,
                    PlaybackService::class.java
                )
            )

        controllerFuture =
            MediaController.Builder(
                applicationContext,
                sessionToken
            ).buildAsync()

        controllerFuture.addListener(
            {
                try {
                    val connectedController =
                        controllerFuture.get()

                    controller =
                        connectedController

                    connectedController.addListener(
                        playerListener
                    )

                    isPlaying =
                        connectedController.isPlaying

                    Log.d(
                        "KenCheck",
                        "MediaController connected to PlaybackService"
                    )

                    pendingStation?.let { station ->
                        pendingStation = null

                        startPlayback(
                            station = station,
                            mediaController =
                                connectedController
                        )
                    }
                } catch (exception: Exception) {
                    Log.e(
                        "KenCheck",
                        "Unable to connect to PlaybackService",
                        exception
                    )

                    errorMessage =
                        "Unable to connect to the playback service."
                }
            },
            ContextCompat.getMainExecutor(
                applicationContext
            )
        )
    }

    fun play(
        station: Station
    ) {
        currentStation = station
        errorMessage = null

        val connectedController =
            controller

        if (connectedController == null) {
            pendingStation = station

            Log.d(
                "KenCheck",
                "Playback queued while controller connects: " +
                        station.name
            )

            return
        }

        pendingStation = null

        startPlayback(
            station = station,
            mediaController = connectedController
        )
    }

    private fun startPlayback(
        station: Station,
        mediaController: MediaController
    ) {
        val playbackUrl =
            preferredPlaybackUrl(
                station
            )

        Log.d(
            "KenCheck",
            "Playing ${station.name} through PlaybackService: " +
                    "verified=${station.streamVerified}, " +
                    "resolvedUrl=${station.resolvedStreamUrl}, " +
                    "playbackUrl=$playbackUrl"
        )

        val mediaMetadata =
            MediaMetadata.Builder()
                .setTitle(
                    station.name
                )
                .setGenre(
                    station.genre
                )
                .setStation(
                    station.name
                )
                .build()

        val mediaItem =
            MediaItem.Builder()
                .setMediaId(
                    station.id.toString()
                )
                .setUri(
                    playbackUrl
                )
                .setMediaMetadata(
                    mediaMetadata
                )
                .build()

        mediaController.setMediaItem(
            mediaItem
        )

        mediaController.prepare()
        mediaController.play()
    }

    private fun preferredPlaybackUrl(
        station: Station
    ): String {
        val savedResolvedUrl =
            station.resolvedStreamUrl.trim()

        return if (
            station.streamVerified &&
            savedResolvedUrl.isNotBlank()
        ) {
            savedResolvedUrl
        } else {
            station.streamUrl
        }
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

    private fun resolveFailedStationInBackground(
        station: Station
    ) {
        synchronized(stationsBeingResolved) {
            if (
                !stationsBeingResolved.add(
                    station.id
                )
            ) {
                return
            }
        }

        resolverScope.launch {
            try {
                val result =
                    streamResolver.resolve(
                        station
                    )

                Log.d(
                    "KenCheck",
                    "Resolver result for ${station.name}: " +
                            "success=${result.success}, " +
                            "verified=${result.verified}, " +
                            "resolvedUrl=${result.resolvedUrl}, " +
                            "error=${result.errorMessage}"
                )

                val resolvedUrl =
                    result.resolvedUrl
                        ?.trim()
                        .orEmpty()

                if (
                    result.success &&
                    result.verified &&
                    resolvedUrl.isNotBlank()
                ) {
                    station.resolvedStreamUrl =
                        resolvedUrl

                    station.streamVerified = true

                    station.lastVerified =
                        System.currentTimeMillis()

                    station.failedThisSession = false

                    appPreferences.saveStationRepair(
                        station
                    )

                    Log.d(
                        "KenCheck",
                        "Saved repair for ${station.name}: " +
                                "resolvedUrl=${station.resolvedStreamUrl}, " +
                                "verified=${station.streamVerified}, " +
                                "lastVerified=${station.lastVerified}"
                    )

                    withContext(Dispatchers.Main) {
                        onStationResolved?.invoke(
                            station,
                            result
                        )
                    }
                }
            } finally {
                synchronized(stationsBeingResolved) {
                    stationsBeingResolved.remove(
                        station.id
                    )
                }
            }
        }
    }

    fun stop() {
        pendingStation = null
        errorMessage = null

        controller?.run {
            stop()
            clearMediaItems()
        }

        isPlaying = false
    }

    fun release() {
        pendingStation = null

        onStationFailed = null
        onStationResolved = null
        currentStation = null

        resolverScope.cancel()

        controller?.removeListener(
            playerListener
        )

        controller = null

        /*
         * This releases only the UI controller connection.
         * It does not stop service playback.
         */
        MediaController.releaseFuture(
            controllerFuture
        )
    }

    companion object {
        const val TEST_STREAM_URL =
            "https://ice5.somafm.com/groovesalad-128-mp3"
    }
}