package com.coppersmith.music1chat.ui.screens

// Music1Chat revision: 2026-07-19 v04 — Ride Diagnostics
// Replace the existing MainScreen.kt with this file, then rename it MainScreen.kt.
// Changes:
// - Deleting the active category selects the next category.
// - Playback continues when the deleted category was playing.
// - Saved-search categories count when choosing the replacement category.
// - The category card does nothing when the library is truly empty.
// - Deleting the final category exits the category list and clears the session.
// - Empty-library guidance directs the user to Search.
// - Adds Start/Stop/Share Ride Log controls and field diagnostics.

import android.util.Log






// Music1Chat coordinated release
// Release: 2026-07-18 v01
// DROP-IN REPLACEMENT
// Change: passes RadioPlayer live title/artist metadata to the now-playing card.
// Matched file: RadioPlayer.kt 2026-07-18 v01

// PLAYBACK SESSION INTEGRATION V2
// Search results participate as a temporary category in category navigation.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coppersmith.music1chat.GenreData
import com.coppersmith.music1chat.diagnostics.RideLogger
import com.coppersmith.music1chat.RadioPlayer
import com.coppersmith.music1chat.models.Category
import com.coppersmith.music1chat.models.CategoryType
import com.coppersmith.music1chat.persistence.AppPreferences
import com.coppersmith.music1chat.persistence.SavedSearchCategory
import com.coppersmith.music1chat.repository.MusicRepository
import com.coppersmith.music1chat.search.LiveStationSearchEngine
import com.coppersmith.music1chat.search.SearchResult
import com.coppersmith.music1chat.search.StationSearchEngine
import com.coppersmith.music1chat.session.PlaybackSessionController
import com.coppersmith.music1chat.session.PlaybackSessionMode
import com.coppersmith.music1chat.session.PlaybackSessionState
import com.coppersmith.music1chat.ui.components.CategoryCard
import com.coppersmith.music1chat.ui.components.CategoryPicker
import com.coppersmith.music1chat.ui.components.GenreSearchBox
import com.coppersmith.music1chat.ui.components.NowPlayingCard
import com.coppersmith.music1chat.ui.components.PlaybackControls
import com.coppersmith.music1chat.ui.components.SearchChips
import com.coppersmith.music1chat.ui.components.TopControlBar
import kotlinx.coroutines.launch
import com.coppersmith.music1chat.playback.MediaButtonCommand
import com.coppersmith.music1chat.playback.MediaButtonCommandBus

