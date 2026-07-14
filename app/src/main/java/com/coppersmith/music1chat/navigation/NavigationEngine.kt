package com.coppersmith.music1chat.navigation

import com.coppersmith.music1chat.models.Category
import com.coppersmith.music1chat.models.Station
import com.coppersmith.music1chat.repository.CategoryRepository
import com.coppersmith.music1chat.repository.MembershipRepository
import com.coppersmith.music1chat.repository.StationRepository

class NavigationEngine(
    private val categoryRepository: CategoryRepository,
    private val stationRepository: StationRepository,
    private val membershipRepository: MembershipRepository,
    initialState: NavigationState = NavigationState()
) {
    private var state = initialState

    init {
        if (state.currentCategoryId == null || state.currentStationId == null) {
            val firstSelection = findFirstPlayableSelection()

            if (firstSelection != null) {
                state = state.copy(
                    currentCategoryId = firstSelection.category.id,
                    currentStationId = firstSelection.station.id
                )
            }
        }
    }

    fun getState(): NavigationState = state

    fun execute(command: NavigationCommand): NavigationResult {
        return when (command) {
            NavigationCommand.NEXT_STATION ->
                moveStation(NavigationDirection.FORWARD)

            NavigationCommand.PREVIOUS_STATION ->
                moveStation(NavigationDirection.BACKWARD)

            NavigationCommand.NEXT_CATEGORY ->
                moveCategory(NavigationDirection.FORWARD)

            NavigationCommand.PREVIOUS_CATEGORY ->
                moveCategory(NavigationDirection.BACKWARD)

            NavigationCommand.PLAY -> play()
            NavigationCommand.STOP -> stop()
            NavigationCommand.TOGGLE_PLAYBACK -> togglePlayback()
        }
    }

    private fun play(): NavigationResult {
        val currentCategoryId = state.currentCategoryId
        val currentStation = state.currentStationId?.let { stationId ->
            stationRepository.getById(stationId)
        }

        if (
            currentCategoryId != null &&
            currentStation != null &&
            isStationEligible(currentStation) &&
            membershipRepository.contains(
                categoryId = currentCategoryId,
                stationId = currentStation.id
            )
        ) {
            state = state.copy(isPlaying = true)

            return NavigationResult(
                state = state,
                shouldStartPlayback = true
            )
        }

        val firstSelection = findFirstPlayableSelection()
            ?: return noEligibleStationsResult()

        return selectStation(
            categoryId = firstSelection.category.id,
            station = firstSelection.station,
            startPlayback = true
        )
    }

    private fun stop(): NavigationResult {
        state = state.copy(isPlaying = false)

        return NavigationResult(state = state)
    }

    private fun togglePlayback(): NavigationResult {
        return if (state.isPlaying) stop() else play()
    }

    private fun moveStation(
        direction: NavigationDirection
    ): NavigationResult {
        val currentCategory = getCurrentCategory()
            ?: getPlayableCategories().firstOrNull()
            ?: return noEligibleCategoriesResult()

        val categoryStations =
            membershipRepository.getNavigationStationsForCategory(
                currentCategory.id
            )

        if (categoryStations.isEmpty()) {
            return NavigationResult(
                state = state,
                statusMessage =
                    "No playable stations are available in ${currentCategory.name}."
            )
        }

        val currentIndex = categoryStations.indexOfFirst { station ->
            station.id == state.currentStationId
        }

        val targetIndex = when {
            currentIndex < 0 -> {
                if (direction == NavigationDirection.FORWARD) {
                    0
                } else {
                    categoryStations.lastIndex
                }
            }

            direction == NavigationDirection.FORWARD -> {
                if (currentIndex == categoryStations.lastIndex) {
                    0
                } else {
                    currentIndex + 1
                }
            }

            else -> {
                if (currentIndex == 0) {
                    categoryStations.lastIndex
                } else {
                    currentIndex - 1
                }
            }
        }

        return selectStation(
            categoryId = currentCategory.id,
            station = categoryStations[targetIndex],
            startPlayback = true
        )
    }

    private fun moveCategory(
        direction: NavigationDirection
    ): NavigationResult {
        val playableCategories = getPlayableCategories()

        if (playableCategories.isEmpty()) {
            return noEligibleCategoriesResult()
        }

        val currentCategoryIndex =
            playableCategories.indexOfFirst { category ->
                category.id == state.currentCategoryId
            }

        val targetCategoryIndex = when {
            currentCategoryIndex < 0 -> {
                if (direction == NavigationDirection.FORWARD) {
                    0
                } else {
                    playableCategories.lastIndex
                }
            }

            direction == NavigationDirection.FORWARD -> {
                if (currentCategoryIndex == playableCategories.lastIndex) {
                    0
                } else {
                    currentCategoryIndex + 1
                }
            }

            else -> {
                if (currentCategoryIndex == 0) {
                    playableCategories.lastIndex
                } else {
                    currentCategoryIndex - 1
                }
            }
        }

        val targetCategory = playableCategories[targetCategoryIndex]

        val targetStation =
            membershipRepository
                .getNavigationStationsForCategory(targetCategory.id)
                .firstOrNull()
                ?: return NavigationResult(
                    state = state,
                    statusMessage =
                        "No playable stations are available in ${targetCategory.name}."
                )

        return selectStation(
            categoryId = targetCategory.id,
            station = targetStation,
            startPlayback = true
        )
    }

    private fun getCurrentCategory(): Category? {
        val categoryId = state.currentCategoryId ?: return null
        return categoryRepository.getById(categoryId)
    }

    private fun getPlayableCategories(): List<Category> {
        return categoryRepository
            .getNavigationCategories()
            .filter { category ->
                membershipRepository
                    .getNavigationStationsForCategory(category.id)
                    .isNotEmpty()
            }
    }

    private fun findFirstPlayableSelection(): Selection? {
        getPlayableCategories().forEach { category ->
            val station =
                membershipRepository
                    .getNavigationStationsForCategory(category.id)
                    .firstOrNull()

            if (station != null) {
                return Selection(
                    category = category,
                    station = station
                )
            }
        }

        return null
    }

    private fun isStationEligible(station: Station): Boolean {
        return station.includedInNavigation &&
                !station.failedThisSession
    }

    private fun selectStation(
        categoryId: Long,
        station: Station,
        startPlayback: Boolean
    ): NavigationResult {
        val selectionChanged =
            state.currentCategoryId != categoryId ||
                    state.currentStationId != station.id

        state = state.copy(
            currentCategoryId = categoryId,
            currentStationId = station.id,
            isPlaying = if (startPlayback) true else state.isPlaying
        )

        return NavigationResult(
            state = state,
            selectionChanged = selectionChanged,
            shouldStartPlayback = startPlayback
        )
    }

    private fun noEligibleStationsResult(): NavigationResult {
        val message = if (stationRepository.getAll().isEmpty()) {
            "No stations are available."
        } else {
            "No navigation-enabled stations are available."
        }

        return NavigationResult(
            state = state,
            statusMessage = message
        )
    }

    private fun noEligibleCategoriesResult(): NavigationResult {
        val message = if (categoryRepository.getAll().isEmpty()) {
            "No categories are available."
        } else {
            "No categories contain navigation-enabled stations."
        }

        return NavigationResult(
            state = state,
            statusMessage = message
        )
    }

    private data class Selection(
        val category: Category,
        val station: Station
    )

    private enum class NavigationDirection {
        FORWARD,
        BACKWARD
    }
}