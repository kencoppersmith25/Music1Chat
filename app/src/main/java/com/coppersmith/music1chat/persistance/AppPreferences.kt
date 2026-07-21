package com.coppersmith.music1chat.persistence

// Music1Chat coordinated release
// Release: 2026-07-16 v01
// Matched files: MainScreen, StationListScreen, AppPreferences

import android.content.Context
import com.coppersmith.music1chat.models.Category
import com.coppersmith.music1chat.models.CategoryType
import com.coppersmith.music1chat.models.SourceType
import com.coppersmith.music1chat.models.Station
import com.coppersmith.music1chat.repository.CategoryRepository
import com.coppersmith.music1chat.repository.MembershipRepository
import com.coppersmith.music1chat.repository.StationRepository
import org.json.JSONArray
import org.json.JSONObject

class AppPreferences(
    context: Context
) {
    fun loadSearchResultLimit(): Int {
        val savedLimit =
            preferences.getInt(
                KEY_SEARCH_RESULT_LIMIT,
                DEFAULT_SEARCH_RESULT_LIMIT
            )

        return normalizeSearchResultLimit(savedLimit)
    }

    fun saveSearchResultLimit(
        limit: Int
    ) {
        preferences.edit()
            .putInt(
                KEY_SEARCH_RESULT_LIMIT,
                normalizeSearchResultLimit(limit)
            )
            .apply()
    }

    private fun normalizeSearchResultLimit(
        limit: Int
    ): Int {
        val roundedLimit =
            ((limit + 2) / SEARCH_RESULT_LIMIT_STEP) *
                    SEARCH_RESULT_LIMIT_STEP

        return roundedLimit.coerceIn(
            MINIMUM_SEARCH_RESULT_LIMIT,
            MAXIMUM_SEARCH_RESULT_LIMIT
        )
    }

    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    fun restoreStationRepairs(
        stations: List<Station>
    ) {
        stations.forEach { station ->
            val keyPrefix = stationKeyPrefix(station.id)

            val resolvedUrl = preferences.getString(
                "${keyPrefix}_resolved_url",
                ""
            ).orEmpty()

            val verified = preferences.getBoolean(
                "${keyPrefix}_verified",
                false
            )

            val lastVerified = preferences.getLong(
                "${keyPrefix}_last_verified",
                0L
            )

            if (resolvedUrl.isNotBlank()) {
                station.resolvedStreamUrl = resolvedUrl
                station.streamVerified = verified
                station.lastVerified = lastVerified
            }
        }
    }

    fun saveStationRepair(
        station: Station
    ) {
        val keyPrefix = stationKeyPrefix(station.id)

        preferences.edit()
            .putString(
                "${keyPrefix}_resolved_url",
                station.resolvedStreamUrl
            )
            .putBoolean(
                "${keyPrefix}_verified",
                station.streamVerified
            )
            .putLong(
                "${keyPrefix}_last_verified",
                station.lastVerified
            )
            .apply()
    }

    fun loadPlaybackState(): SavedPlaybackState {
        val categoryId = preferences
            .getLong(KEY_CATEGORY_ID, NO_ID)
            .takeIf { it != NO_ID }

        val stationId = preferences
            .getLong(KEY_STATION_ID, NO_ID)
            .takeIf { it != NO_ID }

        return SavedPlaybackState(
            categoryId = categoryId,
            stationId = stationId,
            wasPlaying = preferences.getBoolean(
                KEY_WAS_PLAYING,
                false
            )
        )
    }

    fun savePlaybackState(
        categoryId: Long?,
        stationId: Long?,
        wasPlaying: Boolean
    ) {
        preferences.edit()
            .putLong(
                KEY_CATEGORY_ID,
                categoryId ?: NO_ID
            )
            .putLong(
                KEY_STATION_ID,
                stationId ?: NO_ID
            )
            .putBoolean(
                KEY_WAS_PLAYING,
                wasPlaying
            )
            .apply()
    }

    fun saveSelection(
        categoryId: Long?,
        stationId: Long?
    ) {
        preferences.edit()
            .putLong(
                KEY_CATEGORY_ID,
                categoryId ?: NO_ID
            )
            .putLong(
                KEY_STATION_ID,
                stationId ?: NO_ID
            )
            .apply()
    }

    fun saveWasPlaying(
        wasPlaying: Boolean
    ) {
        preferences.edit()
            .putBoolean(
                KEY_WAS_PLAYING,
                wasPlaying
            )
            .apply()
    }


    fun hasPermanentLibrary(): Boolean =
        preferences.getBoolean(
            KEY_PERMANENT_LIBRARY_INITIALIZED,
            false
        )

    fun restorePermanentLibrary(
        categoryRepository: CategoryRepository,
        stationRepository: StationRepository,
        membershipRepository: MembershipRepository
    ) {
        if (!hasPermanentLibrary()) return

        categoryRepository.clear()
        stationRepository.clear()
        membershipRepository.clear()

        val categoryJson =
            preferences.getString(
                KEY_PERMANENT_CATEGORIES_JSON,
                "[]"
            ).orEmpty()

        runCatching {
            val array = JSONArray(categoryJson)

            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)

                categoryRepository.add(
                    Category(
                        id = item.getLong("id"),
                        name = item.optString("name"),
                        type =
                            runCatching {
                                CategoryType.valueOf(
                                    item.optString(
                                        "type",
                                        CategoryType.STANDARD.name
                                    )
                                )
                            }.getOrDefault(CategoryType.STANDARD),
                        includedInNavigation =
                            item.optBoolean(
                                "includedInNavigation",
                                true
                            ),
                        sortOrder =
                            item.optInt("sortOrder", index),
                        lastRefresh =
                            item.optLong("lastRefresh", 0L)
                    )
                )
            }
        }

        val stationJson =
            preferences.getString(
                KEY_PERMANENT_STATIONS_JSON,
                "[]"
            ).orEmpty()

        runCatching {
            val array = JSONArray(stationJson)

            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)

                stationRepository.add(
                    Station(
                        id = item.getLong("id"),
                        name = item.optString("name"),
                        streamUrl = item.optString("streamUrl"),
                        genre = item.optString("genre"),
                        callLetters = item.optString("callLetters"),
                        city = item.optString("city"),
                        country = item.optString("country"),
                        logoUrl = item.optString("logoUrl"),
                        sourceType =
                            runCatching {
                                SourceType.valueOf(
                                    item.optString(
                                        "sourceType",
                                        SourceType.STREAM.name
                                    )
                                )
                            }.getOrDefault(SourceType.STREAM),
                        includedInNavigation =
                            item.optBoolean(
                                "includedInNavigation",
                                true
                            ),
                        failedThisSession = false,
                        resolvedStreamUrl =
                            item.optString("resolvedStreamUrl"),
                        streamVerified =
                            item.optBoolean(
                                "streamVerified",
                                false
                            ),
                        lastVerified =
                            item.optLong("lastVerified", 0L)
                    )
                )
            }
        }

        val membershipJson =
            preferences.getString(
                KEY_PERMANENT_MEMBERSHIPS_JSON,
                "[]"
            ).orEmpty()

        runCatching {
            val array = JSONArray(membershipJson)

            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val categoryId = item.getLong("categoryId")
                val stationId = item.getLong("stationId")
                val position = item.optInt("position", index)

                if (
                    categoryRepository.getById(categoryId) != null &&
                    stationRepository.getById(stationId) != null
                ) {
                    membershipRepository.addStationToCategory(
                        categoryId = categoryId,
                        stationId = stationId
                    )

                    membershipRepository.moveStation(
                        categoryId = categoryId,
                        stationId = stationId,
                        newPosition = position
                    )
                }
            }
        }

        stationRepository.clearAllFailedFlags()
    }

    fun savePermanentLibrary(
        categoryRepository: CategoryRepository,
        stationRepository: StationRepository,
        membershipRepository: MembershipRepository
    ) {
        val categoryArray = JSONArray()

        categoryRepository.getAll().forEach { category ->
            categoryArray.put(
                JSONObject()
                    .put("id", category.id)
                    .put("name", category.name)
                    .put("type", category.type.name)
                    .put(
                        "includedInNavigation",
                        category.includedInNavigation
                    )
                    .put("sortOrder", category.sortOrder)
                    .put("lastRefresh", category.lastRefresh)
            )
        }

        val stationArray = JSONArray()

        stationRepository.getAll().forEach { station ->
            stationArray.put(
                JSONObject()
                    .put("id", station.id)
                    .put("name", station.name)
                    .put("streamUrl", station.streamUrl)
                    .put("genre", station.genre)
                    .put("callLetters", station.callLetters)
                    .put("city", station.city)
                    .put("country", station.country)
                    .put("logoUrl", station.logoUrl)
                    .put("sourceType", station.sourceType.name)
                    .put(
                        "includedInNavigation",
                        station.includedInNavigation
                    )
                    .put(
                        "resolvedStreamUrl",
                        station.resolvedStreamUrl
                    )
                    .put(
                        "streamVerified",
                        station.streamVerified
                    )
                    .put("lastVerified", station.lastVerified)
            )
        }

        val membershipArray = JSONArray()

        membershipRepository.getAll()
            .sortedWith(
                compareBy(
                    { membership -> membership.categoryId },
                    { membership -> membership.position }
                )
            )
            .forEach { membership ->
                membershipArray.put(
                    JSONObject()
                        .put("categoryId", membership.categoryId)
                        .put("stationId", membership.stationId)
                        .put("position", membership.position)
                )
            }

        preferences.edit()
            .putBoolean(
                KEY_PERMANENT_LIBRARY_INITIALIZED,
                true
            )
            .putString(
                KEY_PERMANENT_CATEGORIES_JSON,
                categoryArray.toString()
            )
            .putString(
                KEY_PERMANENT_STATIONS_JSON,
                stationArray.toString()
            )
            .putString(
                KEY_PERMANENT_MEMBERSHIPS_JSON,
                membershipArray.toString()
            )
            .apply()
    }

    fun loadSearchCategories(): List<SavedSearchCategory> {
        val stored = preferences.getString(
            KEY_SEARCH_CATEGORIES_JSON,
            null
        )

        if (!stored.isNullOrBlank()) {
            return runCatching {
                val array = JSONArray(stored)

                buildList {
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        val query = item.optString("query").trim()

                        if (query.isNotBlank()) {
                            add(
                                SavedSearchCategory(
                                    query = query,
                                    anchorCategoryId =
                                        item.optLong(
                                            "anchorCategoryId",
                                            NO_ID
                                        ).takeIf { it != NO_ID },
                                    lastResultCount =
                                        item.optInt(
                                            "lastResultCount",
                                            0
                                        ),
                                    isCurrent =
                                        item.optBoolean(
                                            "isCurrent",
                                            false
                                        ),
                                    currentStationId =
                                        item.optLong(
                                            "currentStationId",
                                            NO_ID
                                        ).takeIf { it != NO_ID },
                                    currentIndex =
                                        item.optInt(
                                            "currentIndex",
                                            -1
                                        ).takeIf { it >= 0 },
                                    navigationEnabled =
                                        item.optBoolean(
                                            "navigationEnabled",
                                            true
                                        ),
                                    sortOrder =
                                        item.optInt(
                                            "sortOrder",
                                            index
                                        )
                                )
                            )
                        }
                    }
                }.sortedBy { it.sortOrder }
            }.getOrDefault(emptyList())
        }

        // One-time migration from the previous single-search format.
        val legacyQuery = preferences.getString(
            KEY_SEARCH_QUERY,
            ""
        ).orEmpty().trim()

        if (legacyQuery.isBlank()) {
            return emptyList()
        }

        val migrated =
            SavedSearchCategory(
                query = legacyQuery,
                anchorCategoryId =
                    preferences.getLong(
                        KEY_SEARCH_ANCHOR_CATEGORY_ID,
                        NO_ID
                    ).takeIf { it != NO_ID },
                lastResultCount =
                    preferences.getInt(
                        KEY_SEARCH_RESULT_COUNT,
                        0
                    ),
                isCurrent =
                    preferences.getBoolean(
                        KEY_SEARCH_IS_CURRENT,
                        false
                    ),
                currentStationId =
                    preferences.getLong(
                        KEY_SEARCH_STATION_ID,
                        NO_ID
                    ).takeIf { it != NO_ID },
                currentIndex =
                    preferences.getInt(
                        KEY_SEARCH_STATION_INDEX,
                        -1
                    ).takeIf { it >= 0 },
                navigationEnabled = true,
                sortOrder = 0
            )

        saveSearchCategories(listOf(migrated))
        clearLegacySearchKeys()

        return listOf(migrated)
    }

    fun upsertSearchCategory(
        category: SavedSearchCategory
    ): List<SavedSearchCategory> {
        val current = loadSearchCategories().toMutableList()
        val matchIndex = current.indexOfFirst {
            it.query.equals(
                category.query,
                ignoreCase = true
            )
        }

        val next =
            category.copy(
                query = category.query.trim(),
                sortOrder =
                    if (matchIndex >= 0) {
                        current[matchIndex].sortOrder
                    } else {
                        current.size
                    }
            )

        if (category.isCurrent) {
            for (index in current.indices) {
                current[index] =
                    current[index].copy(isCurrent = false)
            }
        }

        if (matchIndex >= 0) {
            current[matchIndex] = next
        } else {
            current.add(next)
        }

        val normalized =
            current
                .sortedBy { it.sortOrder }
                .mapIndexed { index, item ->
                    item.copy(sortOrder = index)
                }

        saveSearchCategories(normalized)
        return normalized
    }

    fun markCurrentSearch(
        query: String?
    ): List<SavedSearchCategory> {
        val updated =
            loadSearchCategories().map { item ->
                item.copy(
                    isCurrent =
                        query != null &&
                                item.query.equals(
                                    query,
                                    ignoreCase = true
                                )
                )
            }

        saveSearchCategories(updated)
        return updated
    }

    fun setSearchNavigation(
        query: String,
        enabled: Boolean
    ): List<SavedSearchCategory> {
        val updated =
            loadSearchCategories().map { item ->
                if (
                    item.query.equals(
                        query,
                        ignoreCase = true
                    )
                ) {
                    item.copy(
                        navigationEnabled = enabled
                    )
                } else {
                    item
                }
            }

        saveSearchCategories(updated)
        return updated
    }

    fun removeSearchCategory(
        query: String
    ): List<SavedSearchCategory> {
        val updated =
            loadSearchCategories()
                .filterNot { item ->
                    item.query.equals(
                        query,
                        ignoreCase = true
                    )
                }
                .mapIndexed { index, item ->
                    item.copy(sortOrder = index)
                }

        saveSearchCategories(updated)
        return updated
    }

    private fun saveSearchCategories(
        categories: List<SavedSearchCategory>
    ) {
        val array = JSONArray()

        categories
            .sortedBy { it.sortOrder }
            .forEach { item ->
                array.put(
                    JSONObject()
                        .put("query", item.query)
                        .put(
                            "anchorCategoryId",
                            item.anchorCategoryId ?: NO_ID
                        )
                        .put(
                            "lastResultCount",
                            item.lastResultCount
                        )
                        .put(
                            "isCurrent",
                            item.isCurrent
                        )
                        .put(
                            "currentStationId",
                            item.currentStationId ?: NO_ID
                        )
                        .put(
                            "currentIndex",
                            item.currentIndex ?: -1
                        )
                        .put(
                            "navigationEnabled",
                            item.navigationEnabled
                        )
                        .put(
                            "sortOrder",
                            item.sortOrder
                        )
                )
            }

        preferences.edit()
            .putString(
                KEY_SEARCH_CATEGORIES_JSON,
                array.toString()
            )
            .apply()
    }

    fun getSearchResultLimit(): Int =
        preferences.getInt(
            KEY_SEARCH_RESULT_LIMIT,
            DEFAULT_SEARCH_RESULT_LIMIT
        ).coerceIn(
            MIN_SEARCH_RESULT_LIMIT,
            MAX_SEARCH_RESULT_LIMIT
        )

    fun setSearchResultLimit(
        limit: Int
    ) {
        preferences.edit()
            .putInt(
                KEY_SEARCH_RESULT_LIMIT,
                limit.coerceIn(
                    MIN_SEARCH_RESULT_LIMIT,
                    MAX_SEARCH_RESULT_LIMIT
                )
            )
            .apply()
    }

    private fun clearLegacySearchKeys() {
        preferences.edit()
            .remove(KEY_SEARCH_QUERY)
            .remove(KEY_SEARCH_ANCHOR_CATEGORY_ID)
            .remove(KEY_SEARCH_RESULT_COUNT)
            .remove(KEY_SEARCH_IS_CURRENT)
            .remove(KEY_SEARCH_STATION_ID)
            .remove(KEY_SEARCH_STATION_INDEX)
            .apply()
    }

    private fun stationKeyPrefix(
        stationId: Long
    ): String = "station_$stationId"

    companion object {
        private const val PREFERENCES_NAME =
            "music1chat_preferences"

        private const val KEY_SEARCH_RESULT_LIMIT =
            "search_result_limit"

        private const val DEFAULT_SEARCH_RESULT_LIMIT = 10
        private const val MINIMUM_SEARCH_RESULT_LIMIT = 5
        private const val MAXIMUM_SEARCH_RESULT_LIMIT = 100
        private const val SEARCH_RESULT_LIMIT_STEP = 5
        private const val KEY_CATEGORY_ID =
            "current_category_id"

        private const val KEY_STATION_ID =
            "current_station_id"

        private const val KEY_WAS_PLAYING =
            "was_playing"

        private const val KEY_SEARCH_CATEGORIES_JSON =
            "search_categories_json"

        const val MIN_SEARCH_RESULT_LIMIT = 5
        const val MAX_SEARCH_RESULT_LIMIT = 100
        private const val KEY_PERMANENT_LIBRARY_INITIALIZED =
            "permanent_library_initialized"

        private const val KEY_PERMANENT_CATEGORIES_JSON =
            "permanent_categories_json"

        private const val KEY_PERMANENT_STATIONS_JSON =
            "permanent_stations_json"

        private const val KEY_PERMANENT_MEMBERSHIPS_JSON =
            "permanent_memberships_json"


        // Legacy single-search keys retained only for migration.
        private const val KEY_SEARCH_QUERY =
            "search_category_query"

        private const val KEY_SEARCH_ANCHOR_CATEGORY_ID =
            "search_category_anchor_id"

        private const val KEY_SEARCH_RESULT_COUNT =
            "search_category_result_count"

        private const val KEY_SEARCH_IS_CURRENT =
            "search_category_is_current"

        private const val KEY_SEARCH_STATION_ID =
            "search_category_station_id"

        private const val KEY_SEARCH_STATION_INDEX =
            "search_category_station_index"

        private const val NO_ID = -1L
    }
}

data class SavedPlaybackState(
    val categoryId: Long?,
    val stationId: Long?,
    val wasPlaying: Boolean
)

data class SavedSearchCategory(
    val query: String,
    val anchorCategoryId: Long?,
    val lastResultCount: Int,
    val isCurrent: Boolean,
    val currentStationId: Long?,
    val currentIndex: Int?,
    val navigationEnabled: Boolean = true,
    val sortOrder: Int = 0
)