@Composable
fun MainScreen() {

    var pendingCategoryNavigationKey by remember {
        mutableStateOf<String?>(null)
    }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val stationSearchEngine = remember {
        StationSearchEngine()
    }

    val liveStationSearchEngine = remember {
        LiveStationSearchEngine()
    }

    val appPreferences = remember {
        AppPreferences(context.applicationContext)
    }

    val musicRepository = remember {
        MusicRepository().also { repository ->
            if (appPreferences.hasPermanentLibrary()) {
                appPreferences.restorePermanentLibrary(
                    categoryRepository =
                        repository.categories,
                    stationRepository =
                        repository.stations,
                    membershipRepository =
                        repository.memberships
                )
            } else {
                repository.seedDefaults()

                appPreferences.savePermanentLibrary(
                    categoryRepository =
                        repository.categories,
                    stationRepository =
                        repository.stations,
                    membershipRepository =
                        repository.memberships
                )
            }
        }
    }

    val repositoryCategories =
        musicRepository.categories.getAll()

    val repositoryStations =
        musicRepository.stations.getAll()

    val membershipRepository =
        musicRepository.memberships

    val savedPlaybackState = remember {
        appPreferences.restoreStationRepairs(
            repositoryStations
        )

        appPreferences.loadPlaybackState()
    }

    val initialSavedSearchCategories = remember {
        appPreferences.loadSearchCategories()
    }

    val initiallyCurrentSearch =
        initialSavedSearchCategories.firstOrNull {
            it.isCurrent
        }

    val playableCategories = remember(
        repositoryCategories
    ) {
        musicRepository.categories
            .getNavigationCategories()
            .filter { category ->
                membershipRepository
                    .getNavigationStationsForCategory(category.id)
                    .isNotEmpty()
            }
    }

    val firstPlayableCategory =
        playableCategories.firstOrNull()

    val firstPlayableStation =
        firstPlayableCategory?.let { category ->
            membershipRepository
                .getNavigationStationsForCategory(category.id)
                .firstOrNull()
        }

    val savedCategory =
        savedPlaybackState.categoryId?.let { categoryId ->
            musicRepository.categories.getById(categoryId)
        }

    val savedStation =
        savedPlaybackState.stationId?.let { stationId ->
            musicRepository.stations.getById(stationId)
        }

    val savedSelectionIsValid =
        savedCategory != null &&
                savedStation != null &&
                savedCategory.includedInNavigation &&
                savedStation.includedInNavigation &&
                !savedStation.failedThisSession &&
                membershipRepository.contains(
                    categoryId = savedCategory.id,
                    stationId = savedStation.id
                )

    val initialCategory =
        if (savedSelectionIsValid) {
            savedCategory
        } else {
            firstPlayableCategory
        }

    val initialStation =
        if (savedSelectionIsValid) {
            savedStation
        } else {
            firstPlayableStation
        }

    val shouldResumePlayback =
        savedSelectionIsValid &&
                savedPlaybackState.wasPlaying

    val initialStations =
        initialCategory?.let { category ->
            membershipRepository
                .getNavigationStationsForCategory(category.id)
        } ?: emptyList()

    val sessionController = remember {
        PlaybackSessionController(
            initialState =
                PlaybackSessionState(
                    mode = PlaybackSessionMode.CATEGORY,
                    categoryId = initialCategory?.id,
                    categoryName = initialCategory?.name.orEmpty(),
                    stations = initialStations,
                    currentIndex =
                        initialStations
                            .indexOfFirst { station ->
                                station.id == initialStation?.id
                            }
                            .takeIf { index ->
                                index >= 0
                            } ?: 0,
                    playbackRequested =
                        shouldResumePlayback
                )
        )
    }

    var sessionState by remember {
        mutableStateOf(
            sessionController.getState()
        )
    }

    val sessionStateRef = remember {
        mutableStateOf(sessionState)
    }

    val radioPlayer = remember {
        RadioPlayer(context.applicationContext)
    }

    var navigationStatusMessage by remember {
        mutableStateOf<String?>(null)
    }

    var startupRestoreComplete by remember {
        mutableStateOf(false)
    }

    var rideLogActive by remember {
        mutableStateOf(RideLogger.isActive)
    }

    var rideLogAvailable by remember {
        mutableStateOf(RideLogger.hasLog)
    }

    var stationStateVersion by remember {
        mutableIntStateOf(0)
    }

    var showCategoryList by remember {
        mutableStateOf(false)
    }

    var stationListCategoryKey by remember {
        mutableStateOf<String?>(null)
    }

    var deleteCategoryKey by remember {
        mutableStateOf<String?>(null)
    }

    var stationToSaveElsewhere by remember {
        mutableStateOf<com.coppersmith.music1chat.models.Station?>(null)
    }

    var destinationCategorySearchText by remember {
        mutableStateOf("")
    }

    var destinationCategoryPickerTitle by remember {
        mutableStateOf("Save to another category")
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var latestSearchRequest by remember {
        mutableIntStateOf(0)
    }

    var showGenreMenu by remember {
        mutableStateOf(false)
    }

    // Search result sessions are cached in memory by normalized query.
    var searchSessionStates by remember {
        mutableStateOf<Map<String, PlaybackSessionState>>(
            emptyMap()
        )
    }

    var savedSearchCategories by remember {
        mutableStateOf(initialSavedSearchCategories)
    }

    val libraryHasCategories =
        repositoryCategories.isNotEmpty() ||
                savedSearchCategories.isNotEmpty()

    var activeSearchQuery by remember {
        mutableStateOf(
            initiallyCurrentSearch?.query
        )
    }


    var searchAnchorCategoryId by remember {
        mutableStateOf(
            initiallyCurrentSearch?.anchorCategoryId
                ?: initialCategory?.id
        )
    }

    val genres = GenreData.MAJOR_GENRES

    stationStateVersion

    val displayedStation =
        sessionState.currentStation

    val displayedStationIndex =
        sessionState.safeCurrentIndex

    val displayedStationCount =
        sessionState.stationCount

    val isPlaying =
        radioPlayer.isPlaying

    val currentPermanentCategory =
        sessionState.categoryId?.let { categoryId ->
            musicRepository.categories.getById(categoryId)
        }

    val searchSuggestions: List<String> = remember(
        searchText,
        genres,
        repositoryStations
    ) {
        val typedText = searchText.trim()

        if (typedText.isBlank()) {
            genres
        } else {
            val genreSuggestions =
                genres.filter { genre ->
                    genre.startsWith(
                        typedText,
                        ignoreCase = true
                    )
                }

            val stationSuggestions =
                repositoryStations
                    .flatMap { station ->
                        buildList {
                            if (station.callLetters.isNotBlank()) {
                                add(station.callLetters)
                            }

                            add(station.name)
                        }
                    }
                    .filter { suggestion ->
                        suggestion.contains(
                            typedText,
                            ignoreCase = true
                        )
                    }

            (genreSuggestions + stationSuggestions)
                .distinctBy { suggestion ->
                    suggestion.lowercase()
                }
                .take(12)
        }
    }

    fun normalizedSearchKey(
        query: String
    ): String = query.trim().lowercase()

    fun savedSearchFor(
        query: String
    ): SavedSearchCategory? =
        savedSearchCategories.firstOrNull {
            it.query.equals(
                query,
                ignoreCase = true
            )
        }

    fun replaceSavedSearches(
        updated: List<SavedSearchCategory>
    ) {
        savedSearchCategories = updated
    }

    fun navigationKeys(): List<String> {
        val permanent =
            musicRepository.categories
                .getNavigationCategories()
                .filter { category ->
                    membershipRepository
                        .getNavigationStationsForCategory(category.id)
                        .isNotEmpty()
                }

        val enabledSearches =
            savedSearchCategories
                .filter { it.navigationEnabled }
                .sortedBy { it.sortOrder }

        val result = mutableListOf<String>()

        permanent.forEach { category ->
            result.add("category:${category.id}")

            enabledSearches
                .filter { search ->
                    search.anchorCategoryId == category.id
                }
                .forEach { search ->
                    result.add("search:${search.query}")
                }
        }

        enabledSearches
            .filter { search ->
                permanent.none { category ->
                    category.id == search.anchorCategoryId
                }
            }
            .forEach { search ->
                result.add("search:${search.query}")
            }

        return result
    }

    fun publishSession(
        newState: PlaybackSessionState
    ) {
        sessionState = newState
        sessionStateRef.value = newState

        if (newState.isSearch && newState.hasStations) {
            activeSearchQuery = newState.categoryName
            searchSessionStates =
                searchSessionStates +
                        (
                                normalizedSearchKey(
                                    newState.categoryName
                                ) to newState
                                )
        }
    }

    fun saveCurrentState(
        state: PlaybackSessionState,
        wasPlaying: Boolean
    ) {
        if (state.isSearch) {
            appPreferences.saveWasPlaying(wasPlaying)

            val query =
                state.categoryName.ifBlank {
                    activeSearchQuery.orEmpty()
                }

            if (query.isNotBlank()) {
                val existing =
                    savedSearchFor(query)

                replaceSavedSearches(
                    appPreferences.upsertSearchCategory(
                        SavedSearchCategory(
                            query = query,
                            anchorCategoryId =
                                existing?.anchorCategoryId
                                    ?: searchAnchorCategoryId,
                            lastResultCount =
                                state.stationCount.takeIf {
                                    it > 0
                                } ?: existing?.lastResultCount
                                ?: 0,
                            isCurrent = true,
                            currentStationId =
                                state.currentStation?.id,
                            currentIndex =
                                state.safeCurrentIndex,
                            navigationEnabled =
                                existing?.navigationEnabled
                                    ?: true,
                            sortOrder =
                                existing?.sortOrder
                                    ?: savedSearchCategories.size
                        )
                    )
                )
            }

            return
        }

        appPreferences.savePlaybackState(
            categoryId = state.categoryId,
            stationId = state.currentStation?.id,
            wasPlaying = wasPlaying
        )

        replaceSavedSearches(
            appPreferences.markCurrentSearch(null)
        )
        activeSearchQuery = null
    }

    fun playCurrentSessionStation(
        state: PlaybackSessionState
    ) {
        val station =
            state.currentStation
                ?: run {
                    RideLogger.log(
                        "PLAY_REQUEST_SKIPPED reason='no current station' " +
                                "category='${state.categoryDisplayName}'"
                    )
                    return
                }

        val source =
            if (state.isSearch) {
                RadioPlayer.PlaybackSource.SEARCH
            } else {
                RadioPlayer.PlaybackSource.NAVIGATION
            }

        val playbackUrl =
            station.resolvedStreamUrl
                .ifBlank {
                    station.streamUrl
                }

        RideLogger.log(
            "PLAY_REQUEST " +
                    "category='${state.categoryDisplayName}' " +
                    "station='${station.name}' " +
                    "stationId=${station.id} " +
                    "source=$source " +
                    "url='$playbackUrl'"
        )

        radioPlayer.play(
            station = station,
            source = source
        )

        RideLogger.log(
            "PLAY_REQUEST_SENT " +
                    "station='${station.name}' " +
                    "stationId=${station.id}"
        )
    }

    lateinit var runSearchAction:
                (String, Boolean, Boolean) -> Unit

    fun restoreSearch(
        query: String,
        startPlayback: Boolean = true
    ) {
        val cached =
            searchSessionStates[
                normalizedSearchKey(query)
            ]

        if (cached == null || !cached.hasStations) {
            runSearchAction(
                query,
                startPlayback,
                true
            )
            return
        }

        val restoredState =
            sessionController.showSearch(
                query = cached.categoryName,
                stations = cached.stations,
                preferredStationId =
                    cached.currentStation?.id,
                startPlayback = startPlayback
            )

        publishSession(restoredState)
        navigationStatusMessage = null

        saveCurrentState(
            state = restoredState,
            wasPlaying = startPlayback
        )

        if (startPlayback) {
            playCurrentSessionStation(restoredState)
        }
    }

    fun selectCategory(
        category: Category,
        preferredStationId: Long? = null,
        startPlayback: Boolean = true
    ) {
        val stations =
            membershipRepository
                .getNavigationStationsForCategory(category.id)

        val newState =
            sessionController.showCategory(
                categoryId = category.id,
                categoryName = category.name,
                stations = stations,
                preferredStationId = preferredStationId,
                startPlayback = startPlayback
            )

        publishSession(newState)
        navigationStatusMessage = null

        saveCurrentState(
            state = newState,
            wasPlaying = startPlayback
        )

        if (startPlayback) {
            playCurrentSessionStation(newState)
        }
    }

    fun changeCategory(
        direction: Int
    ) {
        val beforeState = sessionStateRef.value
        RideLogger.log(
            "CATEGORY_COMMAND direction=$direction beforeCategory='${beforeState.categoryDisplayName}' " +
                    "beforeStation='${beforeState.currentStation?.name.orEmpty()}'"
        )

        val keys =
            navigationKeys()

        if (keys.isEmpty()) {
            radioPlayer.stop()
            publishSession(
                sessionController.stop()
            )
            navigationStatusMessage =
                "No playable categories are available."
            return
        }

        val currentState =
            sessionStateRef.value

        val currentKey =
            pendingCategoryNavigationKey
                ?: if (currentState.isSearch) {
                    "search:${currentState.categoryName}"
                } else {
                    "category:${currentState.categoryId}"
                }

        val currentIndex =
            keys.indexOfFirst {
                it.equals(
                    currentKey,
                    ignoreCase = true
                )
            }

        val selectedIndex =
            when {
                currentIndex < 0 -> 0
                direction < 0 && currentIndex <= 0 ->
                    keys.lastIndex
                direction < 0 ->
                    currentIndex - 1
                direction > 0 &&
                        currentIndex >= keys.lastIndex ->
                    0
                else ->
                    currentIndex + 1
            }

        val selectedKey =
            keys[selectedIndex]

        pendingCategoryNavigationKey = selectedKey

        RideLogger.log(
            "CATEGORY_TARGET currentKey='$currentKey' " +
                    "selectedKey='$selectedKey' " +
                    "cached=${
                        selectedKey.startsWith("search:") &&
                                searchSessionStates.containsKey(
                                    normalizedSearchKey(
                                        selectedKey.removePrefix("search:")
                                    )
                                )
                    }"
        )

        if (selectedKey.startsWith("search:")) {
            restoreSearch(
                query =
                    selectedKey.removePrefix(
                        "search:"
                    ),
                startPlayback = true
            )
        } else {
            val categoryId =
                selectedKey.removePrefix(
                    "category:"
                ).toLongOrNull()
            val category =
                categoryId?.let {
                    musicRepository.categories
                        .getById(it)
                }

            if (category != null) {
                selectCategory(
                    category = category,
                    startPlayback = true
                )
            }
        }

        val afterState = sessionStateRef.value
        RideLogger.log(
            "CATEGORY_RESULT afterCategory='${afterState.categoryDisplayName}' " +
                    "afterStation='${afterState.currentStation?.name.orEmpty()}'"
        )
    }

    fun moveStation(
        direction: Int
    ) {
        val beforeState = sessionStateRef.value
        RideLogger.log(
            "STATION_COMMAND direction=$direction category='${beforeState.categoryDisplayName}' " +
                    "before='${beforeState.currentStation?.name.orEmpty()}' " +
                    "count=${beforeState.stationCount}"
        )

        val newState =
            if (direction < 0) {
                sessionController.previousStation(
                    startPlayback = true
                )
            } else {
                sessionController.nextStation(
                    startPlayback = true
                )
            }

        publishSession(newState)
        navigationStatusMessage = null

        saveCurrentState(
            state = newState,
            wasPlaying = true
        )

        playCurrentSessionStation(newState)

        RideLogger.log(
            "STATION_RESULT category='${newState.categoryDisplayName}' " +
                    "after='${newState.currentStation?.name.orEmpty()}' " +
                    "index=${newState.safeCurrentIndex + 1}/${newState.stationCount}"
        )
    }

    fun stopPlayback() {
        RideLogger.log(
            "PLAYBACK_STOP station='${sessionStateRef.value.currentStation?.name.orEmpty()}'"
        )
        radioPlayer.stop()

        val newState =
            sessionController.stop()

        publishSession(newState)

        saveCurrentState(
            state = newState,
            wasPlaying = false
        )
    }

    fun startPlayback() {
        RideLogger.log(
            "PLAYBACK_START station='${sessionStateRef.value.currentStation?.name.orEmpty()}'"
        )
        val newState =
            sessionController.play()

        publishSession(newState)

        if (!newState.hasStations) {
            navigationStatusMessage =
                if (musicRepository.categories.getAll().isEmpty()) {
                    "No categories are available. Search to add a category and stations."
                } else {
                    "No stations are available."
                }
            return
        }

        saveCurrentState(
            state = newState,
            wasPlaying = true
        )

        playCurrentSessionStation(newState)
    }

    fun runSearch(
        query: String,
        startPlayback: Boolean,
        preserveAnchor: Boolean,
        onComplete: (() -> Unit)? = null
    ) {
        val searchQuery =
            query.trim()

        if (searchQuery.isBlank()) {
            return
        }

        val stateBeforeSearch =
            sessionStateRef.value

        val existingSavedSearch =
            savedSearchFor(searchQuery)

        if (existingSavedSearch != null) {
            searchAnchorCategoryId =
                existingSavedSearch.anchorCategoryId
                    ?: searchAnchorCategoryId
        } else if (
            !preserveAnchor &&
            !stateBeforeSearch.isSearch
        ) {
            searchAnchorCategoryId =
                stateBeforeSearch.categoryId
                    ?: searchAnchorCategoryId
        }

        showGenreMenu = false
        focusManager.clearFocus()
        searchText = ""

        latestSearchRequest++

        val thisSearchRequest =
            latestSearchRequest

        navigationStatusMessage =
            "Searching for $searchQuery..."

        coroutineScope.launch {
            try {
                val localResult =
                    stationSearchEngine.search(
                    query = searchQuery,
                    stations = repositoryStations
                )

            val liveResult =
                runCatching {
                    liveStationSearchEngine.search(
                        query = searchQuery,
                        limit = 50
                    )
                }.getOrElse {
                    SearchResult(
                        query = searchQuery,
                        stations = emptyList()
                    )
                }

            if (thisSearchRequest != latestSearchRequest) {
                return@launch
            }

            Log.d(
                "KenCheck",
                "Search submitted='$searchQuery', " +
                        "local=${localResult.stations.size}, " +
                        "live=${liveResult.stations.size}"
            )

            val mergedStations =
                (localResult.stations + liveResult.stations)
                    .onEach { station ->
                        station.includedInNavigation = true
                    }
                    .distinctBy { station ->
                        station.resolvedStreamUrl
                            .ifBlank {
                                station.streamUrl
                            }
                            .trim()
                            .lowercase()
                            .ifBlank {
                                station.name
                                    .trim()
                                    .lowercase()
                            }
                    }

            Log.d(
                "KenCheck",
                "Search merged='$searchQuery', results=${mergedStations.size}"
            )

                val savedSearch =
                    savedSearchFor(searchQuery)

                if (mergedStations.isEmpty()) {
                    /*
                     * A temporary Radio Browser failure must not replace a good
                     * cached search with an empty category or overwrite its saved
                     * station count with zero.
                     */
                    val cachedSearch =
                        searchSessionStates[
                            normalizedSearchKey(searchQuery)
                        ]?.takeIf { it.hasStations }

                    if (cachedSearch != null) {
                        val restoredState =
                            sessionController.showSearch(
                                query = cachedSearch.categoryName,
                                stations = cachedSearch.stations,
                                preferredStationId =
                                    cachedSearch.currentStation?.id,
                                startPlayback = startPlayback
                            )

                        publishSession(restoredState)
                        activeSearchQuery = searchQuery

                        navigationStatusMessage =
                            "Search refresh failed. Showing the previous results."

                        if (startPlayback) {
                            playCurrentSessionStation(restoredState)
                        }
                    } else {
                        navigationStatusMessage =
                            "No matching stations found. Try the search again."
                    }

                    return@launch
                }

                radioPlayer.stop()

                val preferredSearchStationId =
                    savedSearch?.currentStationId
                        ?.takeIf { savedId ->
                            mergedStations.any { station ->
                                station.id == savedId
                            }
                        }
                        ?: savedSearch?.currentIndex
                            ?.let { savedIndex ->
                                mergedStations
                                    .getOrNull(savedIndex)
                                    ?.id
                            }

                val newState =
                    sessionController.showSearch(
                        query = searchQuery,
                        stations = mergedStations,
                        preferredStationId =
                            preferredSearchStationId,
                        startPlayback = startPlayback
                    )

                publishSession(newState)

                replaceSavedSearches(
                    appPreferences.upsertSearchCategory(
                        SavedSearchCategory(
                            query = searchQuery,
                            anchorCategoryId =
                                savedSearch?.anchorCategoryId
                                    ?: searchAnchorCategoryId,
                            lastResultCount =
                                mergedStations.size,
                            isCurrent = true,
                            currentStationId =
                                newState.currentStation?.id,
                            currentIndex =
                                newState.safeCurrentIndex,
                            navigationEnabled =
                                savedSearch?.navigationEnabled
                                    ?: true,
                            sortOrder =
                                savedSearch?.sortOrder
                                    ?: savedSearchCategories.size
                        )
                    )
                )

                activeSearchQuery = searchQuery

                appPreferences.saveWasPlaying(startPlayback)
                navigationStatusMessage = null

                if (startPlayback) {
                    playCurrentSessionStation(newState)
                }
            } finally {
                onComplete?.invoke()
            }
        }
    }

    runSearchAction = { query, startPlayback, preserveAnchor ->
        runSearch(
            query = query,
            startPlayback = startPlayback,
            preserveAnchor = preserveAnchor
        )
    }

    val submitSearch: () -> Unit = {
        val typedText =
            searchText.trim()

        val exactMatch =
            genres.firstOrNull { genre ->
                genre.equals(
                    typedText,
                    ignoreCase = true
                )
            }

        /*
         * Preserve the exact submitted text. Suggestions are choices only;
         * they must not silently replace microphone or keyboard input.
         */
        val selectedSearch =
            exactMatch ?: typedText

        if (selectedSearch.isNotBlank()) {
            runSearch(
                query = selectedSearch,
                startPlayback = true,
                preserveAnchor = false
            )
        }
    }

    fun stationsForCategoryKey(
        key: String
    ) =
        if (key.startsWith("search:")) {
            val query = key.removePrefix("search:")

            searchSessionStates[
                normalizedSearchKey(query)
            ]?.stations.orEmpty()
        } else {
            val categoryId =
                key.removePrefix("category:")
                    .toLongOrNull()

            categoryId?.let {
                membershipRepository
                    .getStationsForCategory(it)
            }.orEmpty()
        }

    fun openStationList(
        key: String
    ) {
        stationListCategoryKey = key
        showCategoryList = false

        if (key.startsWith("search:")) {
            val query = key.removePrefix("search:")

            val cachedSearch =
                searchSessionStates[
                    normalizedSearchKey(query)
                ]

            if (cachedSearch == null || !cachedSearch.hasStations) {
                runSearchAction(
                    query,
                    false,
                    true
                )
            }
        }
    }

    fun categoryDisplayNameForKey(
        key: String
    ): String =
        if (key.startsWith("search:")) {
            "Search: ${key.removePrefix("search:")}"
        } else {
            val categoryId =
                key.removePrefix("category:")
                    .toLongOrNull()

            categoryId?.let {
                musicRepository.categories
                    .getById(it)
                    ?.name
            }.orEmpty()
        }

    fun deleteCategory(
        key: String
    ) {
        val wasPlayingBeforeDelete = radioPlayer.isPlaying

        fun orderedCategoryKeys(): List<String> {
            val permanentCategories =
                musicRepository.categories.getAll()

            val permanentIds =
                permanentCategories.map { it.id }.toSet()

            return buildList {
                permanentCategories.forEach { category ->
                    add("category:${category.id}")

                    savedSearchCategories
                        .filter { search ->
                            search.anchorCategoryId == category.id
                        }
                        .sortedBy { it.sortOrder }
                        .forEach { search ->
                            add("search:${search.query}")
                        }
                }

                savedSearchCategories
                    .filter { search ->
                        search.anchorCategoryId !in permanentIds
                    }
                    .sortedBy { it.sortOrder }
                    .forEach { search ->
                        add("search:${search.query}")
                    }
            }
        }

        val keysBeforeDelete = orderedCategoryKeys()
        val deletingIndex =
            keysBeforeDelete.indexOfFirst { candidate ->
                candidate.equals(key, ignoreCase = true)
            }

        val nextKeyAfterDelete =
            if (deletingIndex >= 0 && keysBeforeDelete.size > 1) {
                keysBeforeDelete[
                    (deletingIndex + 1) % keysBeforeDelete.size
                ]
            } else {
                null
            }

        val currentKey =
            if (sessionStateRef.value.isSearch) {
                "search:${sessionStateRef.value.categoryName}"
            } else {
                "category:${sessionStateRef.value.categoryId}"
            }

        val deletingCurrent =
            currentKey.equals(key, ignoreCase = true)

        if (key.startsWith("search:")) {
            val query = key.removePrefix("search:")

            replaceSavedSearches(
                appPreferences.removeSearchCategory(query)
            )

            searchSessionStates =
                searchSessionStates -
                        normalizedSearchKey(query)

            if (
                activeSearchQuery.equals(
                    query,
                    ignoreCase = true
                )
            ) {
                activeSearchQuery = null
            }
        } else {
            val categoryId =
                key.removePrefix("category:")
                    .toLongOrNull()

            if (categoryId != null) {
                membershipRepository.removeCategory(
                    categoryId
                )
                musicRepository.categories.remove(
                    categoryId
                )

                appPreferences.savePermanentLibrary(
                    categoryRepository =
                        musicRepository.categories,
                    stationRepository =
                        musicRepository.stations,
                    membershipRepository =
                        membershipRepository
                )

                stationStateVersion++
            }
        }

        deleteCategoryKey = null

        if (stationListCategoryKey == key) {
            stationListCategoryKey = null
            showCategoryList = true
        }

        val remainingKeys = orderedCategoryKeys()

        if (remainingKeys.isEmpty()) {
            radioPlayer.stop()

            val emptyState = sessionController.clear()
            publishSession(emptyState)

            showCategoryList = false
            stationListCategoryKey = null
            navigationStatusMessage =
                "No categories are available. Search to add a category and stations."

            appPreferences.savePlaybackState(
                categoryId = null,
                stationId = null,
                wasPlaying = false
            )

            return
        }

        if (!deletingCurrent) {
            return
        }

        radioPlayer.stop()

        val replacementKey =
            nextKeyAfterDelete?.takeIf { candidate ->
                remainingKeys.any { remaining ->
                    remaining.equals(candidate, ignoreCase = true)
                }
            } ?: remainingKeys.first()

        if (replacementKey.startsWith("search:")) {
            restoreSearch(
                query = replacementKey.removePrefix("search:"),
                startPlayback = wasPlayingBeforeDelete
            )
        } else {
            val replacementCategoryId =
                replacementKey.removePrefix("category:")
                    .toLongOrNull()

            val replacementCategory =
                replacementCategoryId?.let { id ->
                    musicRepository.categories.getById(id)
                }

            if (replacementCategory != null) {
                selectCategory(
                    category = replacementCategory,
                    startPlayback = wasPlayingBeforeDelete
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        if (
            initiallyCurrentSearch != null &&
            initiallyCurrentSearch.query.isNotBlank()
        ) {
            runSearch(
                query = initiallyCurrentSearch.query,
                startPlayback = savedPlaybackState.wasPlaying,
                preserveAnchor = true,
                onComplete = {
                    startupRestoreComplete = true
                }
            )
        } else {
            saveCurrentState(
                state = sessionState,
                wasPlaying = shouldResumePlayback
            )

            if (
                shouldResumePlayback &&
                sessionState.currentStation != null
            ) {
                playCurrentSessionStation(sessionState)
            }

            startupRestoreComplete = true
        }
    }

    LaunchedEffect(Unit) {
        MediaButtonCommandBus.commands.collect { command ->
            RideLogger.log(
                "MEDIA_COMMAND command=$command playing=${radioPlayer.isPlaying} " +
                        "category='${sessionStateRef.value.categoryDisplayName}' " +
                        "station='${sessionStateRef.value.currentStation?.name.orEmpty()}'"
            )

            when (command) {
                MediaButtonCommand.TOGGLE_PLAYBACK -> {
                    if (radioPlayer.isPlaying) {
                        stopPlayback()
                    } else {
                        startPlayback()
                    }
                }

                MediaButtonCommand.NEXT_STATION -> {
                    moveStation(direction = 1)
                }

                MediaButtonCommand.NEXT_CATEGORY -> {
                    changeCategory(direction = 1)
                }
            }
        }
    }


    DisposableEffect(
        radioPlayer,
        sessionController
    ) {
        radioPlayer.onStationFailed =
            failureCallback@{ failedStation ->
                RideLogger.log(
                    "STATION_FAILED id=${failedStation.id} name='${failedStation.name}'"
                )

                val activeStation =
                    radioPlayer.activeStation

                if (
                    activeStation == null ||
                    activeStation.id != failedStation.id
                ) {
                    return@failureCallback
                }

                val oldState =
                    sessionStateRef.value

                val newState =
                    sessionController
                        .markCurrentStationFailedAndAdvance()

                publishSession(newState)

                if (newState.hasStations) {
                    if (!newState.isSearch) {
                        saveCurrentState(
                            state = newState,
                            wasPlaying = true
                        )
                    }

                    playCurrentSessionStation(newState)
                } else if (oldState.isSearch) {
                    searchSessionStates =
                        searchSessionStates -
                                normalizedSearchKey(
                                    oldState.categoryName
                                )
                    radioPlayer.stop()
                    navigationStatusMessage =
                        "No playable search results remain."
                } else {
                    changeCategory(direction = 1)
                }
            }

        onDispose {
            saveCurrentState(
                state = sessionStateRef.value,
                wasPlaying = radioPlayer.isPlaying
            )

            radioPlayer.onStationFailed = null
            radioPlayer.release()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val stationListKey =
            stationListCategoryKey

        if (stationListKey != null) {
            val stationListStations =
                stationsForCategoryKey(stationListKey)

            StationListScreen(
                categoryName =
                    categoryDisplayNameForKey(
                        stationListKey
                    ),
                stations = stationListStations,
                selectedStationId =
                    if (
                        stationListKey.startsWith("search:") &&
                        sessionState.isSearch &&
                        stationListKey.equals(
                            "search:${sessionState.categoryName}",
                            ignoreCase = true
                        )
                    ) {
                        displayedStation?.id
                    } else if (
                        stationListKey ==
                        "category:${sessionState.categoryId}"
                    ) {
                        displayedStation?.id
                    } else {
                        null
                    },
                reorderEnabled =
                    !stationListKey.startsWith("search:"),
                stateVersion = stationStateVersion,
                onCloseClick = {
                    stationListCategoryKey = null
                },
                onStationClick = { station ->
                    if (stationListKey.startsWith("search:")) {
                        val query =
                            stationListKey.removePrefix(
                                "search:"
                            )

                        val cached =
                            searchSessionStates[
                                normalizedSearchKey(query)
                            ]

                        if (cached != null) {
                            val selectedState =
                                sessionController.showSearch(
                                    query = query,
                                    stations = cached.stations,
                                    preferredStationId =
                                        station.id,
                                    startPlayback = true
                                )

                            publishSession(selectedState)
                            saveCurrentState(
                                selectedState,
                                true
                            )
                            playCurrentSessionStation(
                                selectedState
                            )
                        }
                    } else {
                        val categoryId =
                            stationListKey
                                .removePrefix("category:")
                                .toLongOrNull()

                        val category =
                            categoryId?.let {
                                musicRepository.categories
                                    .getById(it)
                            }

                        if (category != null) {
                            selectCategory(
                                category = category,
                                preferredStationId =
                                    station.id,
                                startPlayback = true
                            )
                        }
                    }
                },
                onNavigationToggle = { station ->
                    station.includedInNavigation =
                        !station.includedInNavigation

                    appPreferences
                        .savePermanentLibrary(
                            categoryRepository =
                                musicRepository.categories,
                            stationRepository =
                                musicRepository.stations,
                            membershipRepository =
                                membershipRepository
                        )

                    val activeState =
                        sessionController.getState()

                    if (
                        activeState.stations.any {
                            it.id == station.id
                        }
                    ) {
                        publishSession(activeState)
                        saveCurrentState(
                            state = activeState,
                            wasPlaying =
                                radioPlayer.isPlaying
                        )
                    }

                    stationStateVersion++
                },
                onMoveStation = { station, newPosition ->
                    val categoryId =
                        stationListKey
                            .removePrefix("category:")
                            .toLongOrNull()

                    if (categoryId != null) {
                        membershipRepository.moveStation(
                            categoryId = categoryId,
                            stationId = station.id,
                            newPosition = newPosition
                        )

                        appPreferences
                            .savePermanentLibrary(
                                categoryRepository =
                                    musicRepository.categories,
                                stationRepository =
                                    musicRepository.stations,
                                membershipRepository =
                                    membershipRepository
                            )

                        val currentState =
                            sessionStateRef.value

                        if (
                            !currentState.isSearch &&
                            currentState.categoryId ==
                            categoryId
                        ) {
                            val reorderedStations =
                                membershipRepository
                                    .getStationsForCategory(
                                        categoryId
                                    )

                            val refreshedState =
                                sessionController.showCategory(
                                    categoryId = categoryId,
                                    categoryName =
                                        currentState.categoryName,
                                    stations =
                                        reorderedStations,
                                    preferredStationId =
                                        currentState.currentStation
                                            ?.id,
                                    startPlayback =
                                        radioPlayer.isPlaying
                                )

                            publishSession(refreshedState)
                            saveCurrentState(
                                state = refreshedState,
                                wasPlaying =
                                    radioPlayer.isPlaying
                            )
                        }

                        stationStateVersion++
                    }
                },
                onDeleteStation = { station ->
                    val categoryId =
                        stationListKey
                            .removePrefix("category:")
                            .toLongOrNull()

                    if (categoryId != null) {
                        val currentState =
                            sessionStateRef.value

                        val deletingCurrentStation =
                            !currentState.isSearch &&
                                    currentState.categoryId ==
                                    categoryId &&
                                    currentState.currentStation
                                        ?.id == station.id

                        val oldIndex =
                            currentState.safeCurrentIndex

                        membershipRepository
                            .removeStationFromCategory(
                                categoryId = categoryId,
                                stationId = station.id
                            )

                        appPreferences
                            .savePermanentLibrary(
                                categoryRepository =
                                    musicRepository.categories,
                                stationRepository =
                                    musicRepository.stations,
                                membershipRepository =
                                    membershipRepository
                            )

                        val remainingStations =
                            membershipRepository
                                .getStationsForCategory(
                                    categoryId
                                )

                        if (
                            !currentState.isSearch &&
                            currentState.categoryId ==
                            categoryId
                        ) {
                            val category =
                                musicRepository.categories
                                    .getById(categoryId)

                            if (category != null) {
                                val preferredStation =
                                    if (
                                        deletingCurrentStation
                                    ) {
                                        remainingStations
                                            .getOrNull(
                                                oldIndex.coerceAtMost(
                                                    remainingStations
                                                        .lastIndex
                                                        .coerceAtLeast(0)
                                                )
                                            )
                                    } else {
                                        currentState
                                            .currentStation
                                            ?.let { current ->
                                                remainingStations
                                                    .firstOrNull {
                                                        it.id ==
                                                                current.id
                                                    }
                                            }
                                    }

                                val refreshedState =
                                    sessionController
                                        .showCategory(
                                            categoryId =
                                                categoryId,
                                            categoryName =
                                                category.name,
                                            stations =
                                                remainingStations,
                                            preferredStationId =
                                                preferredStation
                                                    ?.id,
                                            startPlayback =
                                                radioPlayer
                                                    .isPlaying &&
                                                        remainingStations
                                                            .isNotEmpty()
                                        )

                                publishSession(
                                    refreshedState
                                )

                                saveCurrentState(
                                    state = refreshedState,
                                    wasPlaying =
                                        radioPlayer.isPlaying &&
                                                remainingStations
                                                    .isNotEmpty()
                                )

                                if (
                                    deletingCurrentStation &&
                                    remainingStations
                                        .isNotEmpty() &&
                                    radioPlayer.isPlaying
                                ) {
                                    playCurrentSessionStation(
                                        refreshedState
                                    )
                                } else if (
                                    remainingStations.isEmpty()
                                ) {
                                    radioPlayer.stop()
                                }
                            }
                        }

                        stationStateVersion++
                    }
                }
            )
        } else if (showCategoryList) {
            val permanentCategoryRows =
                repositoryCategories.associate { category ->
                    "category:${category.id}" to
                            CategorySummary(
                                key = "category:${category.id}",
                                name = category.name,
                                stationCount =
                                    membershipRepository
                                        .getStationsForCategory(
                                            category.id
                                        )
                                        .size,
                                includedInNavigation =
                                    category.includedInNavigation
                            )
                }

            val searchCategoryRows =
                savedSearchCategories.associate { search ->
                    "search:${search.query}" to
                            CategorySummary(
                                key = "search:${search.query}",
                                name = "Search: ${search.query}",
                                stationCount =
                                    searchSessionStates[
                                        normalizedSearchKey(
                                            search.query
                                        )
                                    ]?.stationCount
                                        ?: search.lastResultCount,
                                includedInNavigation =
                                    search.navigationEnabled
                            )
                }

            val orderedKeys =
                buildList {
                    val permanentIds =
                        repositoryCategories.map {
                            it.id
                        }

                    repositoryCategories.forEach { category ->
                        add("category:${category.id}")

                        savedSearchCategories
                            .filter {
                                it.anchorCategoryId ==
                                        category.id
                            }
                            .sortedBy { it.sortOrder }
                            .forEach {
                                add("search:${it.query}")
                            }
                    }

                    savedSearchCategories
                        .filter {
                            it.anchorCategoryId !in
                                    permanentIds
                        }
                        .sortedBy { it.sortOrder }
                        .forEach {
                            add("search:${it.query}")
                        }
                }

            val categoryRows =
                orderedKeys.mapNotNull { key ->
                    permanentCategoryRows[key]
                        ?: searchCategoryRows[key]
                }

            CategoryListScreen(
                categories = categoryRows,
                selectedCategoryKey =
                    if (sessionState.isSearch) {
                        "search:${sessionState.categoryName}"
                    } else {
                        "category:${sessionState.categoryId}"
                    },
                onBackClick = {
                    showCategoryList = false
                },
                onCategoryClick = { categorySummary ->
                    if (
                        categorySummary.key.startsWith(
                            "search:"
                        )
                    ) {
                        showCategoryList = false
                        restoreSearch(
                            query =
                                categorySummary.key
                                    .removePrefix(
                                        "search:"
                                    ),
                            startPlayback = true
                        )
                    } else {
                        val categoryId =
                            categorySummary.key
                                .removePrefix("category:")
                                .toLongOrNull()

                        val selectedCategory =
                            categoryId?.let { id ->
                                musicRepository.categories
                                    .getById(id)
                            }

                        if (selectedCategory != null) {
                            showCategoryList = false

                            selectCategory(
                                category = selectedCategory,
                                startPlayback = true
                            )
                        }
                    }
                },
                onListClick = { categoryKey ->
                    openStationList(categoryKey)
                },
                onDeleteClick = { categoryKey ->
                    deleteCategoryKey = categoryKey
                },
                onNavigationToggle = { categoryKey ->
                    if (categoryKey.startsWith("search:")) {
                        val query =
                            categoryKey.removePrefix(
                                "search:"
                            )

                        val saved =
                            savedSearchFor(query)

                        if (saved != null) {
                            replaceSavedSearches(
                                appPreferences
                                    .setSearchNavigation(
                                        query = query,
                                        enabled =
                                            !saved.navigationEnabled
                                    )
                            )
                            stationStateVersion++
                        }
                    } else {
                        val categoryId =
                            categoryKey
                                .removePrefix("category:")
                                .toLongOrNull()

                        val category =
                            categoryId?.let { id ->
                                musicRepository.categories
                                    .getById(id)
                            }

                        if (category != null) {
                            musicRepository.categories
                                .setNavigation(
                                    category.id,
                                    !category
                                        .includedInNavigation
                                )

                            stationStateVersion++
                        }
                    }
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(
                        start = 14.dp,
                        end = 14.dp,
                        top = 10.dp,
                        bottom = 12.dp
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                TopControlBar()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (rideLogActive) {
                                RideLogger.stop()
                                rideLogActive = false
                                rideLogAvailable = RideLogger.hasLog
                                navigationStatusMessage =
                                    "Ride log stopped."
                            } else {
                                val result = RideLogger.start(context)
                                rideLogActive = result.isSuccess
                                rideLogAvailable = RideLogger.hasLog
                                navigationStatusMessage =
                                    result.fold(
                                        onSuccess = {
                                            "Ride log started."
                                        },
                                        onFailure = { error ->
                                            error.message
                                                ?: "Unable to start ride log."
                                        }
                                    )

                                if (result.isSuccess) {
                                    RideLogger.log(
                                        "INITIAL_STATE playing=${radioPlayer.isPlaying} " +
                                                "category='${sessionState.categoryDisplayName}' " +
                                                "station='${sessionState.currentStation?.name.orEmpty()}'"
                                    )
                                }
                            }
                        }
                    ) {
                        Text(
                            if (rideLogActive) {
                                "Stop Ride Log"
                            } else {
                                "Start Ride Log"
                            }
                        )
                    }

                    if (!rideLogActive && rideLogAvailable) {
                        TextButton(
                            onClick = {
                                RideLogger.share(context)
                                    .onFailure { error ->
                                        navigationStatusMessage =
                                            error.message
                                                ?: "Unable to share ride log."
                                    }
                            }
                        ) {
                            Text("Share Ride Log")
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                GenreSearchBox(
                    searchText = searchText,
                    filteredGenres = searchSuggestions,
                    showGenreMenu = showGenreMenu,
                    onSearchTextChanged = { newText ->
                        searchText = newText
                        showGenreMenu = true
                    },
                    onDropdownClick = {
                        showGenreMenu =
                            !showGenreMenu
                    },
                    onSearchClick = submitSearch,
                    onDismissMenu = {
                        showGenreMenu = false
                    },
                    onGenreSelected = { suggestion ->
                        showGenreMenu = false
                        focusManager.clearFocus()
                        runSearch(
                            query = suggestion,
                            startPlayback = true,
                            preserveAnchor = false
                        )
                    }
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )
                SearchChips(
                    selectedSearch =
                        sessionState.categoryName,
                    onSearchSelected = { genre ->
                        runSearch(
                            query = genre,
                            startPlayback = true,
                            preserveAnchor = false
                        )
                    }
                )
                Spacer(
                    modifier = Modifier.height(13.dp)
                )

                CategoryCard(
                    categoryName =
                        sessionState.categoryDisplayName,
                    includedInNavigation =
                        currentPermanentCategory
                            ?.includedInNavigation
                            ?: activeSearchQuery
                                ?.let { query ->
                                    savedSearchFor(query)
                                        ?.navigationEnabled
                                }
                            ?: false,
                    onNavigationToggle = {
                        val category =
                            currentPermanentCategory

                        if (category != null) {
                            musicRepository.categories
                                .setNavigation(
                                    category.id,
                                    !category
                                        .includedInNavigation
                                )
                            stationStateVersion++
                        } else {
                            val query =
                                activeSearchQuery
                                    ?: return@CategoryCard

                            val saved =
                                savedSearchFor(query)
                                    ?: return@CategoryCard

                            replaceSavedSearches(
                                appPreferences
                                    .setSearchNavigation(
                                        query = query,
                                        enabled =
                                            !saved.navigationEnabled
                                    )
                            )
                            stationStateVersion++
                        }
                    },
                    onCategoryClick = {
                        val hasAnyCategories =
                            musicRepository.categories
                                .getAll()
                                .isNotEmpty() ||
                                    savedSearchCategories.isNotEmpty()

                        if (hasAnyCategories) {
                            showCategoryList = true
                        }
                    },
                    onListClick = {
                        val key =
                            if (sessionState.isSearch) {
                                "search:${sessionState.categoryName}"
                            } else {
                                "category:${sessionState.categoryId}"
                            }

                        openStationList(key)
                    },
                    onDeleteClick = {
                        deleteCategoryKey =
                            if (sessionState.isSearch) {
                                "search:${sessionState.categoryName}"
                            } else {
                                "category:${sessionState.categoryId}"
                            }
                    }
                )

                Spacer(
                    modifier = Modifier.height(13.dp)
                )

                if (displayedStation != null) {
                    NowPlayingCard(
                        stationName =
                            displayedStation.name,
                        stationGenre =
                            displayedStation.genre,
                        stationCallLetters =
                            displayedStation.callLetters,
                        stationCity =
                            displayedStation.city,
                        stationCountry =
                            displayedStation.country,
                        songTitle =
                            radioPlayer.nowPlayingTitle,
                        songArtist =
                            radioPlayer.nowPlayingArtist,
                        stationNumber =
                            displayedStationIndex + 1,
                        stationCount =
                            displayedStationCount,
                        categoryIsSearch =
                            sessionState.isSearch,
                        isPlaying = isPlaying,
                        includedInNavigation =
                            displayedStation
                                .includedInNavigation,
                        onNavigationToggle = {
                            displayedStation
                                .includedInNavigation =
                                !displayedStation
                                    .includedInNavigation

                            val refreshedState =
                                sessionController.getState()

                            publishSession(refreshedState)

                            saveCurrentState(
                                state = refreshedState,
                                wasPlaying =
                                    radioPlayer.isPlaying
                            )

                            stationStateVersion++
                        },
                        onSaveOrMoveClick = {
                            val sourceStation =
                                displayedStation

                            stationToSaveElsewhere =
                                sourceStation

                            destinationCategoryPickerTitle =
                                if (sessionState.isSearch) {
                                    "Save to category"
                                } else {
                                    "Move to category"
                                }

                            destinationCategorySearchText =
                                sessionState.categoryName
                                    .ifBlank {
                                        sourceStation.genre
                                    }
                                    .trim()
                        },
                        onCopyClick = {
                            destinationCategoryPickerTitle =
                                "Save to another category"
                            destinationCategorySearchText = ""
                            stationToSaveElsewhere =
                                displayedStation
                        },
                        onDeleteClick = {
                        }
                    )
                } else {
                    Text(
                        text =
                            when {
                                !startupRestoreComplete -> ""
                                !libraryHasCategories ->
                                    "No categories are available. Search to add a category and stations."
                                else ->
                                    "No stations are available."
                            },
                        color = Color.Black,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                PlaybackControls(
                    isPlaying = isPlaying,
                    onPreviousCategoryClick = {
                        changeCategory(direction = -1)
                    },
                    onPreviousStationClick = {
                        moveStation(direction = -1)
                    },
                    onPlayPauseClick = {
                        if (isPlaying) {
                            stopPlayback()
                        } else if (sessionState.hasStations) {
                            startPlayback()
                        } else {
                            navigationStatusMessage =
                                if (!libraryHasCategories) {
                                    "No categories are available. Search to add a category and stations."
                                } else {
                                    "No stations are available."
                                }
                        }
                    },
                    onNextStationClick = {
                        moveStation(direction = 1)
                    },
                    onNextCategoryClick = {
                        changeCategory(direction = 1)
                    }
                )

                val visibleStatusMessage =
                    radioPlayer.errorMessage
                        ?: navigationStatusMessage

                visibleStatusMessage?.let { message ->
                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = message,
                        color = Color.Black,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(
                    modifier = Modifier.weight(1f)
                )
            }
        }
        val pendingSaveStation =
            stationToSaveElsewhere

        if (pendingSaveStation != null) {
            CategoryPicker(
                title = destinationCategoryPickerTitle,
                searchText = destinationCategorySearchText,
                categories = repositoryCategories,
                suggestedCategoryNames = genres,
                stationCountForCategory = { category ->
                    membershipRepository
                        .getStationsForCategory(category.id)
                        .size
                },
                onSearchTextChanged = { newText ->
                    destinationCategorySearchText = newText
                },
                onCategorySelected = { categoryName, existingCategory ->
                    val destinationCategory =
                        existingCategory
                            ?: Category(
                                id =
                                    (musicRepository
                                        .categories
                                        .getAll()
                                        .maxOfOrNull { category ->
                                            category.id
                                        } ?: 0L) + 1L,
                                name = categoryName,
                                type = CategoryType.STANDARD,
                                includedInNavigation = true,
                                sortOrder =
                                    musicRepository
                                        .categories
                                        .getAll()
                                        .size
                            ).also { newCategory ->
                                musicRepository
                                    .categories
                                    .add(newCategory)
                            }

                    val existingStation =
                        musicRepository.stations
                            .getByStreamUrl(
                                pendingSaveStation.streamUrl
                            )

                    val stationForLibrary =
                        existingStation
                            ?: pendingSaveStation
                                .copy(
                                    id =
                                        (musicRepository
                                            .stations
                                            .getAll()
                                            .maxOfOrNull { station ->
                                                station.id
                                            } ?: 0L) + 1L,
                                    includedInNavigation = true,
                                    failedThisSession = false
                                )
                                .also { station ->
                                    musicRepository
                                        .stations
                                        .add(station)
                                }

                    val added =
                        membershipRepository
                            .addStationToCategory(
                                categoryId = destinationCategory.id,
                                stationId = stationForLibrary.id
                            )

                    appPreferences.savePermanentLibrary(
                        categoryRepository =
                            musicRepository.categories,
                        stationRepository = musicRepository.stations,
                        membershipRepository = membershipRepository
                    )

                    navigationStatusMessage =
                        if (added) {
                            "Saved ${stationForLibrary.name} to ${destinationCategory.name}."
                        } else {
                            "${stationForLibrary.name} is already in ${destinationCategory.name}."
                        }

                    stationToSaveElsewhere = null
                    destinationCategorySearchText = ""
                    destinationCategoryPickerTitle =
                        "Save to another category"
                    stationStateVersion++
                },
                onDismiss = {
                    stationToSaveElsewhere = null
                    destinationCategorySearchText = ""
                    destinationCategoryPickerTitle =
                        "Save to another category"
                }
            )
        }

        val pendingDeleteKey =
            deleteCategoryKey

        if (pendingDeleteKey != null) {
            val pendingName =
                categoryDisplayNameForKey(
                    pendingDeleteKey
                )

            AlertDialog(
                onDismissRequest = {
                    deleteCategoryKey = null
                },
                title = {
                    Text("Delete category?")
                },
                text = {
                    Text(
                        "Are you sure you want to delete the $pendingName category and its stations?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deleteCategory(
                                pendingDeleteKey
                            )
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            deleteCategoryKey = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

    }
}