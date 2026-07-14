package com.coppersmith.music1chat.persistence

import android.content.Context
import com.coppersmith.music1chat.models.Station

class AppPreferences(
    context: Context
) {
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

            station.resolvedStreamUrl =
                preferences.getString(
                    "${keyPrefix}_resolved_url",
                    station.resolvedStreamUrl
                ).orEmpty()

            station.streamVerified =
                preferences.getBoolean(
                    "${keyPrefix}_verified",
                    station.streamVerified
                )

            station.lastVerified =
                preferences.getLong(
                    "${keyPrefix}_last_verified",
                    station.lastVerified
                )
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
            .commit()
    }

    fun loadPlaybackState(): SavedPlaybackState {
        val categoryId =
            preferences
                .getLong(KEY_CATEGORY_ID, NO_ID)
                .takeIf { savedId ->
                    savedId != NO_ID
                }

        val stationId =
            preferences
                .getLong(KEY_STATION_ID, NO_ID)
                .takeIf { savedId ->
                    savedId != NO_ID
                }

        return SavedPlaybackState(
            categoryId = categoryId,
            stationId = stationId,
            wasPlaying =
                preferences.getBoolean(
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
            .commit()
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
            .commit()
    }

    fun saveWasPlaying(
        wasPlaying: Boolean
    ) {
        preferences.edit()
            .putBoolean(
                KEY_WAS_PLAYING,
                wasPlaying
            )
            .commit()
    }

    private fun stationKeyPrefix(
        stationId: Long
    ): String {
        return "station_$stationId"
    }

    companion object {
        private const val PREFERENCES_NAME =
            "music1chat_preferences"

        private const val KEY_CATEGORY_ID =
            "current_category_id"

        private const val KEY_STATION_ID =
            "current_station_id"

        private const val KEY_WAS_PLAYING =
            "was_playing"

        private const val NO_ID = -1L
    }
}

data class SavedPlaybackState(
    val categoryId: Long?,
    val stationId: Long?,
    val wasPlaying: Boolean
)
