package com.coppersmith.music1chat.coordinator

// Music1Chat coordinated release
// Date: 2026-07-30
// Release: 2026-07-30 v01
//
// Owns permanent-category station edits and returns any session refresh
// required by MainScreen.

import com.coppersmith.music1chat.models.Station
import com.coppersmith.music1chat.persistence.AppPreferences
import com.coppersmith.music1chat.repository.MusicRepository
import com.coppersmith.music1chat.session.PlaybackSessionController
import com.coppersmith.music1chat.session.PlaybackSessionState

class StationOperations(
    private val preferences: AppPreferences,
    private val musicRepository: MusicRepository,
    private val sessionController: PlaybackSessionController
) {

    private val memberships = musicRepository.memberships

    fun toggleNavigation(
        station: Station
    ): PlaybackSessionState? {
        station.includedInNavigation =
            !station.includedInNavigation

        savePermanentLibrary()

        val activeState = sessionController.getState()

        return activeState.takeIf { state ->
            state.stations.any { it.id == station.id }
        }
    }

    fun moveStation(
        categoryId: Long,
        station: Station,
        newPosition: Int,
        currentState: PlaybackSessionState,
        wasPlaying: Boolean
    ): PlaybackSessionState? {
        memberships.moveStation(
            categoryId = categoryId,
            stationId = station.id,
            newPosition = newPosition
        )

        savePermanentLibrary()

        if (
            currentState.isSearch ||
            currentState.categoryId != categoryId
        ) {
            return null
        }

        val reorderedStations =
            memberships.getStationsForCategory(categoryId)

        return sessionController.showCategory(
            categoryId = categoryId,
            categoryName = currentState.categoryName,
            stations = reorderedStations,
            preferredStationId = currentState.currentStation?.id,
            startPlayback = wasPlaying
        )
    }

    fun deleteStation(
        station: Station,
        categoryId: Long,
        currentState: PlaybackSessionState,
        wasPlaying: Boolean
    ): StationDeletionResult {
        val deletingCurrentStation =
            !currentState.isSearch &&
                    currentState.categoryId == categoryId &&
                    currentState.currentStation?.id == station.id

        val oldIndex = currentState.safeCurrentIndex

        memberships.removeStationFromCategory(
            categoryId = categoryId,
            stationId = station.id
        )

        savePermanentLibrary()

        val remainingStations =
            memberships.getStationsForCategory(categoryId)

        val isCategoryEmpty = remainingStations.isEmpty()

        if (
            currentState.isSearch ||
            currentState.categoryId != categoryId
        ) {
            return StationDeletionResult(
                isCategoryEmpty = isCategoryEmpty
            )
        }

        val category =
            musicRepository.categories.getById(categoryId)
                ?: return StationDeletionResult(
                    isCategoryEmpty = isCategoryEmpty
                )

        val preferredStation =
            if (deletingCurrentStation) {
                remainingStations.getOrNull(
                    oldIndex.coerceAtMost(
                        remainingStations.lastIndex.coerceAtLeast(0)
                    )
                )
            } else {
                currentState.currentStation?.let { current ->
                    remainingStations.firstOrNull {
                        it.id == current.id
                    }
                }
            }

        val shouldContinuePlaying =
            wasPlaying && remainingStations.isNotEmpty()

        val refreshedState =
            sessionController.showCategory(
                categoryId = categoryId,
                categoryName = category.name,
                stations = remainingStations,
                preferredStationId = preferredStation?.id,
                startPlayback = shouldContinuePlaying
            )

        return StationDeletionResult(
            refreshedState = refreshedState,
            shouldContinuePlaying = shouldContinuePlaying,
            shouldStopPlayback = isCategoryEmpty,
            shouldStartPlayback =
                deletingCurrentStation && shouldContinuePlaying,
            isCategoryEmpty = isCategoryEmpty,
            statusMessage =
                if (isCategoryEmpty) {
                    "No stations remain in ${category.name}."
                } else {
                    null
                }
        )
    }

    private fun savePermanentLibrary() {
        preferences.savePermanentLibrary(
            categoryRepository = musicRepository.categories,
            stationRepository = musicRepository.stations,
            membershipRepository = memberships
        )
    }
}

data class StationDeletionResult(
    val refreshedState: PlaybackSessionState? = null,
    val shouldContinuePlaying: Boolean = false,
    val shouldStopPlayback: Boolean = false,
    val shouldStartPlayback: Boolean = false,
    val isCategoryEmpty: Boolean = false,
    val statusMessage: String? = null
)
