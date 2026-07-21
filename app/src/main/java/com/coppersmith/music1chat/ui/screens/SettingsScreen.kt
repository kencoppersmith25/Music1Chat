package com.coppersmith.music1chat.ui.screens

// Music1Chat coordinated release
// Date: 2026-07-21
// Release: 2026-07-21 v01
//
// First Settings dialog:
// - Configures live-search result limit.
// - Range: 5 through 100.
// - Changes in increments of 5.
// - Dismisses with Back or by tapping outside.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    searchResultLimit: Int,
    onSearchResultLimitChanged: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(0.90f),
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            color =
                MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = 22.dp,
                        vertical = 18.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Done")
                    }
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text = "Search",
                    color =
                        MaterialTheme.colorScheme.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Maximum search results",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text =
                                "Maximum number of live stations returned by a search.",
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        )
                    }

                    Text(
                        text = searchResultLimit.toString(),
                        modifier =
                            Modifier.padding(start = 16.dp),
                        color =
                            MaterialTheme.colorScheme.primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Slider(
                    value = searchResultLimit.toFloat(),
                    onValueChange = { sliderValue ->
                        val roundedValue =
                            (
                                    sliderValue / SEARCH_LIMIT_STEP
                                    ).roundToInt() * SEARCH_LIMIT_STEP

                        onSearchResultLimitChanged(
                            roundedValue.coerceIn(
                                MINIMUM_SEARCH_LIMIT,
                                MAXIMUM_SEARCH_LIMIT
                            )
                        )
                    },
                    valueRange =
                        MINIMUM_SEARCH_LIMIT.toFloat()..
                                MAXIMUM_SEARCH_LIMIT.toFloat(),
                    steps = 18,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {
                    Text(
                        text = MINIMUM_SEARCH_LIMIT.toString(),
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Text(
                        text = MAXIMUM_SEARCH_LIMIT.toString(),
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }
        }
    }
}

private const val MINIMUM_SEARCH_LIMIT = 5
private const val MAXIMUM_SEARCH_LIMIT = 100
private const val SEARCH_LIMIT_STEP = 5