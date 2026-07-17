package com.coppersmith.music1chat.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onBackClick: () -> Unit,
    onCategoryClick: (CategorySummary) -> Unit,
    onListClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onNavigationToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = 8.dp,
                bottom = 12.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Categories",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            categories.forEach { category ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(17.dp),
                    colors =
                        CardDefaults.outlinedCardColors(
                            containerColor =
                                MaterialTheme.colorScheme
                                    .surfaceVariant
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
                                    onCategoryClick(category)
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
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }

                        Row(
                            modifier = Modifier.width(138.dp),
                            horizontalArrangement =
                                Arrangement.End,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    onListClick(category.key)
                                },
                                modifier = Modifier.size(46.dp)
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
                                    onDeleteClick(category.key)
                                },
                                modifier = Modifier.size(46.dp)
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