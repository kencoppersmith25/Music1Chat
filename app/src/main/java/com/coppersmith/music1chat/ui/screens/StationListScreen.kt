package com.coppersmith.music1chat.ui.screens


import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.coppersmith.music1chat.models.Station
import com.coppersmith.music1chat.ui.components.NavigationIndicator
import kotlin.math.abs
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
            stations.map { station -> station.id },
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

    /*
     * Position the selected station near the middle of the dialog when
     * possible. Stations close to the beginning or end are constrained
     * naturally by LazyColumn.
     */
    LaunchedEffect(
        categoryName,
        selectedStationId
    ) {
        val selectedIndex =
            orderedStations.indexOfFirst { station ->
                station.id == selectedStationId
            }

        if (selectedIndex >= 0) {
            val centeredIndex =
                (selectedIndex - 2).coerceAtLeast(0)

            listState.scrollToItem(centeredIndex)
        }
    }

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

    var dragScrollY by remember {
        mutableFloatStateOf(0f)
    }

    var pendingDelete by remember {
        mutableStateOf<Station?>(null)
    }

    var pendingRevealStationId by remember {
        mutableStateOf<Long?>(null)
    }

    var pendingRevealDestination by remember {
        mutableIntStateOf(-1)
    }

    LaunchedEffect(
        stateVersion,
        stations.map { station -> station.id },
        pendingRevealStationId,
        pendingRevealDestination
    ) {
        val revealId = pendingRevealStationId
        val revealDestination = pendingRevealDestination

        if (revealId != null && revealDestination >= 0) {
            val movedIndex =
                orderedStations.indexOfFirst { station ->
                    station.id == revealId
                }

            if (movedIndex == revealDestination) {
                val revealStart =
                    when {
                        movedIndex >= orderedStations.lastIndex ->
                            (movedIndex - 2).coerceAtLeast(0)

                        else ->
                            (movedIndex - 1).coerceAtLeast(0)
                    }

                listState.scrollToItem(revealStart)
                pendingRevealStationId = null
                pendingRevealDestination = -1
            }
        }
    }

    fun finishDrag(
        commit: Boolean
    ) {
        val station =
            draggedStation

        val destination =
            placeholderIndex

        if (
            commit &&
            station != null &&
            destination >= 0 &&
            destination != originalIndex
        ) {
            pendingRevealStationId = station.id
            pendingRevealDestination = destination

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
        dragScrollY = 0f
    }

    Dialog(
        onDismissRequest = onCloseClick,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            color =
                MaterialTheme.colorScheme
                    .surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Stations",
                    fontSize = 24.sp,
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
                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text =
                            "Press and hold, then drag to reorder.",
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                val insertionLineColor =
                    MaterialTheme.colorScheme.primary

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(
                            reorderEnabled,
                            stateVersion,
                            categoryName
                        ) {
                            if (!reorderEnabled) {
                                return@pointerInput
                            }

                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset: Offset ->
                                    val visibleItem =
                                        listState.layoutInfo
                                            .visibleItemsInfo
                                            .firstOrNull { item ->
                                                offset.y >= item.offset &&
                                                        offset.y <=
                                                        item.offset + item.size
                                            }

                                    val stationId =
                                        visibleItem?.key as? Long

                                    val station =
                                        stationId?.let { id ->
                                            orderedStations
                                                .firstOrNull { it.id == id }
                                        }

                                    if (
                                        visibleItem != null &&
                                        station != null
                                    ) {
                                        originalIndex =
                                            orderedStations.indexOfFirst {
                                                it.id == station.id
                                            }
                                        placeholderIndex = originalIndex
                                        floatingStartY =
                                            visibleItem.offset.toFloat()
                                        floatingDragY = 0f
                                        dragScrollY = 0f
                                        draggedStation = station
                                    }
                                },
                                onDragCancel = {
                                    finishDrag(commit = false)
                                },
                                onDragEnd = {
                                    finishDrag(commit = true)
                                },
                                onDrag = { change, dragAmount ->
                                    if (draggedStation == null) {
                                        return@detectDragGesturesAfterLongPress
                                    }

                                    change.consume()
                                    floatingDragY += dragAmount.y

                                    val layoutInfo = listState.layoutInfo
                                    val floatingCenter =
                                        floatingStartY +
                                                floatingDragY +
                                                rowStepPx / 2f
                                    val edgeZonePx = rowStepPx * 0.85f

                                    val requestedScroll =
                                        when {
                                            dragAmount.y < 0f &&
                                                    floatingCenter <
                                                    layoutInfo
                                                        .viewportStartOffset +
                                                    edgeZonePx ->
                                                -rowStepPx * 0.22f

                                            dragAmount.y > 0f &&
                                                    floatingCenter >
                                                    layoutInfo
                                                        .viewportEndOffset -
                                                    edgeZonePx ->
                                                rowStepPx * 0.22f

                                            else -> 0f
                                        }

                                    if (requestedScroll != 0f) {
                                        listState.dispatchRawDelta(
                                            requestedScroll
                                        )
                                    }

                                    val refreshedLayout = listState.layoutInfo
                                    val targetItem =
                                        refreshedLayout.visibleItemsInfo
                                            .minByOrNull { item ->
                                                abs(
                                                    floatingCenter -
                                                            (item.offset +
                                                                    item.size / 2f)
                                                )
                                            }

                                    val destination =
                                        when {
                                            floatingCenter <=
                                                    refreshedLayout
                                                        .viewportStartOffset +
                                                    rowStepPx / 2f -> 0

                                            floatingCenter >=
                                                    refreshedLayout
                                                        .viewportEndOffset -
                                                    rowStepPx / 2f ->
                                                orderedStations.lastIndex

                                            else ->
                                                targetItem?.index
                                                    ?: placeholderIndex
                                        }
                                            .coerceIn(
                                                0,
                                                orderedStations.lastIndex
                                            )

                                    if (destination != placeholderIndex) {
                                        /*
                                         * Do not mutate orderedStations while the finger is down.
                                         * Moving keyed LazyColumn items during the gesture makes
                                         * Compose preserve the moved key's visual position, which
                                         * shifts the viewport and causes destination jumps.
                                         * Commit the single final move in finishDrag().
                                         */
                                        placeholderIndex = destination
                                    }
                                }
                            )
                        }
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
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
                                draggedStation?.id ==
                                        station.id

                            val showInsertionBefore =
                                draggedStation != null &&
                                        placeholderIndex < originalIndex &&
                                        index == placeholderIndex

                            val showInsertionAfter =
                                draggedStation != null &&
                                        placeholderIndex > originalIndex &&
                                        index == placeholderIndex

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
                                    .drawWithContent {
                                        drawContent()

                                        val strokeWidth =
                                            4.dp.toPx()
                                        val horizontalInset =
                                            10.dp.toPx()

                                        if (showInsertionBefore) {
                                            drawLine(
                                                color = insertionLineColor,
                                                start = Offset(
                                                    horizontalInset,
                                                    strokeWidth / 2f
                                                ),
                                                end = Offset(
                                                    size.width - horizontalInset,
                                                    strokeWidth / 2f
                                                ),
                                                strokeWidth = strokeWidth
                                            )
                                        }

                                        if (showInsertionAfter) {
                                            drawLine(
                                                color = insertionLineColor,
                                                start = Offset(
                                                    horizontalInset,
                                                    size.height -
                                                            strokeWidth / 2f
                                                ),
                                                end = Offset(
                                                    size.width - horizontalInset,
                                                    size.height -
                                                            strokeWidth / 2f
                                                ),
                                                strokeWidth = strokeWidth
                                            )
                                        }
                                    },
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
                                                    )
                                                .coerceIn(
                                                    listState.layoutInfo
                                                        .viewportStartOffset
                                                        .toFloat(),
                                                    (
                                                            listState.layoutInfo
                                                                .viewportEndOffset -
                                                                    rowStepPx
                                                            )
                                                        .coerceAtLeast(0f)
                                                )
                                                .roundToInt()
                                    )
                                }
                                .padding(end = 12.dp)
                                .zIndex(10f)
                                .shadow(
                                    elevation = 12.dp,
                                    shape =
                                        RoundedCornerShape(
                                            15.dp
                                        )
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
        modifier =
            modifier.alpha(
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
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
            )
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 15.dp,
                    end = 8.dp,
                    top = 11.dp,
                    bottom = 11.dp
                ),
            verticalAlignment =
                androidx.compose.ui.Alignment
                    .CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text =
                        "$stationNumber. ${station.name}",
                    fontSize = 18.sp,
                    fontWeight =
                        FontWeight.SemiBold,
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
                        modifier =
                            Modifier.height(3.dp)
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