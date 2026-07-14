package com.coppersmith.music1chat.repository

import com.coppersmith.music1chat.models.Membership
import com.coppersmith.music1chat.models.Station

class MembershipRepository(
    private val stationRepository: StationRepository
) {

    private val memberships = mutableListOf<Membership>()

    fun getAll(): List<Membership> =
        memberships.toList()

    fun getMembershipsForCategory(
        categoryId: Long
    ): List<Membership> =
        memberships
            .filter { it.categoryId == categoryId }
            .sortedBy { it.position }

    fun getCategoryIdsForStation(
        stationId: Long
    ): List<Long> =
        memberships
            .filter { it.stationId == stationId }
            .map { it.categoryId }

    fun getStationsForCategory(
        categoryId: Long
    ): List<Station> =
        getMembershipsForCategory(categoryId)
            .mapNotNull { membership ->
                stationRepository.getById(membership.stationId)
            }

    fun getNavigationStationsForCategory(
        categoryId: Long
    ): List<Station> =
        getStationsForCategory(categoryId)
            .filter { station ->
                station.includedInNavigation &&
                        !station.failedThisSession
            }

    fun contains(
        categoryId: Long,
        stationId: Long
    ): Boolean =
        memberships.any {
            it.categoryId == categoryId &&
                    it.stationId == stationId
        }

    fun addStationToCategory(
        categoryId: Long,
        stationId: Long
    ): Boolean {

        if (stationRepository.getById(stationId) == null) {
            return false
        }

        if (contains(categoryId, stationId)) {
            return false
        }

        val nextPosition =
            getMembershipsForCategory(categoryId).size

        memberships.add(
            Membership(
                categoryId = categoryId,
                stationId = stationId,
                position = nextPosition
            )
        )

        return true
    }

    fun removeStationFromCategory(
        categoryId: Long,
        stationId: Long
    ) {
        memberships.removeAll {
            it.categoryId == categoryId &&
                    it.stationId == stationId
        }

        normalizePositions(categoryId)
    }

    fun removeCategory(categoryId: Long) {
        memberships.removeAll {
            it.categoryId == categoryId
        }
    }

    fun removeStation(stationId: Long) {
        val affectedCategoryIds =
            getCategoryIdsForStation(stationId).distinct()

        memberships.removeAll {
            it.stationId == stationId
        }

        affectedCategoryIds.forEach { categoryId ->
            normalizePositions(categoryId)
        }
    }

    fun moveStation(
        categoryId: Long,
        stationId: Long,
        newPosition: Int
    ) {
        val ordered =
            getMembershipsForCategory(categoryId).toMutableList()

        val membership =
            ordered.find { it.stationId == stationId } ?: return

        ordered.remove(membership)

        val safePosition =
            newPosition.coerceIn(0, ordered.size)

        ordered.add(safePosition, membership)

        ordered.forEachIndexed { index, item ->
            item.position = index
        }

        memberships.removeAll {
            it.categoryId == categoryId
        }

        memberships.addAll(ordered)
    }

    fun clear() {
        memberships.clear()
    }

    fun seedDefaults() {

        if (memberships.isNotEmpty()) {
            return
        }

        // Category 1 — Classical
        addStationToCategory(
            categoryId = 1,
            stationId = 3
        )

        addStationToCategory(
            categoryId = 1,
            stationId = 6
        )

        // Category 2 — Christian
        addStationToCategory(
            categoryId = 2,
            stationId = 2
        )

        // Category 3 — Jazz
        addStationToCategory(
            categoryId = 3,
            stationId = 4
        )

        // Category 4 — Alternative
        addStationToCategory(
            categoryId = 4,
            stationId = 5
        )

        // Category 5 — Hawaiian
        addStationToCategory(
            categoryId = 5,
            stationId = 1
        )
    }

    private fun normalizePositions(categoryId: Long) {
        getMembershipsForCategory(categoryId)
            .forEachIndexed { index, membership ->
                membership.position = index
            }
    }
}