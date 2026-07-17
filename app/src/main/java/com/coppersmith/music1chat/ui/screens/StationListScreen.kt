package com.coppersmith.music1chat.ui.screens

// Music1Chat coordinated release
// Release: 2026-07-16 v01
// Matched files: MainScreen, StationListScreen, AppPreferences

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.coppersmith.music1chat.models.Station
import com.coppersmith.music1chat.ui.components.NavigationIndicator
import kotlin.math.roundToInt

@Composable
fun StationListScreen(
    categoryName: String,
    stations: List<Station>,
    selectedStationId: Long?,
    reorderEnabled: Boolean,
    stateVersion: Int,
    onCloseClick: () -> Unit,
    onStationClick: (Station) -> Unit,
    onNavigationToggle: (Station) -> Unit,
    onMoveStation: (Station, Int) -> Unit,
    onDeleteStation: (Station) -> Unit
) {
    val orderedStations =
        remember(
            categoryName,
            stations.map { it.id },
            stateVersion
        ) {
            mutableStateListOf<Station>().apply {
                addAll(stations)
            }
        }

    var localVersion by remember {
        mutableIntStateOf(0)
    }
    localVersion

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val rowStepPx =
        with(density) {
            86.dp.toPx()
        }

    var draggedStation by remember {
        mutableStateOf<Station?>(null)
    }
    var originalIndex by remember {
        mutableIntStateOf(-1)
    }
    var placeholderIndex by remember {
        mutableIntStateOf(-1)
    }
    var floatingStartY by remember {
        mutableFloatStateOf(0f)
    }
    var floatingDragY by remember {
        mutableFloatStateOf(0f)
    }
    var pendingDelete by remember {
        mutableStateOf<Station?>(null)
    }

    fun finishDrag(commit: Boolean) {
        val station = draggedStation
        val destination = placeholderIndex

        if (
            commit &&
            station != null &&
            destination >= 0 &&
            destination != originalIndex
        ) {
            onMoveStation(
                station,
                destination
            )
        }

        draggedStation = null
        originalIndex = -1
        placeholderIndex = -1
        floatingStartY = 0f
        floatingDragY = 0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stations",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = categoryName,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    fontSize = 16.sp,
                    maxLines = 1
                )

                if (reorderEnabled) {
                    Text(
                        text =
                            "Press and hold, then drag to reorder.",
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }

            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close station list"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement =
                    Arrangement.spacedBy(9.dp)
            ) {
                itemsIndexed(
                    items = orderedStations,
                    key = { _, station ->
                        station.id
                    }
                ) { index, station ->
                    val isDragged =
                        draggedStation?.id == station.id

                    val dragModifier =
                        if (reorderEnabled) {
                            Modifier.pointerInput(
                                station.id,
                                orderedStations.size
                            ) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        originalIndex =
                                            orderedStations
                                                .indexOfFirst {
                                                    it.id ==
                                                            station.id
                                                }
                                        placeholderIndex =
                                            originalIndex

                                        val visibleItem =
                                            listState.layoutInfo
                                                .visibleItemsInfo
                                                .firstOrNull {
                                                    it.key ==
                                                            station.id
                                                }

                                        floatingStartY =
                                            visibleItem
                                                ?.offset
                                                ?.toFloat()
                                                ?: 0f
                                        floatingDragY = 0f
                                        draggedStation =
                                            station
                                    },
                                    onDragCancel = {
                                        finishDrag(
                                            commit = false
                                        )
                                    },
                                    onDragEnd = {
                                        finishDrag(
                                            commit = true
                                        )
                                    },
                                    onDrag = {
                                            change,
                                            dragAmount ->

                                        change.consume()
                                        floatingDragY +=
                                            dragAmount.y

                                        val crossedRows =
                                            (
                                                    floatingDragY /
                                                            rowStepPx
                                                    )
                                                .roundToInt()

                                        val destination =
                                            (
                                                    originalIndex +
                                                            crossedRows
                                                    )
                                                .coerceIn(
                                                    0,
                                                    orderedStations
                                                        .lastIndex
                                                )

                                        if (
                                            destination !=
                                            placeholderIndex
                                        ) {
                                            val currentIndex =
                                                orderedStations
                                                    .indexOfFirst {
                                                        it.id ==
                                                                station.id
                                                    }

                                            if (
                                                currentIndex >= 0
                                            ) {
                                                val moved =
                                                    orderedStations
                                                        .removeAt(
                                                            currentIndex
                                                        )

                                                orderedStations.add(
                                                    destination,
                                                    moved
                                                )
                                                placeholderIndex =
                                                    destination
                                            }
                                        }
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }

                    StationListRow(
                        station = station,
                        stationNumber = index + 1,
                        selected =
                            station.id ==
                                    selectedStationId,
                        showDelete = reorderEnabled,
                        dimmed = isDragged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(dragModifier),
                        onStationClick = {
                            onStationClick(station)
                        },
                        onDeleteClick = {
                            pendingDelete = station
                        },
                        onNavigationToggle = {
                            onNavigationToggle(station)
                            localVersion++
                        }
                    )
                }
            }

            val floatingStation =
                draggedStation

            if (floatingStation != null) {
                StationListRow(
                    station = floatingStation,
                    stationNumber =
                        (
                                placeholderIndex + 1
                                ).coerceAtLeast(1),
                    selected = true,
                    showDelete = false,
                    dimmed = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset {
                            IntOffset(
                                x =
                                    with(density) {
                                        12.dp.roundToPx()
                                    },
                                y =
                                    (
                                            floatingStartY +
                                                    floatingDragY
                                            ).roundToInt()
                            )
                        }
                        .padding(end = 12.dp)
                        .zIndex(10f)
                        .shadow(
                            elevation = 12.dp,
                            shape =
                                RoundedCornerShape(15.dp)
                        )
                        .graphicsLayer {
                            scaleX = 1.02f
                            scaleY = 1.02f
                            alpha = 0.96f
                        },
                    onStationClick = {},
                    onDeleteClick = {},
                    onNavigationToggle = {}
                )
            }
        }
    }

    val stationToDelete =
        pendingDelete

    if (stationToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                pendingDelete = null
            },
            title = {
                Text("Remove station?")
            },
            text = {
                Text(
                    "Are you sure you want to remove " +
                            "\"${stationToDelete.name}\" " +
                            "from the $categoryName category?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteStation(
                            stationToDelete
                        )
                        pendingDelete = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StationListRow(
    station: Station,
    stationNumber: Int,
    selected: Boolean,
    showDelete: Boolean,
    dimmed: Boolean,
    modifier: Modifier,
    onStationClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onNavigationToggle: () -> Unit
) {
    OutlinedCard(
        onClick = onStationClick,
        modifier = modifier.alpha(
            if (dimmed) {
                0.25f
            } else {
                1f
            }
        ),
        shape = RoundedCornerShape(15.dp),
        colors =
            CardDefaults.outlinedCardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme
                            .secondaryContainer
                    } else {
                        MaterialTheme.colorScheme
                            .surfaceVariant
                    }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 15.dp,
                    end = 8.dp,
                    top = 11.dp,
                    bottom = 11.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text =
                        "$stationNumber. ${station.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                val details =
                    listOf(
                        station.callLetters,
                        station.genre,
                        station.city,
                        station.country
                    )
                        .filter {
                            it.isNotBlank()
                        }
                        .distinct()

                if (details.isNotEmpty()) {
                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text =
                            details.joinToString(" • "),
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }

            if (showDelete) {
                IconButton(
                    onClick = onDeleteClick
                ) {
                    Icon(
                        imageVector =
                            Icons.Default.Delete,
                        contentDescription =
                            "Remove station"
                    )
                }
            }

            NavigationIndicator(
                included =
                    station.includedInNavigation,
                onClick =
                    onNavigationToggle,
                modifier = Modifier
                    .width(42.dp)
                    .height(34.dp)
            )
        }
    }
}