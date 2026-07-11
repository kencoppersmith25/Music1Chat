package com.coppersmith.music1chat.repository

class MusicRepository {

    val categories = CategoryRepository()

    val stations = StationRepository()

    val memberships = MembershipRepository(
        stationRepository = stations
    )

    init {
        seedDevelopmentData()
    }

    private fun seedDevelopmentData() {
        categories.seedDefaults()
        stations.seedDefaults()
        memberships.seedDefaults()

        stations.clearAllFailedFlags()
    }
}