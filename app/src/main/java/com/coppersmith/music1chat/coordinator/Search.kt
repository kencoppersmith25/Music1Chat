package com.coppersmith.music1chat.coordinator

// Music1Chat coordinated release
// Date: 2026-07-30
// Release: 2026-07-30 v02
//
// Owns the complete search workflow calculation. MainScreen remains
// responsible only for applying the returned state to Compose and playback.

import com.coppersmith.music1chat.models.Station
import com.coppersmith.music1chat.persistence.SavedSearchCategory
import com.coppersmith.music1chat.search.LiveStationSearchEngine
import com.coppersmith.music1chat.search.SearchResult
import com.coppersmith.music1chat.search.StationSearchEngine
import com.coppersmith.music1chat.session.PlaybackSessionController
import com.coppersmith.music1chat.session.PlaybackSessionMode
import com.coppersmith.music1chat.session.PlaybackSessionState
import android.util.Log

data class CoordinatedSearchResult(
    val query: String,
    val localCount: Int,
    val liveCount: Int,
    val stations: List<Station>
)

sealed interface SearchWorkflowOutcome {
    data class Success(
        val state: PlaybackSessionState,
        val savedSearch: SavedSearchCategory,
        val localCount: Int,
        val liveCount: Int
    ) : SearchWorkflowOutcome

    data class CachedFallback(
        val state: PlaybackSessionState
    ) : SearchWorkflowOutcome

    data object Empty : SearchWorkflowOutcome
}

