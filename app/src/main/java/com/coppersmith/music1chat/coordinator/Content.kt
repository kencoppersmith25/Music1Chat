package com.coppersmith.music1chat.coordinator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coppersmith.music1chat.models.Station
import com.coppersmith.music1chat.ui.components.CategoryCard
import com.coppersmith.music1chat.ui.components.GenreSearchBox
import com.coppersmith.music1chat.ui.components.NowPlayingCard
import com.coppersmith.music1chat.ui.components.PlaybackControls
import com.coppersmith.music1chat.ui.components.SearchChips
import com.coppersmith.music1chat.ui.components.TopControlBar
import com.coppersmith.music1chat.ui.screens.SettingsScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.net.Uri


import com.coppersmith.music1chat.ui.components.AdBanner

@Composable
fun Content(
    showSettings: Boolean,
    searchResultLimit: Int,
    rideLogActive: Boolean,
    rideLogAvailable: Boolean,
    searchText: String,
    searchSuggestions: List<String>,
    showGenreMenu: Boolean,
    effectiveSearchQuery: String,
    searchFeedbackMessage: String?,
    effectiveCategoryDisplayName: String,
    categoryIncludedInNavigation: Boolean,
    displayedStation: Station?,
    songTitle: String,
    songArtist: String,
    artworkUri: Uri?,
    displayedStationIndex: Int,
    displayedStationCount: Int,
    categoryIsSearch: Boolean,
    isPlaying: Boolean,
    playbackRequested: Boolean,
    startupRestoreComplete: Boolean,
    libraryHasCategories: Boolean,
    sessionHasStations: Boolean,
    visibleStatusMessage: String?,
    onSettingsClick: () -> Unit,
    onPowerClick: () -> Unit,
    onSearchResultLimitChanged: (Int) -> Unit,
    onSettingsDismiss: () -> Unit,
    onRideLogToggle: () -> Unit,
    onShareRideLog: () -> Unit,
    onSearchTextChanged: (String) -> Unit,
    onSearchDropdownClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSearchMenuDismiss: () -> Unit,
    onSearchSuggestionSelected: (String) -> Unit,
    onSearchChipSelected: (String) -> Unit,
    onCategoryNavigationToggle: () -> Unit,
    onCategoryClick: () -> Unit,
    onCategoryListClick: () -> Unit,
    onCategoryDeleteClick: () -> Unit,
    onStationNavigationToggle: () -> Unit,
    onStationSaveOrMoveClick: () -> Unit,
    onStationCopyClick: () -> Unit,
    onStationDeleteClick: () -> Unit,
    onPreviousCategoryClick: () -> Unit,
    onPreviousStationClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextStationClick: () -> Unit,
    onNextCategoryClick: () -> Unit,
    recentSearches: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = 0.dp, // Reduced from 10.dp
                bottom = 12.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopControlBar(
            onSettingsClick = onSettingsClick,
            onPowerClick = onPowerClick
        )

        if (showSettings) {
            SettingsScreen(
                searchResultLimit = searchResultLimit,
                onSearchResultLimitChanged = onSearchResultLimitChanged,
                onDismiss = onSettingsDismiss
            )
        }

        var rideLogAutoStarted by remember {
            mutableStateOf(false)
        }

        LaunchedEffect(rideLogAvailable, rideLogActive) {
            if (
                rideLogAvailable &&
                !rideLogActive &&
                !rideLogAutoStarted
            ) {
                rideLogAutoStarted = true
                onRideLogToggle()
            }
        }


        Spacer(modifier = Modifier.height(4.dp)) // Reduced from 8.dp

        GenreSearchBox(
            searchText = searchText,
            filteredGenres = searchSuggestions,
            showGenreMenu = showGenreMenu,
            onSearchTextChanged = onSearchTextChanged,
            onDropdownClick = onSearchDropdownClick,
            onSearchClick = onSearchClick,
            onDismissMenu = onSearchMenuDismiss,
            onGenreSelected = onSearchSuggestionSelected
        )

        Spacer(modifier = Modifier.height(4.dp)) // Reduced from 7.dp

        SearchChips(
            selectedSearch = effectiveSearchQuery,
            onSearchSelected = onSearchChipSelected,
            recentSearches = recentSearches
        )

        searchFeedbackMessage?.let { message ->
            Spacer(modifier = Modifier.height(2.dp)) // Reduced from 5.dp
            Text(
                text = message,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(4.dp)) // Reduced from 8.dp

        CategoryCard(
            categoryName = effectiveCategoryDisplayName,
            includedInNavigation = categoryIncludedInNavigation,
            onNavigationToggle = onCategoryNavigationToggle,
            onCategoryClick = onCategoryClick,
            onListClick = onCategoryListClick,
            onDeleteClick = onCategoryDeleteClick
        )

        Spacer(modifier = Modifier.height(4.dp)) // Reduced from 8.dp

        if (displayedStation != null) {
            NowPlayingCard(
                stationName = displayedStation.name,
                stationGenre = displayedStation.genre,
                stationCallLetters = displayedStation.callLetters,
                stationCity = displayedStation.city,
                stationCountry = displayedStation.country,
                songTitle = songTitle,
                songArtist = songArtist,
                artworkUri = artworkUri,
                stationNumber = displayedStationIndex + 1,
                stationCount = displayedStationCount,
                categoryIsSearch = categoryIsSearch,
                isPlaying = isPlaying,
                includedInNavigation = displayedStation.includedInNavigation,
                onNavigationToggle = onStationNavigationToggle,
                onSaveOrMoveClick = onStationSaveOrMoveClick,
                onCopyClick = onStationCopyClick,
                onDeleteClick = onStationDeleteClick
            )
        } else {
            Text(
                text =
                    when {
                        !startupRestoreComplete -> ""
                        !libraryHasCategories ->
                            "No categories yet. Type in the Search box above to find stations and get started!"
                        else -> "No stations are available."
                    },
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(6.dp)) // Reduced from 12.dp

        PlaybackControls(
            isPlaying = playbackRequested,
            onPreviousCategoryClick = onPreviousCategoryClick,
            onPreviousStationClick = onPreviousStationClick,
            onPlayPauseClick = onPlayPauseClick,
            onNextStationClick = onNextStationClick,
            onNextCategoryClick = onNextCategoryClick
        )

        visibleStatusMessage?.let { message ->
            Spacer(modifier = Modifier.height(4.dp)) // Reduced from 8.dp
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        AdBanner()
    }
}
