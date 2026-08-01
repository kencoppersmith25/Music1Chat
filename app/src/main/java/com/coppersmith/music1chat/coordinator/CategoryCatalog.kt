package com.coppersmith.music1chat.coordinator

// Music1Chat coordinated release
// Date: 2026-07-30
// Release: 2026-07-30 v01
//
// Owns category-key translation and the read-only category-list view model.

import com.coppersmith.music1chat.models.Category
import com.coppersmith.music1chat.models.Station
import com.coppersmith.music1chat.persistence.SavedSearchCategory
import com.coppersmith.music1chat.repository.MusicRepository
import com.coppersmith.music1chat.session.PlaybackSessionState
import com.coppersmith.music1chat.ui.screens.CategorySummary

class CategoryCatalog(
    private val musicRepository: MusicRepository,
    private val normalizedSearchKey: (String) -> String
) {

    fun stationsForKey(
        key: String,
        searchSessionStates: Map<String, PlaybackSessionState>
    ): List<Station> =
        if (key.isSearchKey()) {
            searchSessionStates[
                normalizedSearchKey(key.searchQuery())
            ]?.stations.orEmpty()
        } else {
            key.permanentCategoryId()?.let { categoryId ->
                musicRepository.memberships
                    .getStationsForCategory(categoryId)
            }.orEmpty()
        }

    fun displayNameForKey(
        key: String,
        searchSessionStates: Map<String, PlaybackSessionState>
    ): String {
        val stationCount =
            stationsForKey(
                key = key,
                searchSessionStates = searchSessionStates
            ).size

        return if (key.isSearchKey()) {
            "Search: ${key.searchQuery()} ($stationCount)"
        } else {
            val categoryName =
                key.permanentCategoryId()?.let { categoryId ->
                    musicRepository.categories
                        .getById(categoryId)
                        ?.name
                }.orEmpty()

            "$categoryName ($stationCount)"
        }
    }

    fun categoryRows(
        permanentCategories: List<Category>,
        savedSearches: List<SavedSearchCategory>,
        searchSessionStates: Map<String, PlaybackSessionState>
    ): List<CategorySummary> {
        val permanentRows =
            permanentCategories.associate { category ->
                category.key() to
                        CategorySummary(
                            key = category.key(),
                            name = category.name,
                            stationCount =
                                musicRepository.memberships
                                    .getStationsForCategory(category.id)
                                    .size,
                            includedInNavigation =
                                category.includedInNavigation
                        )
            }

        val searchRows =
            savedSearches.associate { search ->
                search.key() to
                        CategorySummary(
                            key = search.key(),
                            name = "Search: ${search.query}",
                            stationCount =
                                searchSessionStates[
                                    normalizedSearchKey(search.query)
                                ]?.stationCount
                                    ?: search.lastResultCount,
                            includedInNavigation =
                                search.navigationEnabled
                        )
            }

        return orderedKeys(
            permanentCategories = permanentCategories,
            savedSearches = savedSearches
        ).mapNotNull { key ->
            permanentRows[key] ?: searchRows[key]
        }
    }

    private fun orderedKeys(
        permanentCategories: List<Category>,
        savedSearches: List<SavedSearchCategory>
    ): List<String> =
        buildList {
            val permanentIds =
                permanentCategories.map { category ->
                    category.id
                }

            permanentCategories.forEach { category ->
                add(category.key())

                savedSearches
                    .filter { search ->
                        search.anchorCategoryId == category.id
                    }
                    .sortedBy { search ->
                        search.sortOrder
                    }
                    .forEach { search ->
                        add(search.key())
                    }
            }

            savedSearches
                .filter { search ->
                    search.anchorCategoryId !in permanentIds
                }
                .sortedBy { search ->
                    search.sortOrder
                }
                .forEach { search ->
                    add(search.key())
                }
        }

    private fun String.isSearchKey(): Boolean =
        startsWith(SEARCH_PREFIX)

    private fun String.searchQuery(): String =
        removePrefix(SEARCH_PREFIX)

    private fun String.permanentCategoryId(): Long? =
        removePrefix(CATEGORY_PREFIX).toLongOrNull()

    private fun Category.key(): String =
        "$CATEGORY_PREFIX$id"

    private fun SavedSearchCategory.key(): String =
        "$SEARCH_PREFIX$query"

    private companion object {
        const val CATEGORY_PREFIX = "category:"
        const val SEARCH_PREFIX = "search:"
    }
}