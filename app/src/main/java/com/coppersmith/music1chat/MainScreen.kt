package com.coppersmith.music1chat


import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.drawscope.Fill
import kotlinx.coroutines.delay
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Search


@Composable
fun MainScreen() {
    var isPlaying by remember { mutableStateOf(true) }
    var categoryIsFavorite by remember { mutableStateOf(true) }
    var stationIsFavorite by remember { mutableStateOf(true) }

    var searchText by remember { mutableStateOf("") }
    var activeCategory by remember { mutableStateOf("Classical") }
    var activeCategoryIsSearch by remember { mutableStateOf(true) }
    var showGenreMenu by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val genres = GenreData.MAJOR_GENRES

    val filteredGenres = remember(searchText, genres) {
        val typedText = searchText.trim()

        when {
            typedText.isBlank() -> genres
            else -> genres.filter { genre ->
                genre.contains(typedText, ignoreCase = true)
            }
        }
    }

    val submitSearch: () -> Unit = {
        val typedText = searchText.trim()

        val exactMatch = genres.firstOrNull {
            it.equals(typedText, ignoreCase = true)
        }

        val selectedSearch = exactMatch
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

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF101418)
        ) {
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
                    isFavorite = categoryIsFavorite,
                    onFavoriteClick = {
                        categoryIsFavorite = !categoryIsFavorite
                    },
                    onCategoryClick = {
                        // Later: open the alphabetical category screen.
                    },
                    onListClick = {
                        // Search category: open all categories.
                        // Fixed category: open that category's ordered station list.
                    }
                )

                Spacer(modifier = Modifier.height(13.dp))

                NowPlayingCard(
                    categoryIsSearch = activeCategoryIsSearch,
                    isPlaying = isPlaying,
                    isFavorite = stationIsFavorite,
                    onFavoriteClick = {
                        stationIsFavorite = !stationIsFavorite
                    },
                    onSaveOrMoveClick = {
                        // Search result: save to a category.
                        // Fixed category: move to another category.
                    },
                    onCopyClick = {
                        // Later: choose one or more destination categories.
                    },
                    onDeleteClick = {
                        // Later: confirm station deletion.
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                PlaybackControls(
                    isPlaying = isPlaying,
                    onPlayPauseClick = {
                        isPlaying = !isPlaying
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Bluetooth controls ready",
                    color = Color.LightGray,
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
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    text = "Search genres",
                    color = Color(0xFF77828A)
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDropdownClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Genres",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { onSearchClick() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Color(0xFF8BBEFF),
                unfocusedBorderColor = Color(0xFF66737D)
            )
        )

        DropdownMenu(
            expanded = showGenreMenu && filteredGenres.isNotEmpty(),
            onDismissRequest = onDismissMenu,
            properties = PopupProperties(focusable = false),
            modifier = Modifier
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Music1Chat",
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Default.Cast,
                contentDescription = "Cast",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Exit",
                tint = Color(0xFFFF5A5F),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun CategoryCard(
    categoryName: String,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onListClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color(0xFF161D22)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 18.dp,
                    end = 3.dp,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = categoryName,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onCategoryClick)
                    .padding(vertical = 8.dp),
                color = Color(0xFFB8D8FF),
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            IconButton(onClick = onFavoriteClick) {
                Text(
                    text = if (isFavorite) "♥" else "♡",
                    color = if (isFavorite) {
                        Color(0xFFFF5A5F)
                    } else {
                        Color.LightGray
                    },
                    fontSize = 19.sp
                )
            }

            IconButton(onClick = onListClick) {
                Icon(
                    imageVector = Icons.Default.FormatListBulleted,
                    contentDescription = "Station List",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NowPlayingCard(
    categoryIsSearch: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onSaveOrMoveClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showStationMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(195.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1D252C)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(75.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF34434F)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♫",
                        color = Color.White,
                        fontSize = 38.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = "WRTI Classical Stream — Beethoven Symphony No. 9",
                        modifier = Modifier.basicMarquee(),
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    Text(
                        text = "General",
                        color = Color(0xFFC8C8C8),
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Station 4 of 5",
                            color = Color(0xFF9FAAB2),
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        MiniVuMeter(
                            isPlaying = isPlaying
                        )
                    }
                }

                IconButton(onClick = onFavoriteClick) {
                    Text(
                        text = if (isFavorite) "♥" else "♡",
                        color = if (isFavorite) {
                            Color(0xFFFF5A5F)
                        } else {
                            Color.LightGray
                        },
                        fontSize = 19.sp
                    )
                }

                Box {
                    IconButton(onClick = { showStationMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Station Menu",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
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
                                        Color.Gray
                                    } else {
                                        Color.Unspecified
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

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Beautiful classical music playing now",
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFCED5DA),
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
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
        "Classical", "Hawaiian", "Jazz", "Rock", "News"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        searches.forEach { search ->
            FilterChip(
                selected = selectedSearch.equals(
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
    onPlayPauseClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DirectionButton("<<") { }
        DirectionButton("<") { }

        Button(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPlaying) {
                    Color(0xFFD71920)
                } else {
                    Color(0xFF1D9A50)
                }
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (isPlaying) "■" else "▶",
                color = Color.Black,
                fontSize = if (isPlaying) 38.sp else 35.sp,
                fontWeight = FontWeight.Bold
            )
        }

        DirectionButton(">") { }
        DirectionButton(">>") { }
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
            animationFrame = (animationFrame + 1) % 8
        }
    }

    val patterns = listOf(
        listOf(0.18f, 0.18f, 0.18f, 0.18f, 0.18f),
        listOf(0.30f, 0.55f, 0.85f, 0.45f, 0.25f),
        listOf(0.55f, 0.30f, 0.65f, 0.90f, 0.40f),
        listOf(0.25f, 0.75f, 0.45f, 0.65f, 0.85f),
        listOf(0.70f, 0.45f, 0.90f, 0.35f, 0.60f),
        listOf(0.40f, 0.85f, 0.55f, 0.75f, 0.30f),
        listOf(0.80f, 0.35f, 0.70f, 0.50f, 0.90f),
        listOf(0.45f, 0.65f, 0.30f, 0.85f, 0.55f)
    )

    val currentPattern = patterns[animationFrame]

    Canvas(
        modifier = Modifier
            .width(34.dp)
            .height(18.dp)
    ) {
        val barCount = currentPattern.size
        val gap = 2.dp.toPx()
        val availableWidth =
            size.width - gap * (barCount - 1)

        val barWidth = availableWidth / barCount

        currentPattern.forEachIndexed { index, heightFraction ->
            val barHeight = size.height * heightFraction

            drawRoundRect(
                color = Color(0xFF9FAAB2),
                topLeft = Offset(
                    x = index * (barWidth + gap),
                    y = size.height - barHeight
                ),
                size = Size(
                    width = barWidth,
                    height = barHeight
                ),
                cornerRadius = CornerRadius(
                    x = 1.dp.toPx(),
                    y = 1.dp.toPx()
                ),
                style = Fill
            )
        }
    }
}
@Composable
private fun DirectionButton(
    symbol: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .width(59.dp)
            .height(96.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = symbol,
            color = Color.White,
            fontSize = 35.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            softWrap = false,
            maxLines = 1
        )
    }
}

