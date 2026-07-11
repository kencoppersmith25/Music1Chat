package com.coppersmith.music1chat.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.coppersmith.music1chat.GenreData
import com.coppersmith.music1chat.RadioPlayer
import com.coppersmith.music1chat.StationData
import kotlinx.coroutines.delay
import kotlin.collections.plus
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

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

    val filteredGenres = remember(
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
        modifier = Modifier.Companion.fillMaxSize(),
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
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(
                        start = 14.dp,
                        end = 14.dp,
                        top = 10.dp,
                        bottom = 12.dp
                    ),
                horizontalAlignment = Alignment.Companion.CenterHorizontally
            ) {
                TopControlBar()

                Spacer(modifier = Modifier.Companion.height(14.dp))

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

                Spacer(modifier = Modifier.Companion.height(7.dp))

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

                Spacer(modifier = Modifier.Companion.height(13.dp))

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

                Spacer(modifier = Modifier.Companion.height(13.dp))

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

                Spacer(modifier = Modifier.Companion.height(12.dp))

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
                    Spacer(modifier = Modifier.Companion.height(8.dp))

                    Text(
                        text = message,
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Companion.Center
                    )
                }

                Spacer(modifier = Modifier.Companion.weight(1f))

                Text(
                    text = "Bluetooth controls ready",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun GenreSearchBox(
    searchText: String,
    filteredGenres: List<String>,
    showGenreMenu: Boolean,
    onSearchTextChanged: (String) -> Unit,
    onDropdownClick: () -> Unit,
    onSearchClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onGenreSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier.Companion.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChanged,
            modifier = Modifier.Companion.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            singleLine = true,
            placeholder = {
                Text(
                    text = "Search genres",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                Row(
                    verticalAlignment =
                        Alignment.Companion.CenterVertically
                ) {
                    IconButton(
                        onClick = onDropdownClick
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.ArrowDropDown,
                            contentDescription =
                                "Show genres",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier =
                                Modifier.Companion.size(34.dp)
                        )
                    }

                    IconButton(
                        onClick = onSearchClick
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier =
                                Modifier.Companion.size(28.dp)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Companion.Text,
                imeAction = ImeAction.Companion.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchClick()
                }
            ),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
        )

        DropdownMenu(
            expanded =
                showGenreMenu &&
                        filteredGenres.isNotEmpty(),
            onDismissRequest = onDismissMenu,
            properties =
                PopupProperties(focusable = false),
            modifier = Modifier.Companion
                .fillMaxWidth(0.92f)
                .heightIn(max = 520.dp)
        ) {
            filteredGenres.forEach { genre ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = genre,
                            fontSize = 18.sp
                        )
                    },
                    onClick = {
                        onGenreSelected(genre)
                    }
                )
            }
        }
    }
}

@Composable
private fun TopControlBar() {
    Row(
        modifier = Modifier.Companion.fillMaxWidth(),
        verticalAlignment =
            Alignment.Companion.CenterVertically
    ) {
        Text(
            text = "Music1Chat",
            modifier = Modifier.Companion.weight(1f),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 27.sp,
            fontWeight = FontWeight.Companion.Bold
        )

        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = "Bluetooth",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.Companion.size(30.dp)
            )
        }

        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.Companion.size(30.dp)
            )
        }

        IconButton(onClick = {}) {
            Icon(
                imageVector =
                    Icons.Default.PowerSettingsNew,
                contentDescription = "Exit",
                tint = Color(0xFFFF5A5F),
                modifier = Modifier.Companion.size(30.dp)
            )
        }
    }
}

