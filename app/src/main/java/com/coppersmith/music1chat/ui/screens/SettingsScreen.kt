package com.coppersmith.music1chat.ui.screens

// Music1Chat coordinated release
// Date: 2026-07-30
// Release: 2026-08-06 v02
//
// Settings:
// - Configures live-search result limit.
// - Selects any installed English Android voice.
// - Enables or disables category-change announcements.
// - Tapping the announcement voice opens the voice picker.
// - Configures what the spoken Previous Track command does.

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import com.coppersmith.music1chat.persistence.AppPreferences
import com.coppersmith.music1chat.speech.CategoryAnnouncer
import com.coppersmith.music1chat.speech.VoiceExplorer
import com.coppersmith.music1chat.diagnostics.RideLogger
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

    var announceCategoryChanges by
    remember {
        mutableStateOf(
            appPreferences
                .loadAnnounceCategoryChanges()
        )
    }

    var voicePreviousMeansNextCategory by
    remember {
        mutableStateOf(
            appPreferences
                .loadVoicePreviousMeansNextCategory()
        )
    }

    var searchIndicatorSoundEnabled by
    remember {
        mutableStateOf(
            appPreferences
                .loadSearchIndicatorSoundEnabled()
        )
    }

    var showVoicePicker by
    remember {
        mutableStateOf(false)
    }

    var developerModeActive by
    remember {
        mutableStateOf(com.coppersmith.music1chat.BuildConfig.DEBUG)
    }

    var versionTapCount by
    remember {
        mutableStateOf(0)
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
                Modifier
                    .fillMaxWidth(0.78f)
                    .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            color =
                MaterialTheme.colorScheme
                    .surfaceContainer
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(
                            horizontal = 22.dp,
                            vertical = 18.dp
                        )
                        .verticalScroll(rememberScrollState())
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
                    text = "Audio",
                    color =
                        MaterialTheme.colorScheme.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
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
                            text = "System feedback sounds",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text =
                                "Play a gentle sound when skipping stations or starting playback.",
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        )
                    }

                    Switch(
                        checked = searchIndicatorSoundEnabled,
                        onCheckedChange = { enabled ->
                            searchIndicatorSoundEnabled = enabled
                            appPreferences
                                .saveSearchIndicatorSoundEnabled(
                                    enabled
                                )
                        }
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
                            text = "Category announcements",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text =
                                "Speak the category name after the category changes.",
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        )
                    }

                    Switch(
                        checked = announceCategoryChanges,
                        onCheckedChange = { enabled ->
                            announceCategoryChanges = enabled
                            appPreferences
                                .saveAnnounceCategoryChanges(
                                    enabled
                                )
                        }
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                showVoicePicker = true
                            }
                            .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = "Category announcement voice",
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
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                HorizontalDivider()

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text = "Previous (Voice or Bluetooth button)",
                    color =
                        MaterialTheme.colorScheme.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text =
                        "Choose what happens when you say “Hey Google, Previous” or press the Previous button on a Bluetooth device.",
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                VoiceChoiceRow(
                    title = "Next Category",
                    subtitle =
                        "Recommended. Advancing to the next category.",
                    selected = voicePreviousMeansNextCategory,
                    onClick = {
                        voicePreviousMeansNextCategory = true
                        appPreferences
                            .saveVoicePreviousMeansNextCategory(
                                true
                            )
                    }
                )

                HorizontalDivider()

                VoiceChoiceRow(
                    title = "Previous Station",
                    subtitle =
                        "Returning to the previous station.",
                    selected = !voicePreviousMeansNextCategory,
                    onClick = {
                        voicePreviousMeansNextCategory = false
                        appPreferences
                            .saveVoicePreviousMeansNextCategory(
                                false
                            )
                    }
                )

                if (developerModeActive) {
                    Spacer(
                        modifier = Modifier.height(22.dp)
                    )

                    HorizontalDivider()

                    Spacer(
                        modifier = Modifier.height(18.dp)
                    )

                    Text(
                        text = "Diagnostics",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { RideLogger.share(context) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .weight(1f)
                        ) {
                            Text(
                                text = "Share Ride Log",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Send technical logs to the developer for troubleshooting.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { RideLogger.clearLog() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Clear Log")
                    }
                }

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                HorizontalDivider()

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text = "About",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No Hands Radio",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "The world's most accessible radio app, designed for the ride.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Version ${com.coppersmith.music1chat.BuildConfig.VERSION_NAME} (${com.coppersmith.music1chat.BuildConfig.VERSION_CODE})",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                versionTapCount++
                                if (versionTapCount >= 5) {
                                    developerModeActive = !developerModeActive
                                    versionTapCount = 0
                                }
                            }
                            .padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }
        }
    }

    if (showVoicePicker) {
        VoicePickerDialog(
            voices = installedVoices,
            voicesLoading = voicesLoading,
            onVoiceSelected = { voiceId ->
                appPreferences
                    .saveCategoryAnnouncementVoiceId(
                        voiceId
                    )

                selectedVoiceId = voiceId

                categoryAnnouncer
                    .selectVoiceForSession(voiceId)

                categoryAnnouncer
                    .testVoice("Classical")
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
    onVoiceSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedVoiceId by
    remember {
        mutableStateOf<String?>(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Category Announcement Voice",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text =
                        "Tap a voice to hear it immediately. Your choice is saved automatically.",
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
                    selected = selectedVoiceId == DEFAULT_VOICE_MARKER,
                    onClick = {
                        selectedVoiceId = DEFAULT_VOICE_MARKER
                        onVoiceSelected(null)
                    }
                )

                HorizontalDivider()

                when {
                    voicesLoading -> {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                            horizontalArrangement =
                                Arrangement.Center,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    voices.isEmpty() -> {
                        Text(
                            text =
                                "No installed English voices were found.",
                            modifier =
                                Modifier.padding(vertical = 18.dp),
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 360.dp)
                        ) {
                            items(
                                items = voices,
                                key = { voice -> voice.voiceId }
                            ) { voice ->
                                VoiceChoiceRow(
                                    title = voice.displayName,
                                    subtitle =
                                        if (voice.isNetworkVoice) {
                                            "Requires a network connection"
                                        } else {
                                            "Available offline"
                                        },
                                    selected =
                                        selectedVoiceId == voice.voiceId,
                                    onClick = {
                                        selectedVoiceId = voice.voiceId
                                        onVoiceSelected(voice.voiceId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

private const val DEFAULT_VOICE_MARKER =
    "__phone_default__"

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