package com.coppersmith.music1chat.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coppersmith.music1chat.GenreData
import com.coppersmith.music1chat.RadioPlayer
import com.coppersmith.music1chat.navigation.NavigationCommand
import com.coppersmith.music1chat.navigation.NavigationEngine
import com.coppersmith.music1chat.navigation.NavigationResult
import com.coppersmith.music1chat.navigation.NavigationState
import com.coppersmith.music1chat.persistence.AppPreferences
import com.coppersmith.music1chat.repository.MusicRepository
import com.coppersmith.music1chat.ui.components.CategoryCard
import com.coppersmith.music1chat.ui.components.GenreSearchBox
import com.coppersmith.music1chat.ui.components.NowPlayingCard
import com.coppersmith.music1chat.ui.components.PlaybackControls
import com.coppersmith.music1chat.ui.components.SearchChips
import com.coppersmith.music1chat.ui.components.TopControlBar

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val musicRepository = remember {
        MusicRepository()
    }

    val appPreferences = remember {
        AppPreferences(context.applicationContext)
    }

    val repositoryCategories =
        musicRepository.categories.getAll()

    val repositoryStations =
        musicRepository.stations.getAll()

    val membershipRepository =
        musicRepository.memberships

    val savedPlaybackState = remember {
        appPreferences.restoreStationRepairs(
            repositoryStations
        )

        appPreferences.loadPlaybackState()
    }

    val firstPlayableCategory =
        musicRepository.categories
            .getNavigationCategories()
            .firstOrNull { category ->
                membershipRepository
                    .getNavigationStationsForCategory(category.id)
                    .isNotEmpty()
            }

    val firstPlayableStation =
        firstPlayableCategory?.let { category ->
            membershipRepository
                .getNavigationStationsForCategory(category.id)
                .firstOrNull()
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

    val initialCategoryId =
        if (savedSelectionIsValid) {
            savedCategory?.id
        } else {
            firstPlayableCategory?.id
        }

    val initialStationId =
        if (savedSelectionIsValid) {
            savedStation?.id
        } else {
            firstPlayableStation?.id
        }

    val shouldResumePlayback =
        savedSelectionIsValid &&
                savedPlaybackState.wasPlaying

    val radioPlayer = remember {
        RadioPlayer(context.applicationContext)
    }

    val navigationEngine = remember {
        NavigationEngine(
            categoryRepository = musicRepository.categories,
            stationRepository = musicRepository.stations,
            membershipRepository = membershipRepository,
            initialState = NavigationState(
                currentCategoryId = initialCategoryId,
                currentStationId = initialStationId,
                isPlaying = shouldResumePlayback
            )
        )
    }

    var navigationState by remember {
        mutableStateOf(
            navigationEngine.getState()
        )
    }

    var navigationStatusMessage by remember {
        mutableStateOf<String?>(null)
    }

    var stationStateVersion by remember {
        mutableIntStateOf(0)
    }

    var showCategoryList by remember {
        mutableStateOf(false)
    }

    var categoryIncludedInNavigation by remember {
        mutableStateOf(true)
    }

    var savedCategoryNavigationStates by remember {
        mutableStateOf(
            mapOf(
                "Classical" to true,
                "Jazz" to true,
                "Bike Ride" to false,
                "Morning Drive" to true
            )
        )
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var activeCategory by remember {
        mutableStateOf(
            initialCategoryId?.let { categoryId ->
                musicRepository.categories
                    .getById(categoryId)
                    ?.name
            }.orEmpty()
        )
    }

    var activeCategoryIsSearch by remember {
        mutableStateOf(false)
    }

    var showGenreMenu by remember {
        mutableStateOf(false)
    }

    val genres = GenreData.MAJOR_GENRES

    stationStateVersion

    val navigationStations =
        musicRepository.stations.getNavigationStations()

    val currentStation =
        navigationState.currentStationId?.let { stationId ->
            musicRepository.stations.getById(stationId)
        }

    val currentCategory =
        navigationState.currentCategoryId?.let { categoryId ->
            musicRepository.categories.getById(categoryId)
        }

    val currentCategoryStations =
        navigationState.currentCategoryId?.let { categoryId ->
            membershipRepository
                .getNavigationStationsForCategory(categoryId)
        } ?: emptyList()

    val currentStationIndex =
        currentCategoryStations
            .indexOfFirst { station ->
                station.id == navigationState.currentStationId
            }
            .takeIf { index ->
                index >= 0
            } ?: 0

    val isPlaying = radioPlayer.isPlaying

    val filteredGenres: List<String> = remember(
        searchText,
        genres
    ) {
        val typedText = searchText.trim()

        if (typedText.isBlank()) {
            genres
        } else {
            genres.filter { genre ->
                genre.contains(
                    typedText,
                    ignoreCase = true
                )
            }
        }
    }

    fun saveCurrentState(
        state: NavigationState
    ) {
        appPreferences.savePlaybackState(
            categoryId = state.currentCategoryId,
            stationId = state.currentStationId,
            wasPlaying = state.isPlaying
        )
    }

    fun applyNavigationResult(
        result: NavigationResult
    ) {
        navigationState = result.state
        navigationStatusMessage = result.statusMessage

        saveCurrentState(result.state)

        val selectedStation =
            result.state.currentStationId?.let { stationId ->
                musicRepository.stations.getById(stationId)
            }

        val selectedCategory =
            result.state.currentCategoryId?.let { categoryId ->
                musicRepository.categories.getById(categoryId)
            }

        if (selectedCategory != null) {
            activeCategory = selectedCategory.name
            activeCategoryIsSearch = false
        }

        if (
            selectedStation != null &&
            result.shouldStartPlayback
        ) {
            radioPlayer.play(selectedStation)
        }
    }

    fun executeNavigationCommand(
        command: NavigationCommand
    ) {
        applyNavigationResult(
            navigationEngine.execute(command)
        )
    }

    val submitSearch: () -> Unit = {
        val typedText = searchText.trim()

        val exactMatch =
            genres.firstOrNull { genre ->
                genre.equals(
                    typedText,
                    ignoreCase = true
                )
            }

        val selectedSearch =
            exactMatch
                ?: filteredGenres.firstOrNull()
                ?: typedText

        if (selectedSearch.isNotBlank()) {
            activeCategory = selectedSearch
            activeCategoryIsSearch = true
            searchText = ""
            showGenreMenu = false
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(Unit) {
        saveCurrentState(navigationState)

        if (
            shouldResumePlayback &&
            currentStation != null
        ) {
            radioPlayer.play(currentStation)
        }
    }

    DisposableEffect(
        radioPlayer,
        navigationEngine
    ) {
        radioPlayer.onStationFailed = {
            executeNavigationCommand(
                NavigationCommand.NEXT_STATION
            )
        }

        onDispose {
            saveCurrentState(
                navigationEngine.getState()
            )

            radioPlayer.onStationFailed = null
            radioPlayer.release()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (showCategoryList) {
            val categoryRows = listOf(
                CategorySummary(
                    key = "current_search",
                    name = "Search: $activeCategory",
                    stationCount = navigationStations.size,
                    includedInNavigation =
                        categoryIncludedInNavigation
                ),
                CategorySummary(
                    key = "Classical",
                    name = "Classical",
                    stationCount = 247,
                    includedInNavigation =
                        savedCategoryNavigationStates["Classical"] == true
                ),
                CategorySummary(
                    key = "Jazz",
                    name = "Jazz",
                    stationCount = 183,
                    includedInNavigation =
                        savedCategoryNavigationStates["Jazz"] == true
                ),
                CategorySummary(
                    key = "Bike Ride",
                    name = "Bike Ride",
                    stationCount = 42,
                    includedInNavigation =
                        savedCategoryNavigationStates["Bike Ride"] == true
                ),
                CategorySummary(
                    key = "Morning Drive",
                    name = "Morning Drive",
                    stationCount = 31,
                    includedInNavigation =
                        savedCategoryNavigationStates["Morning Drive"] == true
                )
            )

            CategoryListScreen(
                categories = categoryRows,
                onBackClick = {
                    showCategoryList = false
                },
                onCategoryClick = { _ ->
                },
                onNavigationToggle = { categoryKey ->
                    if (categoryKey == "current_search") {
                        categoryIncludedInNavigation =
                            !categoryIncludedInNavigation
                    } else {
                        val currentValue =
                            savedCategoryNavigationStates[categoryKey] == true

                        savedCategoryNavigationStates =
                            savedCategoryNavigationStates +
                                    (categoryKey to !currentValue)
                    }
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(
                        start = 14.dp,
                        end = 14.dp,
                        top = 10.dp,
                        bottom = 12.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopControlBar()

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                GenreSearchBox(
                    searchText = searchText,
                    filteredGenres = filteredGenres,
                    showGenreMenu = showGenreMenu,
                    onSearchTextChanged = { newText ->
                        searchText = newText
                        showGenreMenu = true
                    },
                    onDropdownClick = {
                        showGenreMenu = !showGenreMenu
                    },
                    onSearchClick = submitSearch,
                    onDismissMenu = {
                        showGenreMenu = false
                    },
                    onGenreSelected = { genre ->
                        activeCategory = genre
                        activeCategoryIsSearch = true
                        searchText = ""
                        showGenreMenu = false
                        focusManager.clearFocus()
                    }
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                SearchChips(
                    selectedSearch = activeCategory,
                    onSearchSelected = { genre ->
                        activeCategory = genre
                        activeCategoryIsSearch = true
                        searchText = ""
                        showGenreMenu = false
                        focusManager.clearFocus()
                    }
                )

                Spacer(
                    modifier = Modifier.height(13.dp)
                )

                CategoryCard(
                    categoryName =
                        if (activeCategoryIsSearch) {
                            "Search: $activeCategory"
                        } else {
                            currentCategory?.name
                                ?: activeCategory
                        },
                    includedInNavigation =
                        categoryIncludedInNavigation,
                    onNavigationToggle = {
                        categoryIncludedInNavigation =
                            !categoryIncludedInNavigation
                    },
                    onCategoryClick = {
                        showCategoryList = true
                    },
                    onListClick = {
                        showCategoryList = true
                    }
                )

                Spacer(
                    modifier = Modifier.height(13.dp)
                )

                if (currentStation != null) {
                    NowPlayingCard(
                        stationName = currentStation.name,
                        stationGenre = currentStation.genre,
                        stationNumber = currentStationIndex + 1,
                        stationCount =
                            currentCategoryStations.size,
                        categoryIsSearch =
                            activeCategoryIsSearch,
                        isPlaying = isPlaying,
                        includedInNavigation =
                            currentStation.includedInNavigation,
                        onNavigationToggle = {
                            currentStation.includedInNavigation =
                                !currentStation.includedInNavigation

                            stationStateVersion++
                        },
                        onSaveOrMoveClick = {
                        },
                        onCopyClick = {
                        },
                        onDeleteClick = {
                        }
                    )
                } else {
                    Text(
                        text = "No stations are available.",
                        color =
                            MaterialTheme.colorScheme.onBackground,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                PlaybackControls(
                    isPlaying = isPlaying,
                    onPreviousCategoryClick = {
                        executeNavigationCommand(
                            NavigationCommand.PREVIOUS_CATEGORY
                        )
                    },
                    onPreviousStationClick = {
                        executeNavigationCommand(
                            NavigationCommand.PREVIOUS_STATION
                        )
                    },
                    onPlayPauseClick = {
                        if (isPlaying) {
                            radioPlayer.stop()

                            executeNavigationCommand(
                                NavigationCommand.STOP
                            )
                        } else {
                            executeNavigationCommand(
                                NavigationCommand.PLAY
                            )
                        }
                    },
                    onNextStationClick = {
                        executeNavigationCommand(
                            NavigationCommand.NEXT_STATION
                        )
                    },
                    onNextCategoryClick = {
                        executeNavigationCommand(
                            NavigationCommand.NEXT_CATEGORY
                        )
                    }
                )

                val visibleStatusMessage =
                    radioPlayer.errorMessage
                        ?: navigationStatusMessage

                visibleStatusMessage?.let { message ->
                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = message,
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "Bluetooth controls ready",
                    color =
                        MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp
                )

                Text(
                    text =
                        "Repository: " +
                                "${repositoryCategories.size} categories, " +
                                "${repositoryStations.size} stations",
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}