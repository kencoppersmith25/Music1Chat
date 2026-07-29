package com.coppersmith.music1chat.coordinator

// Music1Chat MainScreen extraction: 2026-07-29 v01
// Owns station search, result merging/deduplication, and preferred-result restoration.

import com.coppersmith.music1chat.models.Station
import com.coppersmith.music1chat.persistence.SavedSearchCategory
import com.coppersmith.music1chat.search.LiveStationSearchEngine
import com.coppersmith.music1chat.search.SearchResult
import com.coppersmith.music1chat.search.StationSearchEngine
import com.coppersmith.music1chat.session.PlaybackSessionMode
import com.coppersmith.music1chat.session.PlaybackSessionState

data class CoordinatedSearchResult(
    val query: String,
    val localCount: Int,
    val liveCount: Int,
    val stations: List<Station>
)

class MainScreenSearchCoordinator(
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

        return CoordinatedSearchResult(
            query = searchQuery,
            localCount = localResult.stations.size,
            liveCount = liveResult.stations.size,
            stations = mergedStations
        )
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