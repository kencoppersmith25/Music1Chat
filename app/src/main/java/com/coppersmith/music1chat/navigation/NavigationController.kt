package com.coppersmith.music1chat.navigation

import com.coppersmith.music1chat.models.Station
import com.coppersmith.music1chat.repository.MusicRepository

class NavigationController(
    private val musicRepository: MusicRepository,
    initialState: NavigationState = NavigationState(),
    private val playStation: (Station) -> Unit,
    private val stopPlayback: () -> Unit
) {
    private val navigationEngine = NavigationEngine(
        categoryRepository = musicRepository.categories,
        stationRepository = musicRepository.stations,
        membershipRepository = musicRepository.memberships,
        initialState = initialState
    )

    fun getState(): NavigationState {
        return navigationEngine.getState()
    }

    fun execute(
        command: NavigationCommand
    ): NavigationResult {
        val result = navigationEngine.execute(command)

        when {
            command == NavigationCommand.STOP -> {
                stopPlayback()
            }

            result.shouldStartPlayback -> {
                val selectedStation = findStation(
                    stationId = result.state.currentStationId
                )

                if (selectedStation != null) {
                    playStation(selectedStation)
                } else {
                    return result.copy(
                        shouldStartPlayback = false,
                        statusMessage =
                            "No station is currently selected."
                    )
                }
            }
        }

        return result
    }

    fun getCurrentStation(): Station? {
        return findStation(
            stationId = navigationEngine
                .getState()
                .currentStationId
        )
    }

    private fun findStation(
        stationId: Long?
    ): Station? {
        if (stationId == null) {
            return null
        }

        return musicRepository.stations.getById(stationId)
    }
}