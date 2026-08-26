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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction


// Music1Chat coordinated release
// File: CategoryPicker.kt
// Release: 2026-07-23 v03
// DROP-IN REPLACEMENT
// Change: displays existing category counts as parenthesized numbers.

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
    proposedName: String = "",
    categories: List<Category>,
    suggestedCategoryNames: List<String>,
    stationCountForCategory: (Category) -> Int,
    onSearchTextChanged: (String) -> Unit,
    onCategorySelected: (categoryName: String, existingCategory: Category?) -> Unit,
    onDismiss: () -> Unit
) {
    /*
     * The picker is often opened with a useful proposed destination already
     * in proposedName, such as "Classical". That initial value should rank the
     * matching category first.
     * The searchText starts empty so the user can just type.
     */

    val focusManager = LocalFocusManager.current
    
    // UX FIX: The picker text field now starts empty to allow instant typing.
    // We still use proposedName (e.g. "Classical") to filter/rank the list.
    val initialSearchText = remember { proposedName.trim() }

    val pickerItems = remember(
        searchText,
        proposedName,
        initialSearchText,
        categories,
        suggestedCategoryNames
    ) {
        val typedText = searchText.trim()
        val displaySearchText = typedText.ifBlank { proposedName.trim() }
        
        val isInitialText =
            displaySearchText.equals(initialSearchText, ignoreCase = true)

        val allCategoryNames =
            (categories.map { category -> category.name } +
                    suggestedCategoryNames)
                .filter { name -> name.isNotBlank() }
                .distinctBy { name ->
                    name.trim().lowercase()
                }

        val matchingNames =
            when {
                displaySearchText.isBlank() || isInitialText -> {
                    buildList {
                        if (displaySearchText.isNotBlank()) {
                            add(displaySearchText)
                        }
                        addAll(allCategoryNames)
                    }
                }

                else -> {
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
                    }
                }
            }.distinctBy { name ->
                name.trim().lowercase()
            }

        matchingNames.sortedWith(
            compareByDescending<String> { name ->
                displaySearchText.isNotBlank() &&
                        name.equals(
                            displaySearchText,
                            ignoreCase = true
                        )
            }.thenByDescending { name ->
                categories.any { category ->
                    category.name.equals(
                        name,
                        ignoreCase = true
                    )
                }
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
                .fillMaxWidth(0.78f)
                .fillMaxHeight(0.75f),
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
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { onSearchTextChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear text"
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val categoryName = searchText.trim()

                            if (categoryName.isNotBlank()) {
                                val existingCategory =
                                    categories.firstOrNull { category ->
                                        category.name.equals(
                                            categoryName,
                                            ignoreCase = true
                                        )
                                    }

                                focusManager.clearFocus()

                                onCategorySelected(
                                    categoryName,
                                    existingCategory
                                )
                            }
                        }
                    )
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
                                        text = "($stationCount)",
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