class Search(
    private val stationSearchEngine: StationSearchEngine = StationSearchEngine(),
    private val liveStationSearchEngine: LiveStationSearchEngine = LiveStationSearchEngine()
) {
    fun normalizedKey(query: String): String =
        query.trim().lowercase()

    fun savedSearchFor(
        savedSearches: List<SavedSearchCategory>,
        query: String
    ): SavedSearchCategory? =
        savedSearches.firstOrNull { savedSearch ->
            savedSearch.query.equals(query, ignoreCase = true)
        }

    fun resolveAnchorCategoryId(
        preserveAnchor: Boolean,
        stateBeforeSearch: PlaybackSessionState,
        existingSavedSearch: SavedSearchCategory?,
        currentAnchorCategoryId: Long?
    ): Long? =
        existingSavedSearch?.anchorCategoryId
            ?: if (!preserveAnchor && !stateBeforeSearch.isSearch) {
                stateBeforeSearch.categoryId ?: currentAnchorCategoryId
            } else {
                currentAnchorCategoryId
            }

    suspend fun executeWorkflow(
        query: String,
        limit: Int,
        repositoryStations: List<Station>,
        savedSearches: List<SavedSearchCategory>,
        cachedSessions: Map<String, PlaybackSessionState>,
        anchorCategoryId: Long?,
        startPlayback: Boolean,
        sessionController: PlaybackSessionController,
        isStartup: Boolean = false
    ): SearchWorkflowOutcome {
        val searchQuery = query.trim()
        val coordinatedResult = search(
            query = searchQuery,
            limit = limit,
            repositoryStations = repositoryStations
        )

        val savedSearch = savedSearchFor(savedSearches, searchQuery)
        val previousCount = savedSearch?.lastResultCount ?: 0

        // SAFETY SHIELD: If a refresh returns significantly fewer results than before,
        // it's likely an API hiccup. Use the cache instead of corrupting the category.
        val isSuspectShrink = coordinatedResult.stations.size < (previousCount / 2) && previousCount > 10
        
        if (coordinatedResult.stations.isEmpty() || isSuspectShrink) {
            val cachedSearch =
                cachedSessions[normalizedKey(searchQuery)]
                    ?.takeIf { state -> state.hasStations }
                    ?: if (coordinatedResult.stations.isEmpty()) return SearchWorkflowOutcome.Empty else null

            if (cachedSearch != null) {
                return SearchWorkflowOutcome.CachedFallback(
                    state = sessionController.showSearch(
                        query = cachedSearch.categoryName,
                        stations = cachedSearch.stations,
                        preferredStationId = cachedSearch.currentStation?.id,
                        startPlayback = startPlayback
                    )
                )
            }
        }

        val newState =
            sessionController.showSearch(
                query = searchQuery,
                stations = coordinatedResult.stations,
                preferredStationId = preferredStationId(
                    savedSearch = savedSearch,
                    stations = coordinatedResult.stations
                ),
                startPlayback = startPlayback
            )

        return SearchWorkflowOutcome.Success(
            state = newState,
            savedSearch = SavedSearchCategory(
                query = searchQuery,
                anchorCategoryId =
                    savedSearch?.anchorCategoryId ?: anchorCategoryId,
                lastResultCount = coordinatedResult.stations.size,
                isCurrent = true,
                currentStationId = newState.currentStation?.id,
                currentIndex = newState.safeCurrentIndex,
                navigationEnabled =
                    savedSearch?.navigationEnabled ?: true,
                sortOrder =
                    savedSearch?.sortOrder ?: savedSearches.size
            ),
            localCount = coordinatedResult.localCount,
            liveCount = coordinatedResult.liveCount
        )
    }

    suspend fun prefetchWorkflow(
        query: String,
        limit: Int,
        repositoryStations: List<Station>,
        savedSearches: List<SavedSearchCategory>
    ): PlaybackSessionState? {
        val searchQuery = query.trim()
        val stations = search(
            query = searchQuery,
            limit = limit,
            repositoryStations = repositoryStations
        ).stations

        if (stations.isEmpty()) {
            return null
        }

        return createPrefetchedState(
            query = searchQuery,
            stations = stations,
            savedSearch = savedSearchFor(savedSearches, searchQuery)
        )
    }

    suspend fun search(
        query: String,
        limit: Int,
        repositoryStations: List<Station>
    ): CoordinatedSearchResult {
        val searchQuery = query.trim()

        val localResult =
            stationSearchEngine.search(
                query = searchQuery,
                stations = repositoryStations
            )

        val liveResult =
            runCatching {
                liveStationSearchEngine.search(
                    query = searchQuery,
                    limit = limit
                )
            }.getOrElse {
                SearchResult(
                    query = searchQuery,
                    stations = emptyList()
                )
            }

        val mergedStations =
            (localResult.stations + liveResult.stations)
                .onEach { station ->
                    station.includedInNavigation = true
                }
                .distinctBy { station ->
                    station.resolvedStreamUrl
                        .ifBlank { station.streamUrl }
                        .trim()
                        .lowercase()
                        .ifBlank {
                            station.name.trim().lowercase()
                        }
                }
                .take(limit)

        val interleavedStations = interleaveStations(mergedStations)

        return CoordinatedSearchResult(
            query = searchQuery,
            localCount = localResult.stations.size,
            liveCount = liveResult.stations.size,
            stations = interleavedStations
        )
    }

    /**
     * Interleaves stations to prevent consecutive related stations (sister stations) 
     * from appearing together. This helps mitigate consecutive ads from the same provider.
     * 
     * The logic places station i at position: (i * 10) % N + (i * 10) / N
     */
    private fun interleaveStations(stations: List<Station>): List<Station> {
        if (stations.size <= 1) return stations

        val n = stations.size
        val stride = 10
        val result = arrayOfNulls<Station>(n)

        for (i in stations.indices) {
            val targetIndex = ((i * stride) % n) + ((i * stride) / n)
            // Safety check for array bounds in case of rounding/division edge cases
            val safeIndex = targetIndex.coerceIn(0, n - 1)
            
            // If the position is already occupied (can happen if n and stride have common factors 
            // and the formula doesn't perfectly distribute), find the next available slot.
            if (result[safeIndex] == null) {
                result[safeIndex] = stations[i]
            } else {
                for (j in 0 until n) {
                    val fallbackIndex = (safeIndex + j) % n
                    if (result[fallbackIndex] == null) {
                        result[fallbackIndex] = stations[i]
                        break
                    }
                }
            }
        }

        return result.filterNotNull()
    }

    fun preferredStationId(
        savedSearch: SavedSearchCategory?,
        stations: List<Station>
    ): Long? =
        savedSearch?.currentStationId
            ?.takeIf { savedId ->
                stations.any { station -> station.id == savedId }
            }
            ?: savedSearch?.currentIndex
                ?.let { savedIndex -> stations.getOrNull(savedIndex)?.id }

    fun createPrefetchedState(
        query: String,
        stations: List<Station>,
        savedSearch: SavedSearchCategory?
    ): PlaybackSessionState {
        val preferredStationId =
            preferredStationId(
                savedSearch = savedSearch,
                stations = stations
            )

        val preferredIndex =
            preferredStationId
                ?.let { stationId ->
                    stations.indexOfFirst { station ->
                        station.id == stationId
                    }
                }
                ?.takeIf { index -> index >= 0 }
                ?: 0

        return PlaybackSessionState(
            mode = PlaybackSessionMode.SEARCH,
            categoryId = null,
            categoryName = query.trim(),
            stations = stations,
            currentIndex = preferredIndex,
            playbackRequested = false
        )
    }
}
