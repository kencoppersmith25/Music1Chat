package com.coppersmith.music1chat.ui.components

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

    val secondaryInformation =
        listOf(
            stationCallLetters,
            stationGenre,
            stationCity,
            stationCountry
        )
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("  •  ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(174.dp),
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
                    start = 14.dp,
                    end = 3.dp,
                    top = 10.dp,
                    bottom = 10.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = stationName,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text =
                            "Station $stationNumber of $stationCount",
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
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
                                    Text("Save to another category")
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

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            MaterialTheme.colorScheme
                                .surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♫",
                        fontSize = 29.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text =
                            nowPlayingText.ifBlank {
                                "Live radio"
                            },
                        color =
                            MaterialTheme.colorScheme
                                .onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (secondaryInformation.isNotBlank()) {
                        Spacer(
                            modifier = Modifier.height(3.dp)
                        )

                        Text(
                            text = secondaryInformation,
                            modifier = Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                velocity =
                                    MarqueeDefaults.Velocity * 1.6f
                            ),
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                if (isPlaying) {
                    MiniVuMeter(isPlaying = true)
                } else {
                    Text(
                        text = "Stopped",
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }
        }
    }
}