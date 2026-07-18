package com.coppersmith.music1chat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coppersmith.music1chat.models.Category

/**
 * Reusable category chooser for current-category selection, saving, and moving.
 * Existing library categories are listed first and include their station counts.
 * The exact typed text is also offered, allowing the caller to create a custom
 * category when no matching category already exists.
 */
@Composable
fun CategoryPicker(
    title: String,
    searchText: String,
    categories: List<Category>,
    suggestedCategoryNames: List<String>,
    stationCountForCategory: (Category) -> Int,
    onSearchTextChanged: (String) -> Unit,
    onCategorySelected: (categoryName: String, existingCategory: Category?) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerItems = remember(
        searchText,
        categories,
        suggestedCategoryNames
    ) {
        val typedText = searchText.trim()

        val allCategoryNames =
            (categories.map { category -> category.name } +
                    suggestedCategoryNames)
                .distinctBy { name ->
                    name.trim().lowercase()
                }

        val matchingNames =
            if (typedText.isBlank()) {
                allCategoryNames
            } else {
                buildList {
                    add(typedText)

                    addAll(
                        allCategoryNames.filter { name ->
                            name.contains(
                                typedText,
                                ignoreCase = true
                            )
                        }
                    )
                }.distinctBy { name ->
                    name.trim().lowercase()
                }
            }

        matchingNames.sortedWith(
            compareByDescending<String> { name ->
                categories.any { category ->
                    category.name.equals(
                        name,
                        ignoreCase = true
                    )
                }
            }.thenByDescending { name ->
                name.equals(
                    typedText,
                    ignoreCase = true
                )
            }.thenBy { name ->
                name.lowercase()
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchTextChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = {
                        Text("Find a category")
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Find category"
                        )
                    }
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                if (pickerItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Type a category name to create it.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(
                            items = pickerItems,
                            key = { name -> name.lowercase() }
                        ) { categoryName ->
                            val existingCategory =
                                categories.firstOrNull { category ->
                                    category.name.equals(
                                        categoryName,
                                        ignoreCase = true
                                    )
                                }

                            val stationCount =
                                existingCategory?.let(
                                    stationCountForCategory
                                )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCategorySelected(
                                            categoryName,
                                            existingCategory
                                        )
                                    }
                                    .padding(
                                        horizontal = 8.dp,
                                        vertical = 13.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = categoryName,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 17.sp
                                )

                                if (stationCount != null) {
                                    Text(
                                        text = "$stationCount " +
                                                if (stationCount == 1) {
                                                    "station"
                                                } else {
                                                    "stations"
                                                },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}