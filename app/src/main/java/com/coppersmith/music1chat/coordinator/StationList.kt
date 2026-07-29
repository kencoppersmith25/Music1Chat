package com.coppersmith.music1chat.coordinator

import androidx.compose.runtime.Composable
import com.coppersmith.music1chat.models.Station
import com.coppersmith.music1chat.ui.screens.StationListScreen

/**
 * Presentation coordinator for the station-list screen.
 *
 * This file deliberately owns no repository, persistence, playback, or session
 * behavior. MainScreen supplies those operations as callbacks so this extraction
 * cannot change application behavior.
 */
@Composable
fun StationList(
    stationListKey: String,
    categoryName: String,
    stations: List<Station>,
    currentCategoryId: Long?,
    currentStationId: Long?,
    currentSessionIsSearch: Boolean,
    effectiveSearchQuery: String,
    stateVersion: Int,
    onCloseClick: () -> Unit,
    onStationClick: (Station) -> Unit,
    onNavigationToggle: (Station) -> Unit,
    onMoveStation: (Station, Int) -> Unit,
    onDeleteStation: (Station) -> Unit
) {
    val selectedStationId =
        when {
            stationListKey.startsWith("search:") &&
                    currentSessionIsSearch &&
                    stationListKey.equals(
                        "search:$effectiveSearchQuery",
                        ignoreCase = true
                    ) -> currentStationId

            stationListKey == "category:$currentCategoryId" ->
                currentStationId

            else -> null
        }

    StationListScreen(
        categoryName = categoryName,
        stations = stations,
        selectedStationId = selectedStationId,
        reorderEnabled = !stationListKey.startsWith("search:"),
        stateVersion = stateVersion,
        onCloseClick = onCloseClick,
        onStationClick = onStationClick,
        onNavigationToggle = onNavigationToggle,
        onMoveStation = onMoveStation,
        onDeleteStation = onDeleteStation
    )
}