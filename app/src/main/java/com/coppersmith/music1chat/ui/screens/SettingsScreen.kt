package com.coppersmith.music1chat.ui.screens

// Music1Chat coordinated release
// Date: 2026-07-27
// Release: 2026-07-27 v01
//
// Settings:
// - Configures live-search result limit.
// - Selects any installed English Android voice.
// - Tests the selected voice with "Classical."
// - Saves the selected voice for category announcements.

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coppersmith.music1chat.persistence.AppPreferences
import com.coppersmith.music1chat.speech.CategoryAnnouncer
import com.coppersmith.music1chat.speech.VoiceExplorer
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    searchResultLimit: Int,
    onSearchResultLimitChanged: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context =
        LocalContext.current

    val appPreferences =
        remember(context) {
            AppPreferences(
                context.applicationContext
            )
        }

    val categoryAnnouncer =
        remember(context) {
            CategoryAnnouncer(context)
        }

    var installedVoices by
    remember {
        mutableStateOf<
                List<VoiceExplorer.VoiceOption>
                >(
            emptyList()
        )
    }

    var voicesLoading by
    remember {
        mutableStateOf(true)
    }

    var selectedVoiceId by
    remember {
        mutableStateOf(
            appPreferences
                .loadCategoryAnnouncementVoiceId()
        )
    }

    var showVoicePicker by
    remember {
        mutableStateOf(false)
    }

    DisposableEffect(categoryAnnouncer) {
        onDispose {
            categoryAnnouncer.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        VoiceExplorer
            .loadInstalledEnglishVoices(
                context = context
            ) { voices ->
                installedVoices = voices
                voicesLoading = false
            }
    }

    val selectedVoiceDescription =
        selectedVoiceId
            ?.let { savedId ->
                installedVoices
                    .firstOrNull { voice ->
                        voice.voiceId == savedId
                    }
                    ?.displayName
            }
            ?: "Phone Default"

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
                Modifier.fillMaxWidth(0.90f),
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            color =
                MaterialTheme.colorScheme
                    .surfaceContainer
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = 22.dp,
                        vertical = 18.dp
                    )
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
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
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text =
                                "Maximum search results",
                            fontSize = 18.sp,
                            fontWeight =
                                FontWeight.SemiBold
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
                        text =
                            searchResultLimit.toString(),
                        modifier =
                            Modifier.padding(
                                start = 16.dp
                            ),
                        color =
                            MaterialTheme.colorScheme
                                .primary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Slider(
                    value =
                        searchResultLimit.toFloat(),
                    onValueChange = { sliderValue ->
                        val roundedValue =
                            (
                                    sliderValue /
                                            SEARCH_LIMIT_STEP
                                    ).roundToInt() *
                                    SEARCH_LIMIT_STEP

                        onSearchResultLimitChanged(
                            roundedValue.coerceIn(
                                MINIMUM_SEARCH_LIMIT,
                                MAXIMUM_SEARCH_LIMIT
                            )
                        )
                    },
                    valueRange =
                        MINIMUM_SEARCH_LIMIT
                            .toFloat()..
                                MAXIMUM_SEARCH_LIMIT
                                    .toFloat(),
                    steps = 18,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {
                    Text(
                        text =
                            MINIMUM_SEARCH_LIMIT
                                .toString(),
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Text(
                        text =
                            MAXIMUM_SEARCH_LIMIT
                                .toString(),
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                HorizontalDivider()

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text = "Voice",
                    color =
                        MaterialTheme.colorScheme.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text =
                        "Category announcement voice",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = selectedVoiceDescription,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            showVoicePicker = true
                        }
                    ) {
                        Text(
                            text = "Choose Voice",
                            fontSize = 17.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = {
                            categoryAnnouncer
                                .selectVoiceForSession(
                                    selectedVoiceId
                                )

                            categoryAnnouncer
                                .testVoice("Classical")
                        }
                    ) {
                        Text(
                            text = "Test Voice",
                            fontSize = 17.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }
        }
    }

    if (showVoicePicker) {
        VoicePickerDialog(
            voices = installedVoices,
            voicesLoading = voicesLoading,
            initialVoiceId = selectedVoiceId,
            onTestVoice = { voiceId ->
                categoryAnnouncer
                    .selectVoiceForSession(voiceId)

                categoryAnnouncer
                    .testVoice("Classical")
            },
            onSave = { voiceId ->
                appPreferences
                    .saveCategoryAnnouncementVoiceId(
                        voiceId
                    )

                selectedVoiceId = voiceId

                categoryAnnouncer
                    .selectVoiceForSession(voiceId)

                showVoicePicker = false
            },
            onDismiss = {
                categoryAnnouncer
                    .selectVoiceForSession(
                        selectedVoiceId
                    )

                showVoicePicker = false
            }
        )
    }
}

@Composable
private fun VoicePickerDialog(
    voices: List<VoiceExplorer.VoiceOption>,
    voicesLoading: Boolean,
    initialVoiceId: String?,
    onTestVoice: (String?) -> Unit,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var temporaryVoiceId by
    remember(
        initialVoiceId,
        voices
    ) {
        mutableStateOf(initialVoiceId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =
                    "Category Announcement Voice",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    text =
                        "Tap a voice to hear it immediately. The selected voice is saved automatically.",
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                VoiceChoiceRow(
                    title = "Phone Default",
                    subtitle =
                        "Use the phone's normal Text-to-Speech voice.",
                    selected =
                        temporaryVoiceId == null,
                    onClick = {
                        temporaryVoiceId = null
                        onSave(null)
                        onTestVoice(null)
                    }
                )

                HorizontalDivider()

                when {
                    voicesLoading -> {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical = 24.dp
                                    ),
                            horizontalArrangement =
                                Arrangement.Center,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(28.dp)
                            )
                        }
                    }

                    voices.isEmpty() -> {
                        Text(
                            text =
                                "No installed English voices were found.",
                            modifier =
                                Modifier.padding(
                                    vertical = 18.dp
                                ),
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier =
                                Modifier.heightIn(
                                    max = 360.dp
                                )
                        ) {
                            items(
                                items = voices,
                                key = { voice ->
                                    voice.voiceId
                                }
                            ) { voice ->
                                VoiceChoiceRow(
                                    title =
                                        voice.displayName,
                                    subtitle =
                                        if (
                                            voice.isNetworkVoice
                                        ) {
                                            "Requires a network connection"
                                        } else {
                                            "Available offline"
                                        },
                                    selected =
                                        temporaryVoiceId ==
                                                voice.voiceId,
                                    onClick = {
                                        temporaryVoiceId =
                                            voice.voiceId

                                        onTestVoice(
                                            voice.voiceId
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                TextButton(
                    onClick = {
                        onTestVoice(
                            temporaryVoiceId
                        )
                    },
                    enabled = !voicesLoading
                ) {
                    Text(
                        text =
                            "Test Selected Voice",
                        fontSize = 16.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(temporaryVoiceId)
                },
                enabled = !voicesLoading
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun VoiceChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    vertical = 8.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 6.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight =
                    if (selected) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    }
            )

            Text(
                text = subtitle,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
        }
    }
}

private const val MINIMUM_SEARCH_LIMIT = 5
private const val MAXIMUM_SEARCH_LIMIT = 100
private const val SEARCH_LIMIT_STEP = 5