@Composable
private fun CategoryCard(
    categoryName: String,
    includedInNavigation: Boolean,
    onNavigationToggle: () -> Unit,
    onCategoryClick: () -> Unit,
    onListClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.Companion.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(17.dp),
        colors =
            CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(
                    start = 18.dp,
                    end = 3.dp,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Text(
                text = categoryName,
                modifier = Modifier.Companion
                    .weight(1f)
                    .clickable(onClick = onCategoryClick)
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Companion.SemiBold,
                maxLines = 1
            )

            CardTrailingControls(
                includedInNavigation = includedInNavigation,
                onNavigationToggle = onNavigationToggle,
                iconContentDescription = "Station list",
                onIconClick = onListClick
            ) {
                Icon(
                    imageVector = Icons.Default.FormatListBulleted,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.Companion.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun CardTrailingControls(
    includedInNavigation: Boolean,
    onNavigationToggle: () -> Unit,
    iconContentDescription: String,
    onIconClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.Companion.width(90.dp),
        verticalAlignment = Alignment.Companion.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        NavigationIndicator(
            included = includedInNavigation,
            onClick = onNavigationToggle,
            modifier = Modifier.Companion
                .width(42.dp)
                .height(34.dp)
        )

        IconButton(
            onClick = onIconClick,
            modifier = Modifier.Companion.size(48.dp)
        ) {
            icon()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NowPlayingCard(
    stationName: String,
    stationGenre: String,
    stationNumber: Int,
    stationCount: Int,
    categoryIsSearch: Boolean,
    isPlaying: Boolean,
    includedInNavigation: Boolean,
    onNavigationToggle: () -> Unit,
    onSaveOrMoveClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showStationMenu by remember {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .height(174.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(
                    start = 14.dp,
                    end = 3.dp,
                    top = 12.dp,
                    bottom = 12.dp
                )
        ) {
            // The scrolling title now gets the full left side of the first row.
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                val scrollingTitle =
                    "$stationName  •  $stationGenre radio  •  Live now  •  $stationName"

                Text(
                    text = scrollingTitle,
                    modifier = Modifier.Companion
                        .weight(1f)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Companion.Bold,
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.Companion.width(90.dp),
                    verticalAlignment = Alignment.Companion.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    NavigationIndicator(
                        included = includedInNavigation,
                        onClick = onNavigationToggle,
                        modifier = Modifier.Companion
                            .width(42.dp)
                            .height(34.dp)
                    )

                    Box(
                        modifier = Modifier.Companion.size(48.dp),
                        contentAlignment = Alignment.Companion.Center
                    ) {
                        IconButton(
                            onClick = {
                                showStationMenu = true
                            },
                            modifier = Modifier.Companion.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Station menu",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.Companion.size(28.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showStationMenu,
                            onDismissRequest = {
                                showStationMenu = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (categoryIsSearch) {
                                            "Save to category"
                                        } else {
                                            "Move to category"
                                        }
                                    )
                                },
                                onClick = {
                                    showStationMenu = false
                                    onSaveOrMoveClick()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Copy to categories")
                                },
                                onClick = {
                                    showStationMenu = false
                                    onCopyClick()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Delete station",
                                        color = if (categoryIsSearch) {
                                            Color.Companion.Gray
                                        } else {
                                            Color.Companion.Unspecified
                                        }
                                    )
                                },
                                enabled = !categoryIsSearch,
                                onClick = {
                                    showStationMenu = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.Companion.height(10.dp))

            // Artwork begins below the title, aligned with the category text edge.
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                verticalAlignment = Alignment.Companion.Top
            ) {
                Box(
                    modifier = Modifier.Companion
                        .size(54.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Companion.Center
                ) {
                    Text(
                        text = "♫",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 29.sp
                    )
                }

                Spacer(modifier = Modifier.Companion.width(10.dp))

                Column(
                    modifier = Modifier.Companion
                        .weight(1f)
                        .padding(top = 2.dp)
                ) {
                    Text(
                        text = stationGenre,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.Companion.height(7.dp))

                    Row(
                        modifier = Modifier.Companion.fillMaxWidth(),
                        verticalAlignment = Alignment.Companion.CenterVertically
                    ) {
                        Text(
                            text = "Station $stationNumber of $stationCount",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.Companion.weight(1f))

                        if (isPlaying) {
                            MiniVuMeter(isPlaying = true)
                        } else {
                            Text(
                                text = "Ready",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Companion.SemiBold
                            )
                        }
                    }
                }
            }

        }
    }
}

private data class CategorySummary(
    val key: String,
    val name: String,
    val stationCount: Int,
    val includedInNavigation: Boolean
)

@Composable
private fun CategoryListScreen(
    categories: List<CategorySummary>,
    onBackClick: () -> Unit,
    onCategoryClick: (CategorySummary) -> Unit,
    onNavigationToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier.Companion
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = 8.dp,
                bottom = 12.dp
            )
    ) {
        Row(
            modifier = Modifier.Companion.fillMaxWidth(),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = "Categories",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 27.sp,
                fontWeight = FontWeight.Companion.Bold
            )
        }

        Spacer(modifier = Modifier.Companion.height(12.dp))

        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            categories.forEach { category ->
                OutlinedCard(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .clickable {
                            onCategoryClick(category)
                        },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(17.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .padding(
                                start = 17.dp,
                                end = 10.dp,
                                top = 12.dp,
                                bottom = 12.dp
                            ),
                        verticalAlignment = Alignment.Companion.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.Companion.weight(1f)
                        ) {
                            Text(
                                text = category.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Companion.SemiBold,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.Companion.height(3.dp))

                            Text(
                                text = "${category.stationCount} stations",
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }

                        NavigationIndicator(
                            included = category.includedInNavigation,
                            onClick = {
                                onNavigationToggle(category.key)
                            },
                            modifier = Modifier.Companion
                                .width(42.dp)
                                .height(34.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationIndicator(
    included: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.Companion
) {
    val enabledColor = Color(0xFF21A53A)
    val disabledColor = MaterialTheme.colorScheme.onSurface
    val routeColor by animateColorAsState(
        targetValue = if (included) enabledColor else disabledColor,
        animationSpec = tween(durationMillis = 180),
        label = "navigationRouteColor"
    )
    val redX = Color(0xFFE3262E)

    Canvas(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        val strokeWidth = 2.6.dp.toPx()
        val centerY = size.height * 0.62f

        val leftBaseX = size.width * 0.05f
        val leftTipX = size.width * 0.20f
        val leftHalfHeight = size.height * 0.12f

        val routeStartX = leftTipX + size.width * 0.03f
        val rightTipX = size.width * 0.94f
        val rightBaseX = size.width * 0.72f
        val rightHalfHeight = size.height * 0.19f

        val markCenterX = size.width * 0.52f
        val gapHalfWidth = size.width * 0.15f

        // Small starting arrow, pointing to the right.
        drawLine(
            color = routeColor,
            start = Offset(leftBaseX, centerY - leftHalfHeight),
            end = Offset(leftTipX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Companion.Round
        )
        drawLine(
            color = routeColor,
            start = Offset(leftBaseX, centerY + leftHalfHeight),
            end = Offset(leftTipX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Companion.Round
        )

        if (included) {
            drawLine(
                color = routeColor,
                start = Offset(routeStartX, centerY),
                end = Offset(rightBaseX, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Companion.Round
            )
        } else {
            drawLine(
                color = routeColor,
                start = Offset(routeStartX, centerY),
                end = Offset(markCenterX - gapHalfWidth, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Companion.Round
            )
            drawLine(
                color = routeColor,
                start = Offset(markCenterX + gapHalfWidth, centerY),
                end = Offset(rightBaseX, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Companion.Round
            )
        }

        // Larger destination arrow, also pointing to the right.
        drawLine(
            color = routeColor,
            start = Offset(rightBaseX, centerY),
            end = Offset(rightTipX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Companion.Round
        )
        drawLine(
            color = routeColor,
            start = Offset(rightTipX - size.width * 0.16f, centerY - rightHalfHeight),
            end = Offset(rightTipX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Companion.Round
        )
        drawLine(
            color = routeColor,
            start = Offset(rightTipX - size.width * 0.16f, centerY + rightHalfHeight),
            end = Offset(rightTipX, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Companion.Round
        )

        if (included) {
            val checkCenterX = markCenterX
            val checkCenterY = size.height * 0.24f
            drawLine(
                color = enabledColor,
                start = Offset(checkCenterX - size.width * 0.09f, checkCenterY),
                end = Offset(checkCenterX - size.width * 0.02f, checkCenterY + size.height * 0.08f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Companion.Round
            )
            drawLine(
                color = enabledColor,
                start = Offset(
                    checkCenterX - size.width * 0.02f,
                    checkCenterY + size.height * 0.08f
                ),
                end = Offset(checkCenterX + size.width * 0.11f, checkCenterY - size.height * 0.08f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Companion.Round
            )
        } else {
            val xHalfWidth = size.width * 0.11f
            val xHalfHeight = size.height * 0.14f
            drawLine(
                color = redX,
                start = Offset(markCenterX - xHalfWidth, centerY - xHalfHeight),
                end = Offset(markCenterX + xHalfWidth, centerY + xHalfHeight),
                strokeWidth = 3.2.dp.toPx(),
                cap = StrokeCap.Companion.Round
            )
            drawLine(
                color = redX,
                start = Offset(markCenterX - xHalfWidth, centerY + xHalfHeight),
                end = Offset(markCenterX + xHalfWidth, centerY - xHalfHeight),
                strokeWidth = 3.2.dp.toPx(),
                cap = StrokeCap.Companion.Round
            )
        }
    }
}

@Composable
private fun SearchChips(
    selectedSearch: String,
    onSearchSelected: (String) -> Unit
) {
    val searches = listOf(
        "Classical",
        "Hawaiian",
        "Jazz",
        "Rock",
        "News"
    )

    Row(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState()
            ),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        searches.forEach { search ->
            FilterChip(
                selected =
                    selectedSearch.equals(
                        search,
                        ignoreCase = true
                    ),
                onClick = {
                    onSearchSelected(search)
                },
                label = {
                    Text(search)
                }
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onPreviousCategoryClick: () -> Unit,
    onPreviousStationClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextStationClick: () -> Unit,
    onNextCategoryClick: () -> Unit
) {
    Row(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        verticalAlignment =
            Alignment.Companion.CenterVertically,
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        DirectionButton(
            symbol = "<<",
            onClick =
                onPreviousCategoryClick
        )

        DirectionButton(
            symbol = "<",
            onClick =
                onPreviousStationClick
        )

        Button(
            onClick = onPlayPauseClick,
            modifier = Modifier.Companion.size(96.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    if (isPlaying) {
                        Color(0xFFD71920)
                    } else {
                        Color(0xFF1D9A50)
                    }
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text =
                    if (isPlaying) "■" else "▶",
                color = Color.Companion.Black,
                fontSize =
                    if (isPlaying) {
                        38.sp
                    } else {
                        35.sp
                    },
                fontWeight = FontWeight.Companion.Bold
            )
        }

        DirectionButton(
            symbol = ">",
            onClick =
                onNextStationClick
        )

        DirectionButton(
            symbol = ">>",
            onClick =
                onNextCategoryClick
        )
    }
}

@Composable
private fun DirectionButton(
    symbol: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.Companion
            .width(44.dp)
            .height(96.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = symbol,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 35.sp,
            fontWeight = FontWeight.Companion.Bold,
            textAlign = TextAlign.Companion.Center,
            softWrap = false,
            maxLines = 1
        )
    }
}

@Composable
private fun MiniVuMeter(
    isPlaying: Boolean
) {
    var animationFrame by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            animationFrame = 0
            return@LaunchedEffect
        }

        while (true) {
            delay(150)
            animationFrame =
                (animationFrame + 1) % 8
        }
    }

    val patterns = listOf(
        listOf(
            0.18f,
            0.18f,
            0.18f,
            0.18f,
            0.18f
        ),
        listOf(
            0.30f,
            0.55f,
            0.85f,
            0.45f,
            0.25f
        ),
        listOf(
            0.55f,
            0.30f,
            0.65f,
            0.90f,
            0.40f
        ),
        listOf(
            0.25f,
            0.75f,
            0.45f,
            0.65f,
            0.85f
        ),
        listOf(
            0.70f,
            0.45f,
            0.90f,
            0.35f,
            0.60f
        ),
        listOf(
            0.40f,
            0.85f,
            0.55f,
            0.75f,
            0.30f
        ),
        listOf(
            0.80f,
            0.35f,
            0.70f,
            0.50f,
            0.90f
        ),
        listOf(
            0.45f,
            0.65f,
            0.30f,
            0.85f,
            0.55f
        )
    )

    val currentPattern =
        patterns[animationFrame]

    val vuBarColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = Modifier.Companion
            .width(34.dp)
            .height(18.dp)
    ) {
        val barCount =
            currentPattern.size

        val gap =
            2.dp.toPx()

        val availableWidth =
            size.width -
                    gap * (barCount - 1)

        val barWidth =
            availableWidth / barCount

        currentPattern.forEachIndexed { index,
                                        heightFraction ->

            val barHeight =
                size.height * heightFraction

            drawRoundRect(
                color = vuBarColor,
                topLeft = Offset(
                    x =
                        index *
                                (barWidth + gap),
                    y =
                        size.height -
                                barHeight
                ),
                size = Size(
                    width = barWidth,
                    height = barHeight
                ),
                cornerRadius =
                    CornerRadius(
                        x = 1.dp.toPx(),
                        y = 1.dp.toPx()
                    ),
                style = Fill
            )
        }
    }
}