package com.coppersmith.music1chat.ui.screens

import com.coppersmith.music1chat.GenreData
import com.coppersmith.music1chat.RadioPlayer
import com.coppersmith.music1chat.StationData
import com.coppersmith.music1chat.ui.components.CategoryCard
import com.coppersmith.music1chat.ui.components.GenreSearchBox
import com.coppersmith.music1chat.ui.components.NowPlayingCard
import com.coppersmith.music1chat.ui.components.PlaybackControls
import com.coppersmith.music1chat.ui.components.SearchChips
import com.coppersmith.music1chat.ui.components.TopControlBar
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import com.coppersmith.music1chat.repository.MusicRepository


@Composable
fun MainScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val musicRepository = remember {
        MusicRepository()
    }
    val repositoryCategories = musicRepository.categories.getAll()
    val repositoryStations = musicRepository.stations.getAll()

    val radioPlayer = remember {
        RadioPlayer(context.applicationContext)
    }

    val stations = StationData.RELIABLE_STATIONS

    var currentStationIndex by remember {
        mutableStateOf(0)
    }

    val currentStation = stations[currentStationIndex]
    val isPlaying = radioPlayer.isPlaying

    var showCategoryList by remember {
        mutableStateOf(false)
    }

    var categoryIncludedInNavigation by remember {
        mutableStateOf(true)
    }

    var stationIncludedInNavigation by remember {
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
        mutableStateOf(currentStation.genre)
    }

    var activeCategoryIsSearch by remember {
        mutableStateOf(true)
    }

    var showGenreMenu by remember {
        mutableStateOf(false)
    }

    val genres = GenreData.MAJOR_GENRES

    val filteredGenres: List<String> = remember(
        searchText,
        genres
    ) {
        val typedText = searchText.trim()

        if (typedText.isBlank()) {
            genres
        } else {
            genres.filter { genre: String ->
                genre.contains(
                    typedText,
                    ignoreCase = true
                )
            }
        }
    }

    val submitSearch: () -> Unit = {
        val typedText = searchText.trim()

        val exactMatch = genres.firstOrNull {
            it.equals(
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

    DisposableEffect(radioPlayer) {
        onDispose {
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
                    stationCount = stations.size,
                    includedInNavigation = categoryIncludedInNavigation
                ),
                CategorySummary(
                    key = "Classical",
                    name = "Classical",
                    stationCount = 247,
                    includedInNavigation = savedCategoryNavigationStates["Classical"] == true
                ),
                CategorySummary(
                    key = "Jazz",
                    name = "Jazz",
                    stationCount = 183,
                    includedInNavigation = savedCategoryNavigationStates["Jazz"] == true
                ),
                CategorySummary(
                    key = "Bike Ride",
                    name = "Bike Ride",
                    stationCount = 42,
                    includedInNavigation = savedCategoryNavigationStates["Bike Ride"] == true
                ),
                CategorySummary(
                    key = "Morning Drive",
                    name = "Morning Drive",
                    stationCount = 31,
                    includedInNavigation = savedCategoryNavigationStates["Morning Drive"] == true
                )
            )

            CategoryListScreen(
                categories = categoryRows,
                onBackClick = {
                    showCategoryList = false
                },
                onCategoryClick = { _ ->
                    // Station-list navigation comes next.
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

                Spacer(modifier = Modifier.height(14.dp))

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

                Spacer(modifier = Modifier.height(7.dp))

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

                Spacer(modifier = Modifier.height(13.dp))

                CategoryCard(
                    categoryName = if (activeCategoryIsSearch) {
                        "Search: $activeCategory"
                    } else {
                        activeCategory
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

                Spacer(modifier = Modifier.height(13.dp))

                NowPlayingCard(
                    stationName = currentStation.name,
                    stationGenre = currentStation.genre,
                    stationNumber = currentStationIndex + 1,
                    stationCount = stations.size,
                    categoryIsSearch = activeCategoryIsSearch,
                    isPlaying = isPlaying,
                    includedInNavigation =
                        stationIncludedInNavigation,
                    onNavigationToggle = {
                        stationIncludedInNavigation =
                            !stationIncludedInNavigation
                    },
                    onSaveOrMoveClick = {
                    },
                    onCopyClick = {
                    },
                    onDeleteClick = {
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                PlaybackControls(
                    isPlaying = isPlaying,
                    onPreviousCategoryClick = {
                    },
                    onPreviousStationClick = {
                        currentStationIndex =
                            if (currentStationIndex == 0) {
                                stations.lastIndex
                            } else {
                                currentStationIndex - 1
                            }

                        val newStation =
                            stations[currentStationIndex]

                        activeCategory =
                            newStation.genre

                        if (isPlaying) {
                            radioPlayer.play(newStation)
                        }
                    },
                    onPlayPauseClick = {
                        if (isPlaying) {
                            radioPlayer.stop()
                        } else {
                            radioPlayer.play(currentStation)
                        }
                    },
                    onNextStationClick = {
                        currentStationIndex =
                            if (
                                currentStationIndex ==
                                stations.lastIndex
                            ) {
                                0
                            } else {
                                currentStationIndex + 1
                            }

                        val newStation =
                            stations[currentStationIndex]

                        activeCategory =
                            newStation.genre

                        if (isPlaying) {
                            radioPlayer.play(newStation)
                        }
                    },
                    onNextCategoryClick = {
                    }
                )

                radioPlayer.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = message,
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Bluetooth controls ready",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp
                )

                Text(
                    text =
                        "Repository: ${repositoryCategories.size} categories, " +
                                "${repositoryStations.size} stations",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}