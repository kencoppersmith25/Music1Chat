package com.coppersmith.music1chat.ui.screens

// Music1Chat coordinated release
// Date: 2026-07-18
// Release: 2026-07-18 v02
// DROP-IN REPLACEMENT
//
// Changes:
// - Converts CategoryListScreen from a full-page screen to a dialog.
// - Removes the Back arrow.
// - Dismisses when tapping outside the dialog.
// - Dismisses with the Android Back button or gesture.
// - Automatically scrolls near the currently selected category.
// - Highlights the currently selected category.
// - Preserves category selection, station-list, delete, and navigation actions.
//
// Matched files: MainScreen, CategoryListScreen, StationListScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coppersmith.music1chat.ui.components.NavigationIndicator

data class CategorySummary(
    val key: String,
    val name: String,
    val stationCount: Int,
    val includedInNavigation: Boolean
)

@Composable
fun CategoryListScreen(
    categories: List<CategorySummary>,
    selectedCategoryKey: String?,
    onBackClick: () -> Unit,
    onCategoryClick: (CategorySummary) -> Unit,
    onListClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onNavigationToggle: (String) -> Unit
) {
    val listState =
        rememberLazyListState()

    LaunchedEffect(
        selectedCategoryKey,
        categories.map { category ->
            category.key
        }
    ) {
        val selectedIndex =
            categories.indexOfFirst { category ->
                category.key == selectedCategoryKey
            }

        if (selectedIndex >= 0) {
            /*
             * Place the current category a couple of rows below the
             * top when possible, rather than pinning it directly to
             * the first visible position.
             */
            val scrollIndex =
                (selectedIndex - 2).coerceAtLeast(0)

            listState.scrollToItem(scrollIndex)
        }
    }

    Dialog(
        onDismissRequest = onBackClick,
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
            shape =
                RoundedCornerShape(22.dp),
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
                    text = "Categories",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = categories,
                        key = { category ->
                            category.key
                        }
                    ) { category ->
                        val selected =
                            category.key ==
                                    selectedCategoryKey

                        OutlinedCard(
                            modifier =
                                Modifier.fillMaxWidth(),
                            shape =
                                RoundedCornerShape(17.dp),
                            colors =
                                CardDefaults
                                    .outlinedCardColors(
                                        containerColor =
                                            if (selected) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                    )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 17.dp,
                                        end = 7.dp,
                                        top = 12.dp,
                                        bottom = 12.dp
                                    ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onCategoryClick(
                                                category
                                            )
                                        }
                                ) {
                                    Text(
                                        text = category.name,
                                        fontSize = 20.sp,
                                        fontWeight =
                                            FontWeight.SemiBold,
                                        maxLines = 1
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.height(3.dp)
                                    )

                                    Text(
                                        text =
                                            "${category.stationCount} stations",
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }

                                Row(
                                    modifier =
                                        Modifier.width(138.dp),
                                    horizontalArrangement =
                                        Arrangement.End,
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            onListClick(
                                                category.key
                                            )
                                        },
                                        modifier =
                                            Modifier.size(46.dp)
                                    ) {
                                        Icon(
                                            imageVector =
                                                Icons.Default
                                                    .FormatListBulleted,
                                            contentDescription =
                                                "Open station list"
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            onDeleteClick(
                                                category.key
                                            )
                                        },
                                        modifier =
                                            Modifier.size(46.dp)
                                    ) {
                                        Icon(
                                            imageVector =
                                                Icons.Default.Delete,
                                            contentDescription =
                                                "Delete category"
                                        )
                                    }

                                    NavigationIndicator(
                                        included =
                                            category
                                                .includedInNavigation,
                                        onClick = {
                                            onNavigationToggle(
                                                category.key
                                            )
                                        },
                                        modifier = Modifier
                                            .width(42.dp)
                                            .height(34.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}