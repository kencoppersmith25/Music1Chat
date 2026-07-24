package com.coppersmith.music1chat.session

import com.coppersmith.music1chat.models.Station

enum class PlaybackSessionMode {
    CATEGORY,
    SEARCH
}

data class PlaybackSessionState(
    val mode: PlaybackSessionMode = PlaybackSessionMode.CATEGORY,
    val categoryId: Long? = null,
    val categoryName: String = "",
    val stations: List<Station> = emptyList(),
    val currentIndex: Int = 0,
    val playbackRequested: Boolean = false
) {
    val currentStation: Station?
        get() = stations.getOrNull(safeCurrentIndex)

    val stationCount: Int
        get() = stations.size

    val stationNumber: Int
        get() = if (stations.isEmpty()) 0 else safeCurrentIndex + 1

    val safeCurrentIndex: Int
        get() =
            when {
                stations.isEmpty() -> 0
                currentIndex < 0 -> 0
                currentIndex > stations.lastIndex -> stations.lastIndex
                else -> currentIndex
            }

    val categoryDisplayName: String
        get() =
            when (mode) {
                PlaybackSessionMode.CATEGORY -> categoryName
                PlaybackSessionMode.SEARCH ->
                    if (categoryName.isBlank()) "Search"
                    else "Search: $categoryName"
            }

    val isSearch: Boolean
        get() = mode == PlaybackSessionMode.SEARCH

    val hasStations: Boolean
        get() = stations.isNotEmpty()

    val hasEligibleStations: Boolean
        get() =
            stations.any { station ->
                station.includedInNavigation &&
                        !station.failedThisSession
            }
}

class PlaybackSessionController(
    initialState: PlaybackSessionState = PlaybackSessionState()
) {
    private var state = initialState.normalized()

    fun getState(): PlaybackSessionState = state

    fun clear(): PlaybackSessionState {
        state = PlaybackSessionState()
        return state
    }

    fun showCategory(
        categoryId: Long,
        categoryName: String,
        stations: List<Station>,
        preferredStationId: Long? = null,
        startPlayback: Boolean
    ): PlaybackSessionState {
        state =
            PlaybackSessionState(
                mode = PlaybackSessionMode.CATEGORY,
                categoryId = categoryId,
                categoryName = categoryName.trim(),
                stations = stations.toList(),
                currentIndex = findPreferredIndex(
                    stations,
                    preferredStationId
                ),
                playbackRequested =
                    startPlayback && stations.isNotEmpty()
            ).normalized()

        return state
    }

    fun showSearch(
        query: String,
        stations: List<Station>,
        preferredStationId: Long? = null,
        startPlayback: Boolean
    ): PlaybackSessionState {
        state =
            PlaybackSessionState(
                mode = PlaybackSessionMode.SEARCH,
                categoryId = null,
                categoryName = query.trim(),
                stations = stations.toList(),
                currentIndex = findPreferredIndex(
                    stations,
                    preferredStationId
                ),
                playbackRequested =
                    startPlayback && stations.isNotEmpty()
            ).normalized()

        return state
    }

    fun replaceStations(
        stations: List<Station>,
        keepCurrentStation: Boolean = true
    ): PlaybackSessionState {
        val currentStationId =
            state.currentStation?.id.takeIf {
                keepCurrentStation
            }

        state =
            state.copy(
                stations = stations.toList(),
                currentIndex = findPreferredIndex(
                    stations,
                    currentStationId
                ),
                playbackRequested =
                    state.playbackRequested && stations.isNotEmpty()
            ).normalized()

        return state
    }

    fun selectStation(
        stationId: Long,
        startPlayback: Boolean = true
    ): PlaybackSessionState {
        val selectedIndex =
            state.stations.indexOfFirst {
                it.id == stationId
            }

        if (selectedIndex < 0) return state

        state =
            state.copy(
                currentIndex = selectedIndex,
                playbackRequested = startPlayback
            ).normalized()

        return state
    }

    fun previousStation(
        startPlayback: Boolean = true
    ): PlaybackSessionState =
        moveToEligibleStation(
            direction = -1,
            startPlayback = startPlayback
        )

    fun nextStation(
        startPlayback: Boolean = true
    ): PlaybackSessionState =
        moveToEligibleStation(
            direction = 1,
            startPlayback = startPlayback
        )

    fun play(): PlaybackSessionState {
        state =
            state.copy(
                playbackRequested = state.currentStation != null
            )
        return state
    }

    fun stop(): PlaybackSessionState {
        state = state.copy(playbackRequested = false)
        return state
    }

    fun togglePlayback(): PlaybackSessionState =
        if (state.playbackRequested) stop() else play()

    fun markCurrentStationFailedAndAdvance(): PlaybackSessionState {
        state.currentStation?.failedThisSession = true

        val eligibleExists =
            state.stations.any {
                it.includedInNavigation && !it.failedThisSession
            }

        if (!eligibleExists) {
            state =
                state.copy(playbackRequested = false)
                    .normalized()
            return state
        }

        return moveToEligibleStation(
            direction = 1,
            startPlayback = true
        )
    }

    fun restoreStation(
        stationId: Long?,
        playbackRequested: Boolean
    ): PlaybackSessionState {
        val restoredIndex =
            stationId?.let { id ->
                state.stations.indexOfFirst {
                    it.id == id
                }
            } ?: -1

        state =
            state.copy(
                currentIndex =
                    if (restoredIndex >= 0) restoredIndex
                    else state.safeCurrentIndex,
                playbackRequested =
                    playbackRequested && state.stations.isNotEmpty()
            ).normalized()

        return state
    }

    private fun moveToEligibleStation(
        direction: Int,
        startPlayback: Boolean
    ): PlaybackSessionState {
        if (state.stations.isEmpty()) return state

        val eligibleCount =
            state.stations.count {
                it.includedInNavigation && !it.failedThisSession
            }

        if (eligibleCount == 0) {
            state = state.copy(playbackRequested = false)
            return state
        }

        var candidateIndex = state.safeCurrentIndex

        repeat(state.stations.size) {
            candidateIndex =
                if (direction < 0) {
                    if (candidateIndex <= 0) state.stations.lastIndex
                    else candidateIndex - 1
                } else {
                    if (candidateIndex >= state.stations.lastIndex) 0
                    else candidateIndex + 1
                }

            val candidate = state.stations[candidateIndex]

            if (
                candidate.includedInNavigation &&
                !candidate.failedThisSession
            ) {
                state =
                    state.copy(
                        currentIndex = candidateIndex,
                        playbackRequested = startPlayback
                    ).normalized()

                return state
            }
        }

        return state
    }

    private fun findPreferredIndex(
        stations: List<Station>,
        preferredStationId: Long?
    ): Int {
        if (stations.isEmpty() || preferredStationId == null) return 0

        return stations.indexOfFirst {
            it.id == preferredStationId
        }.takeIf {
            it >= 0
        } ?: 0
    }

    private fun PlaybackSessionState.normalized(): PlaybackSessionState {
        val normalizedIndex =
            when {
                stations.isEmpty() -> 0
                currentIndex < 0 -> 0
                currentIndex > stations.lastIndex -> stations.lastIndex
                else -> currentIndex
            }

        return copy(
            categoryName = categoryName.trim(),
            currentIndex = normalizedIndex,
            playbackRequested =
                playbackRequested && stations.isNotEmpty()
        )
    }
}