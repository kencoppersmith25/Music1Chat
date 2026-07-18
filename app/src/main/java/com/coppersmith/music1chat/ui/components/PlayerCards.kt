package com.coppersmith.music1chat.ui.components

// Music1Chat coordinated release
// File: PlayerCards.kt
// Release: 2026-07-17 v02
// DROP-IN REPLACEMENT
// Change: removes Live Radio/Stopped labels, keeps the VU meter visible, and disables redundant Move to category.


import com.coppersmith.music1chat.ui.components.NavigationIndicator
import com.coppersmith.music1chat.ui.components.MiniVuMeter
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.foundation.MarqueeDefaults
import androidx.compose.ui.text.style.TextOverflow
import com.coppersmith.music1chat.ui.components.NavigationIndicator
import com.coppersmith.music1chat.ui.components.MiniVuMeter
@Composable
fun CategoryCard(
    categoryName: String,
    includedInNavigation: Boolean,
    onNavigationToggle: () -> Unit,
    onCategoryClick: () -> Unit,
    onListClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors =
            CardDefaults.outlinedCardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant
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
                color = MaterialTheme.colorScheme.primary,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Row(
                modifier = Modifier.width(138.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onListClick,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector =
                            Icons.Default.FormatListBulleted,
                        contentDescription =
                            "Open station list",
                        modifier = Modifier.size(27.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription =
                            "Delete category",
                        modifier = Modifier.size(26.dp)
                    )
                }

                NavigationIndicator(
                    included = includedInNavigation,
                    onClick = onNavigationToggle,
                    modifier = Modifier
                        .width(42.dp)
                        .height(34.dp)
                )
            }
        }
    }
}


@Composable
fun CardTrailingControls(
    includedInNavigation: Boolean,
    onNavigationToggle: () -> Unit,
    iconContentDescription: String,
    onIconClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.width(90.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        NavigationIndicator(
            included = includedInNavigation,
            onClick = onNavigationToggle,
            modifier = Modifier
                .width(42.dp)
                .height(34.dp)
        )

        IconButton(
            onClick = onIconClick,
            modifier = Modifier.size(48.dp)
        ) {
            icon()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingCard(
    stationName: String,
    stationGenre: String,
    stationCallLetters: String,
    stationCity: String,
    stationCountry: String,
    nowPlayingText: String,
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

    val hasNowPlayingMetadata =
        nowPlayingText.isNotBlank()

    val secondaryInformation =
        listOf(
            stationGenre,
            stationCallLetters,
            stationCity,
            stationCountry
        )
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("  •  ")

    val stationPositionText =
        if (stationCount > 0) {
            "$stationNumber of $stationCount"
        } else {
            ""
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(188.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 3.dp,
                    top = 11.dp,
                    bottom = 11.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            top = 2.dp,
                            end = 8.dp
                        )
                ) {
                    if (hasNowPlayingMetadata) {
                        Text(
                            text = nowPlayingText,
                            modifier = Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                velocity =
                                    MarqueeDefaults.Velocity * 1.35f
                            ),
                            color =
                                MaterialTheme.colorScheme.onSurface,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text =
                                if (stationPositionText.isBlank()) {
                                    stationName
                                } else {
                                    "$stationName  ($stationPositionText)"
                                },
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = stationName,
                            modifier = Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                velocity =
                                    MarqueeDefaults.Velocity * 1.35f
                            ),
                            color =
                                MaterialTheme.colorScheme.onSurface,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        if (stationPositionText.isNotBlank()) {
                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "Station $stationPositionText",
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight =
                                    FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.width(90.dp),
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.End
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                showStationMenu = true
                            }
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Default.Folder,
                                contentDescription =
                                    "Station actions",
                                modifier = Modifier.size(28.dp)
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
                                        text =
                                            if (categoryIsSearch) {
                                                "Save to category"
                                            } else {
                                                "Move to category"
                                            },
                                        color =
                                            if (categoryIsSearch) {
                                                Color.Unspecified
                                            } else {
                                                Color.Gray
                                            }
                                    )
                                },
                                enabled = categoryIsSearch,
                                onClick = {
                                    showStationMenu = false
                                    onSaveOrMoveClick()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Save to another category"
                                    )
                                },
                                onClick = {
                                    showStationMenu = false
                                    onCopyClick()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete station",
                                        color =
                                            if (categoryIsSearch) {
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

                    NavigationIndicator(
                        included = includedInNavigation,
                        onClick = onNavigationToggle,
                        modifier = Modifier
                            .width(42.dp)
                            .height(34.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            if (secondaryInformation.isNotBlank()) {
                Text(
                    text = secondaryInformation,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        velocity =
                            MarqueeDefaults.Velocity * 1.5f
                    ),
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.End
            ) {
                MiniVuMeter(
                    isPlaying = isPlaying
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )
            }
        }
    }
}