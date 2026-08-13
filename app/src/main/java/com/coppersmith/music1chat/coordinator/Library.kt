package com.coppersmith.music1chat.coordinator

// Music1Chat coordinated release
// Date: 2026-07-30
// Release: 2026-07-30 v01
//
// Owns permanent-library initialization and playback-selection restoration.

import android.content.Context
import com.coppersmith.music1chat.models.Category
import com.coppersmith.music1chat.models.Station
import com.coppersmith.music1chat.persistence.AppPreferences
import com.coppersmith.music1chat.persistence.SavedPlaybackState
import com.coppersmith.music1chat.persistence.SavedSearchCategory
import com.coppersmith.music1chat.repository.MusicRepository

class Library(
    context: Context
) {

    private val preferences =
        AppPreferences(context.applicationContext)

    fun load(): LibraryStartup {
        val musicRepository =
            MusicRepository().also { repository ->
                if (preferences.hasPermanentLibrary()) {
                    preferences.restorePermanentLibrary(
                        categoryRepository = repository.categories,
                        stationRepository = repository.stations,
                        membershipRepository = repository.memberships
                    )
                } else {
                    repository.seedDefaults()

                    preferences.savePermanentLibrary(
                        categoryRepository = repository.categories,
                        stationRepository = repository.stations,
                        membershipRepository = repository.memberships
                    )
                }

                // SNEAKY CLEANUP: Purge any static categories that have zero stations.
                // This prevents "ghost" categories from cluttering the library if a 
                // deletion didn't complete or was interrupted in a previous build.
                val emptyCategories = repository.categories.getAll().filter { category ->
                    repository.memberships.getStationsForCategory(category.id).isEmpty()
                }

                if (emptyCategories.isNotEmpty()) {
                    emptyCategories.forEach { empty ->
                        repository.memberships.removeCategory(empty.id)
                        repository.categories.remove(empty.id)
                    }

                    preferences.savePermanentLibrary(
                        categoryRepository = repository.categories,
                        stationRepository = repository.stations,
                        membershipRepository = repository.memberships
                    )
                }
            }

        val repositoryCategories =
            musicRepository.categories.getAll()

        val repositoryStations =
            musicRepository.stations.getAll()

        preferences.restoreStationRepairs(
            repositoryStations
        )

        val savedPlaybackState =
            preferences.loadPlaybackState()

        val savedSearchCategories =
            loadCleanSearchCategories()

        val initiallyCurrentSearch =
            savedSearchCategories
                .firstOrNull { savedSearch ->
                    savedSearch.isCurrent
                }
                ?: savedSearchCategories
                    .firstOrNull { savedSearch ->
                        savedSearch.navigationEnabled
                    }
                ?: savedSearchCategories.firstOrNull()

        val membershipRepository =
            musicRepository.memberships

        val firstPlayableCategory =
            musicRepository.categories
                .getNavigationCategories()
                .firstOrNull { category ->
                    membershipRepository
                        .getNavigationStationsForCategory(category.id)
                        .isNotEmpty()
                }
                ?: musicRepository.categories
                    .getAll()
                    .firstOrNull { category ->
                        membershipRepository
                            .getStationsForCategory(category.id)
                            .isNotEmpty()
                    }
        val firstPlayableStation =
            firstPlayableCategory?.let { category ->
                if (category.includedInNavigation) {
                    membershipRepository
                        .getNavigationStationsForCategory(category.id)
                        .firstOrNull()
                } else {
                    membershipRepository
                        .getStationsForCategory(category.id)
                        .firstOrNull()
                }
            }

        val savedCategory =
            savedPlaybackState.categoryId?.let { categoryId ->
                musicRepository.categories.getById(categoryId)
            }

        val savedStation =
            savedPlaybackState.stationId?.let { stationId ->
                musicRepository.stations.getById(stationId)
            }

        val savedSelectionIsValid =
            savedCategory != null &&
                    savedStation != null &&
                    savedCategory.includedInNavigation &&
                    savedStation.includedInNavigation &&
                    !savedStation.failedThisSession &&
                    membershipRepository.contains(
                        categoryId = savedCategory.id,
                        stationId = savedStation.id
                    )

        val initialCategory =
            if (savedSelectionIsValid) {
                savedCategory
            } else {
                firstPlayableCategory
            }

        val initialStation =
            if (savedSelectionIsValid) {
                savedStation
            } else {
                firstPlayableStation
            }

        val shouldResumePlayback =
            savedSelectionIsValid &&
                    savedPlaybackState.wasPlaying

        val initialStations =
            initialCategory?.let { category ->
                membershipRepository
                    .getNavigationStationsForCategory(category.id)
            } ?: emptyList()

        return LibraryStartup(
            preferences = preferences,
            musicRepository = musicRepository,
            repositoryCategories = repositoryCategories,
            repositoryStations = repositoryStations,
            savedPlaybackState = savedPlaybackState,
            savedSearchCategories = savedSearchCategories,
            initiallyCurrentSearch = initiallyCurrentSearch,
            initialCategory = initialCategory,
            initialStation = initialStation,
            initialStations = initialStations,
            shouldResumePlayback = shouldResumePlayback
        )
    }

    private fun loadCleanSearchCategories(): List<SavedSearchCategory> {
        val loadedSearchCategories =
            preferences.loadSearchCategories()

        /*
         * Remove malformed search records left by an older build.
         * Otherwise they appear in the category list as "Search:".
         */
        loadedSearchCategories
            .filter { savedSearch ->
                savedSearch.query.isBlank()
            }
            .forEach { malformedSearch ->
                preferences.removeSearchCategory(
                    malformedSearch.query
                )
            }

        return preferences.loadSearchCategories()
            .filter { savedSearch ->
                savedSearch.query.isNotBlank()
            }
            .map { savedSearch ->
                savedSearch.copy(
                    query = savedSearch.query.trim()
                )
            }
            .distinctBy { savedSearch ->
                savedSearch.query.lowercase()
            }
    }
}

data class LibraryStartup(
    val preferences: AppPreferences,
    val musicRepository: MusicRepository,
    val repositoryCategories: List<Category>,
    val repositoryStations: List<Station>,
    val savedPlaybackState: SavedPlaybackState,
    val savedSearchCategories: List<SavedSearchCategory>,
    val initiallyCurrentSearch: SavedSearchCategory?,
    val initialCategory: Category?,
    val initialStation: Station?,
    val initialStations: List<Station>,
    val shouldResumePlayback: Boolean
)