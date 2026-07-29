package com.coppersmith.music1chat.coordinator

// Music1Chat MainScreen extraction: 2026-07-29 v04
// Owns category/search navigation ordering, wraparound selection, and queued commands.

import com.coppersmith.music1chat.models.Category
import com.coppersmith.music1chat.persistence.SavedSearchCategory
import com.coppersmith.music1chat.session.PlaybackSessionState

sealed interface CategoryNavigationRequest {
    data object Empty : CategoryNavigationRequest

    data class Queued(
        val direction: Int,
        val pendingKey: String
    ) : CategoryNavigationRequest

    data class Activate(
        val currentKey: String,
        val selectedKey: String,
        val searchQuery: String?,
        val categoryId: Long?,
        val searchCached: Boolean
    ) : CategoryNavigationRequest
}

data class CategoryNavigationCompletion(
    val succeeded: Boolean,
    val activatedKey: String,
    val queuedDirection: Int?
)

class MainScreenNavigationCoordinator {
    private var pendingNavigationKey: String? = null
    private var queuedDirection: Int? = null

    fun requestNavigation(
        direction: Int,
        currentState: PlaybackSessionState,
        permanentCategories: List<Category>,
        savedSearches: List<SavedSearchCategory>,
        categoryHasStations: (Long) -> Boolean,
        searchIsCached: (String) -> Boolean
    ): CategoryNavigationRequest {
        pendingNavigationKey?.let { pendingKey ->
            queuedDirection = direction
            return CategoryNavigationRequest.Queued(
                direction = direction,
                pendingKey = pendingKey
            )
        }

        val keys = navigationKeys(
            permanentCategories = permanentCategories,
            savedSearches = savedSearches,
            categoryHasStations = categoryHasStations
        )

        if (keys.isEmpty()) {
            return CategoryNavigationRequest.Empty
        }

        val currentKey = keyFor(currentState)
        val currentIndex =
            keys.indexOfFirst { key ->
                key.equals(currentKey, ignoreCase = true)
            }

        val selectedIndex =
            when {
                currentIndex < 0 -> 0
                direction < 0 && currentIndex <= 0 -> keys.lastIndex
                direction < 0 -> currentIndex - 1
                direction > 0 && currentIndex >= keys.lastIndex -> 0
                else -> currentIndex + 1
            }

        val selectedKey = keys[selectedIndex]
        val searchQuery =
            selectedKey
                .takeIf { key -> key.startsWith(SEARCH_PREFIX) }
                ?.removePrefix(SEARCH_PREFIX)

        pendingNavigationKey = selectedKey

        return CategoryNavigationRequest.Activate(
            currentKey = currentKey,
            selectedKey = selectedKey,
            searchQuery = searchQuery,
            categoryId =
                selectedKey
                    .takeIf { key -> key.startsWith(CATEGORY_PREFIX) }
                    ?.removePrefix(CATEGORY_PREFIX)
                    ?.toLongOrNull(),
            searchCached = searchQuery?.let(searchIsCached) ?: false
        )
    }

    fun finishNavigation(
        requestedKey: String,
        activatedState: PlaybackSessionState
    ): CategoryNavigationCompletion {
        val activatedKey = keyFor(activatedState)
        val succeeded =
            activatedKey.equals(requestedKey, ignoreCase = true)
        val nextDirection = queuedDirection

        pendingNavigationKey = null
        queuedDirection = null

        return CategoryNavigationCompletion(
            succeeded = succeeded,
            activatedKey = activatedKey,
            queuedDirection = nextDirection
        )
    }

    fun navigationKeys(
        permanentCategories: List<Category>,
        savedSearches: List<SavedSearchCategory>,
        categoryHasStations: (Long) -> Boolean
    ): List<String> {
        val permanent =
            permanentCategories.filter { category ->
                categoryHasStations(category.id)
            }

        val enabledSearches =
            savedSearches
                .filter { search -> search.navigationEnabled }
                .sortedBy { search -> search.sortOrder }

        return buildList {
            permanent.forEach { category ->
                add("$CATEGORY_PREFIX${category.id}")

                enabledSearches
                    .filter { search ->
                        search.anchorCategoryId == category.id
                    }
                    .forEach { search ->
                        add("$SEARCH_PREFIX${search.query}")
                    }
            }

            enabledSearches
                .filter { search ->
                    permanent.none { category ->
                        category.id == search.anchorCategoryId
                    }
                }
                .forEach { search ->
                    add("$SEARCH_PREFIX${search.query}")
                }
        }
    }

    private fun keyFor(state: PlaybackSessionState): String =
        if (state.isSearch) {
            "$SEARCH_PREFIX${state.categoryName}"
        } else {
            "$CATEGORY_PREFIX${state.categoryId}"
        }

    private companion object {
        const val CATEGORY_PREFIX = "category:"
        const val SEARCH_PREFIX = "search:"
    }
}