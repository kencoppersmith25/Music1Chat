package com.coppersmith.music1chat.repository

import com.coppersmith.music1chat.models.SourceType
import com.coppersmith.music1chat.models.Station

class StationRepository {

    private val stations = mutableListOf<Station>()

    fun getAll(): List<Station> = stations.toList()

    fun getById(id: Long): Station? =
        stations.find { it.id == id }

    fun getByStreamUrl(streamUrl: String): Station? =
        stations.find {
            it.streamUrl.equals(streamUrl, ignoreCase = true)
        }

    fun getNavigationStations(): List<Station> =
        stations.filter {
            it.includedInNavigation && !it.failedThisSession
        }

    fun add(station: Station): Boolean {
        val duplicate = stations.any {
            it.id == station.id ||
                    it.streamUrl.equals(
                        station.streamUrl,
                        ignoreCase = true
                    )
        }

        if (duplicate) return false

        stations.add(station)
        return true
    }

    fun remove(stationId: Long) {
        stations.removeAll { it.id == stationId }
    }

    fun rename(stationId: Long, newName: String) {
        getById(stationId)?.name = newName
    }

    fun setNavigation(stationId: Long, enabled: Boolean) {
        getById(stationId)?.includedInNavigation = enabled
    }

    fun markFailed(stationId: Long) {
        getById(stationId)?.failedThisSession = true
    }

    fun clearFailed(stationId: Long) {
        getById(stationId)?.failedThisSession = false
    }

    fun clearAllFailedFlags() {
        stations.forEach {
            it.failedThisSession = false
        }
    }

    fun clear() {
        stations.clear()
    }

    fun seedDefaults() {
        if (stations.isNotEmpty()) return

        add(
            Station(
                id = 1,
                name = "KMNO Maui",
                streamUrl = "http://kmno.streamguys1.com/live",
                genre = "Hawaiian",
                city = "Maui",
                country = "United States",
                sourceType = SourceType.STREAM
            )
        )

        add(
            Station(
                id = 2,
                name = "99.1 JOY",
                streamUrl = "http://gateway.cdnstream1.com/2808_96.aac",
                genre = "Christian",
                country = "United States",
                sourceType = SourceType.STREAM
            )
        )

        add(
            Station(
                id = 3,
                name = "Classical King FM",
                streamUrl = "https://classicalking.streamguys1.com/king-fm-aac-128k",
                genre = "Classical",
                city = "Seattle",
                country = "United States",
                sourceType = SourceType.STREAM
            )
        )

        add(
            Station(
                id = 4,
                name = "WBGO Jazz 88.3",
                streamUrl = "https://ais-sa8.cdnstream1.com/3629_128.mp3",
                genre = "Jazz",
                city = "Newark",
                country = "United States",
                sourceType = SourceType.STREAM
            )
        )

        add(
            Station(
                id = 5,
                name = "KEXP Seattle",
                streamUrl = "https://kexp.streamguys1.com/kexp160.aac",
                genre = "Alternative",
                city = "Seattle",
                country = "United States",
                sourceType = SourceType.STREAM
            )
        )

        add(
            Station(
                id = 6,
                name = "KDFC Classical",
                streamUrl = "http://kdfc.streamguys1.com/kdfc-128.mp3",
                genre = "Classical",
                city = "San Francisco",
                country = "United States",
                sourceType = SourceType.STREAM
            )
        )
        add(
            Station(
                id = 7,
                name = "Classical Test Station",
                streamUrl = "https://classicalking.streamguys1.com/king-fm-aac-128k",
                genre = "Classical",
                city = "Test",
                country = "United States",
                sourceType = SourceType.STREAM
            )
        )
    }
}