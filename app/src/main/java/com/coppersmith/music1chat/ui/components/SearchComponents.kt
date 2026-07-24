package com.coppersmith.music1chat.ui.components

// Music1Chat coordinated release
// File: the source file containing GenreSearchBox, TopControlBar, and SearchChips
// Release: 2026-07-23 v03
// DROP-IN REPLACEMENT
// Changes:
// - Replaces the duplicate Settings placeholder with the Cast icon.
// - Adds Cast and Power callbacks to TopControlBar.
// - Preserves the existing genre-search and search-chip behavior.


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import com.coppersmith.music1chat.cast.Music1CastButton
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


@Composable
fun GenreSearchBox(
    searchText: String,
    filteredGenres: List<String>,
    showGenreMenu: Boolean,
    onSearchTextChanged: (String) -> Unit,
    onDropdownClick: () -> Unit,
    onSearchClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onGenreSelected: (String) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val menuWidth = maxWidth

        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChanged,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            singleLine = true,
            placeholder = {
                Text(
                    text = "Search genres",
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDropdownClick
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.ArrowDropDown,
                            contentDescription =
                                if (showGenreMenu) {
                                    "Close genres"
                                } else {
                                    "Show genres"
                                },
                            tint =
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            onDismissMenu()
                            onSearchClick()
                        }
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.Search,
                            contentDescription = "Search",
                            tint =
                                MaterialTheme.colorScheme.onSurface,
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
                onSearch = {
                    onDismissMenu()
                    onSearchClick()
                }
            ),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor =
                        MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor =
                        MaterialTheme.colorScheme.onSurface,
                    cursorColor =
                        MaterialTheme.colorScheme.primary,
                    focusedBorderColor =
                        MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor =
                        MaterialTheme.colorScheme.outline
                )
        )

        DropdownMenu(
            expanded =
                showGenreMenu &&
                        filteredGenres.isNotEmpty(),
            onDismissRequest = onDismissMenu,
            modifier = Modifier
                .width(menuWidth)
                .heightIn(max = 600.dp),
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            filteredGenres.forEach { genre ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = genre,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurface,
                            fontSize = 18.sp
                        )
                    },
                    onClick = {
                        onDismissMenu()
                        onGenreSelected(genre)
                    },
                    contentPadding = PaddingValues(
                        horizontal = 18.dp,
                        vertical = 4.dp
                    )
                )
            }
        }
    }
}

@Composable
fun TopControlBar(
    onSettingsClick: () -> Unit,
    onPowerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text = "Music1Chat",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        IconButton(
            onClick = onSettingsClick
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(30.dp)
            )
        }

        Music1CastButton(
            modifier = Modifier.size(40.dp)
        )

        IconButton(onClick = onPowerClick) {
            Icon(
                imageVector =
                    Icons.Default.PowerSettingsNew,
                contentDescription = "Exit",
                tint = Color(0xFFFF5A5F),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}


@Composable
fun SearchChips(
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
        modifier = Modifier
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