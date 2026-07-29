package com.coppersmith.music1chat.coordinator

// Music1Chat MainScreen extraction: 2026-07-29 v04
// Owns direct playback commands, station movement, and playback diagnostics.

import com.coppersmith.music1chat.RadioPlayer
import com.coppersmith.music1chat.diagnostics.RideLogger
import com.coppersmith.music1chat.session.PlaybackSessionController
import com.coppersmith.music1chat.session.PlaybackSessionState

data class StationMoveResult(
    val state: PlaybackSessionState,
    val changed: Boolean
)

data class PlaybackStartResult(
    val state: PlaybackSessionState,
    val canPlay: Boolean
)

class Playback(
    private val radioPlayer: RadioPlayer,
    private val sessionController: PlaybackSessionController
) {
    fun playCurrentStation(state: PlaybackSessionState): Boolean {
        val station =
            state.currentStation
                ?: run {
                    RideLogger.log(
                        "PLAY_REQUEST_SKIPPED reason='no current station' " +
                                "category='${state.categoryDisplayName}'"
                    )
                    return false
                }

        val source =
            if (state.isSearch) {
                RadioPlayer.PlaybackSource.SEARCH
            } else {
                RadioPlayer.PlaybackSource.NAVIGATION
            }

        val playbackUrl =
            station.resolvedStreamUrl.ifBlank {
                station.streamUrl
            }

        RideLogger.log(
            "PLAY_REQUEST " +
                    "category='${state.categoryDisplayName}' " +
                    "station='${station.name}' " +
                    "stationId=${station.id} " +
                    "source=$source " +
                    "url='$playbackUrl'"
        )

        radioPlayer.play(
            station = station,
            source = source
        )

        RideLogger.log(
            "PLAY_REQUEST_SENT " +
                    "station='${station.name}' " +
                    "stationId=${station.id}"
        )

        return true
    }

    fun moveStation(direction: Int): StationMoveResult {
        val beforeState = sessionController.getState()

        RideLogger.log(
            "STATION_COMMAND direction=$direction " +
                    "category='${beforeState.categoryDisplayName}' " +
                    "before='${beforeState.currentStation?.name.orEmpty()}' " +
                    "count=${beforeState.stationCount}"
        )

        val newState =
            if (direction < 0) {
                sessionController.previousStation(startPlayback = true)
            } else {
                sessionController.nextStation(startPlayback = true)
            }

        val changed =
            newState.currentStation?.id != beforeState.currentStation?.id

        if (!changed) {
            RideLogger.log(
                "STATION_RESULT unchanged=true " +
                        "category='${beforeState.categoryDisplayName}' " +
                        "station='${beforeState.currentStation?.name.orEmpty()}'"
            )
        } else {
            RideLogger.log(
                "STATION_RESULT category='${newState.categoryDisplayName}' " +
                        "after='${newState.currentStation?.name.orEmpty()}' " +
                        "index=${newState.safeCurrentIndex + 1}/${newState.stationCount}"
            )
        }

        return StationMoveResult(
            state = newState,
            changed = changed
        )
    }

    fun stop(): PlaybackSessionState {
        RideLogger.log(
            "PLAYBACK_STOP station='${sessionController.getState().currentStation?.name.orEmpty()}'"
        )

        radioPlayer.stop()
        return sessionController.stop()
    }

    fun start(): PlaybackStartResult {
        RideLogger.log(
            "PLAYBACK_START station='${sessionController.getState().currentStation?.name.orEmpty()}'"
        )

        val newState = sessionController.play()

        return PlaybackStartResult(
            state = newState,
            canPlay = newState.hasEligibleStations
        )
    }

    fun markCurrentStationFailedAndAdvance(): PlaybackSessionState =
        sessionController.markCurrentStationFailedAndAdvance()
}