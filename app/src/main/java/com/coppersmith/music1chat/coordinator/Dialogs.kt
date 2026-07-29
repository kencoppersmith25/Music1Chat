package com.coppersmith.music1chat.coordinator

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.coppersmith.music1chat.models.Station

@Composable
fun Dialogs(
    pendingStation: Station?,
    pendingStationCategoryId: Long?,
    pendingStationCategoryName: String,
    pendingCategoryKey: String?,
    pendingCategoryDisplayName: String,
    onDismissStationDelete: () -> Unit,
    onConfirmStationDelete: (Station, Long) -> Unit,
    onDismissCategoryDelete: () -> Unit,
    onConfirmCategoryDelete: (String) -> Unit
) {
    if (pendingStation != null && pendingStationCategoryId != null) {
        AlertDialog(
            onDismissRequest = onDismissStationDelete,
            title = {
                Text("Delete station?")
            },
            text = {
                Text(
                    "Are you sure you want to delete " +
                            "“${pendingStation.name}” from " +
                            "“$pendingStationCategoryName”?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmStationDelete(
                            pendingStation,
                            pendingStationCategoryId
                        )
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissStationDelete
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (pendingCategoryKey != null) {
        AlertDialog(
            onDismissRequest = onDismissCategoryDelete,
            title = {
                Text("Delete category?")
            },
            text = {
                Text(
                    "Are you sure you want to delete the " +
                            "$pendingCategoryDisplayName category and its stations?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmCategoryDelete(pendingCategoryKey)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissCategoryDelete
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}