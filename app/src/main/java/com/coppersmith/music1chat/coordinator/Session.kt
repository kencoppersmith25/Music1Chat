package com.coppersmith.music1chat.coordinator

// Music1Chat coordinated release
// Date: 2026-07-30
// Release: 2026-07-30 v01
//
// Owns playback-session construction, persistence, cached-search restoration,
// and category-navigation decisions. MainScreen applies returned states to
// Compose and starts or stops the player.

import com.coppersmith.music1chat.models.Category
import com.coppersmith.music1chat.persistence.AppPreferences
import com.coppersmith.music1chat.persistence.SavedSearchCategory
import com.coppersmith.music1chat.repository.MusicRepository
import com.coppersmith.music1chat.session.PlaybackSessionController
import com.coppersmith.music1chat.session.PlaybackSessionState

data class SessionSaveResult(
    val savedSearches: List<SavedSearchCategory>,
    val activeSearchQuery: String?
)

class Session(
    private val preferences: AppPreferences,
    private val musicRepository: MusicRepository,
    private val sessionController: PlaybackSessionController,
    private val navigationCoordinator: MainScreenNavigationCoordinator
) {
    private val memberships = musicRepository.memberships

    fun save(
        state: PlaybackSessionState,
        wasPlaying: Boolean,
        savedSearches: List<SavedSearchCategory>,
        activeSearchQuery: String?,
        searchAnchorCategoryId: Long?,
        promoteToFront: Boolean = true
    ): SessionSaveResult {
        if (state.isSearch) {
            // CRITICAL: Clear the library selection when saving a search state.
            // This prevents "Ghost Categories" from playing on startup.
            preferences.savePlaybackState(
                categoryId = null,
                stationId = null,
                wasPlaying = wasPlaying
            )

            val query = state.categoryName.ifBlank {
                activeSearchQuery.orEmpty()
            }

            if (query.isBlank()) {
                return SessionSaveResult(
                    savedSearches = sanitize(savedSearches),
                    activeSearchQuery = activeSearchQuery
                )
            }

            val existing = savedSearches.firstOrNull {
                it.query.equals(query, ignoreCase = true)
            }

            val searchToSave = SavedSearchCategory(
                query = query,
                anchorCategoryId =
                    existing?.anchorCategoryId
                        ?: searchAnchorCategoryId,
                lastResultCount =
                    state.stationCount.takeIf { it > 0 }
                        ?: existing?.lastResultCount
                        ?: 0,
                isCurrent = true,
                currentStationId = state.currentStation?.id,
                currentIndex = state.safeCurrentIndex,
                navigationEnabled =
                    existing?.navigationEnabled ?: true,
                sortOrder = existing?.sortOrder ?: savedSearches.size
            )

            val updated = if (promoteToFront) {
                preferences.upsertSearchCategory(searchToSave, true)
            } else {
                preferences.upsertSearchCategory(searchToSave, false)
            }

            return SessionSaveResult(
                savedSearches = sanitize(updated),
                activeSearchQuery = query
            )
        }

        preferences.savePlaybackState(
            categoryId = state.categoryId,
            stationId = state.currentStation?.id,
            wasPlaying = wasPlaying
        )

        return SessionSaveResult(
            savedSearches = sanitize(
                preferences.markCurrentSearch(null)
            ),
            activeSearchQuery = null
        )
    }

    fun restoreCachedSearch(
        query: String,
        cachedSessions: Map<String, PlaybackSessionState>,
        startPlayback: Boolean
    ): PlaybackSessionState? {
        val cached = cachedSessions[normalizedKey(query)]
            ?.takeIf { it.hasStations }
            ?: return null

        return sessionController.showSearch(
            query = cached.categoryName,
            stations = cached.stations,
            preferredStationId = cached.currentStation?.id,
            startPlayback = startPlayback
        )
    }

    fun selectCategory(
        category: Category,
        preferredStationId: Long? = null,
        startPlayback: Boolean = true
    ): PlaybackSessionState {
        val stations =
            memberships.getStationsForCategory(category.id)
                .filter { !it.failedThisSession }

        return sessionController.showCategory(
            categoryId = category.id,
            categoryName = category.name,
            stations = stations,
            preferredStationId = preferredStationId,
            startPlayback = startPlayback
        )
    }

    fun requestCategoryNavigation(
        direction: Int,
        currentState: PlaybackSessionState,
        savedSearches: List<SavedSearchCategory>,
        cachedSessions: Map<String, PlaybackSessionState>
    ): CategoryNavigationRequest =
        navigationCoordinator.requestNavigation(
            direction = direction,
            currentState = currentState,
            permanentCategories =
                musicRepository.categories.getNavigationCategories(),
            savedSearches = savedSearches,
            categoryHasStations = { categoryId ->
                memberships
                    .getNavigationStationsForCategory(categoryId)
                    .isNotEmpty()
            },
            searchIsCached = { query ->
                cachedSessions[normalizedKey(query)]
                    ?.hasStations == true
            }
        )

    fun finishCategoryNavigation(
        requestedKey: String,
        activatedState: PlaybackSessionState
    ): CategoryNavigationCompletion =
        navigationCoordinator.finishNavigation(
            requestedKey = requestedKey,
            activatedState = activatedState
        )

    fun categoryById(categoryId: Long?): Category? =
        categoryId?.let { musicRepository.categories.getById(it) }

    private fun sanitize(
        searches: List<SavedSearchCategory>
    ): List<SavedSearchCategory> =
        searches
            .filter { it.query.isNotBlank() }
            .map { it.copy(query = it.query.trim()) }
            .distinctBy { it.query.lowercase() }

    private fun normalizedKey(query: String): String =
        query.trim().lowercase()
}