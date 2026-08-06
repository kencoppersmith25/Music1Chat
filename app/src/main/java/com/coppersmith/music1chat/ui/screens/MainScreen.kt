package com.coppersmith.music1chat.ui.screens

// Music1Chat revision: 2026-07-30 v07 — Extract playback-session coordination
// Change: selecting a station immediately dismisses the station-list dialog.
// Replace the existing MainScreen.kt with this file, then rename it MainScreen.kt.
// Change: the existing Cast icon now opens the standard Google Cast device picker.
// This checkpoint does not yet transfer playback to the selected device.
// Power stops playback immediately.
// Changes:
// - Deleting the active category selects the next category.name = "Search: ${search.query}",
// - Playback continues when the deleted category was playing.name = "Search: ${search.query}",
// - Saved-search categories count when choosing the replacement category.
// - The category card does nothing when the library is truly empty.
// - Deleting the final category exits the category list and clears the session.
// - Empty-library guidance directs the user to Search.
// - Adds Start/Stop/Share Ride Log controls and field diagnostics.


// Music1Chat coordinated release
// Release: 2026-07-18 v01
// DROP-IN REPLACEMENT
// Change: passes RadioPlayer live title/artist metadata to the now-playing card.
// Matched file: RadioPlayer.kt 2026-07-18 v01

// PLAYBACK SESSION INTEGRATION V2
// Search results participate as a temporary category in category navigation.

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import com.coppersmith.music1chat.GenreData
import com.coppersmith.music1chat.RadioPlayer
import com.coppersmith.music1chat.coordinator.AnnouncementManager
import com.coppersmith.music1chat.coordinator.CategoryCatalog
import com.coppersmith.music1chat.coordinator.CategoryNavigationRequest
import com.coppersmith.music1chat.coordinator.CategoryOperations
import com.coppersmith.music1chat.coordinator.Content
import com.coppersmith.music1chat.coordinator.Dialogs
import com.coppersmith.music1chat.coordinator.Library
import com.coppersmith.music1chat.coordinator.MainScreenNavigationCoordinator
import com.coppersmith.music1chat.coordinator.Playback
import com.coppersmith.music1chat.coordinator.Search
import com.coppersmith.music1chat.coordinator.SearchWorkflowOutcome
import com.coppersmith.music1chat.coordinator.Session
import com.coppersmith.music1chat.coordinator.StationList
import com.coppersmith.music1chat.coordinator.StationOperations
import com.coppersmith.music1chat.diagnostics.RideLogger
import com.coppersmith.music1chat.models.Category
import com.coppersmith.music1chat.models.CategoryType
import com.coppersmith.music1chat.persistence.SavedSearchCategory
import com.coppersmith.music1chat.playback.MediaButtonCommand
import com.coppersmith.music1chat.playback.MediaButtonCommandBus
import com.coppersmith.music1chat.session.PlaybackSessionController
import com.coppersmith.music1chat.session.PlaybackSessionMode
import com.coppersmith.music1chat.session.PlaybackSessionState
import com.coppersmith.music1chat.ui.components.CategoryPicker
import kotlinx.coroutines.launch
import com.coppersmith.music1chat.BuildConfig


@Composable
fun MainScreen() {

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val searchCoordinator = remember {
        Search()
    }

    val navigationCoordinator = remember {
        MainScreenNavigationCoordinator()
    }

    val libraryStartup = remember {
        Library(context.applicationContext).load()
    }

    val appPreferences =
        libraryStartup.preferences

    val musicRepository =
        libraryStartup.musicRepository

    val categoryCatalog = remember {
        CategoryCatalog(
            musicRepository = musicRepository,
            normalizedSearchKey = searchCoordinator::normalizedKey
        )
    }

    val repositoryCategories =
        libraryStartup.repositoryCategories

    val repositoryStations =
        libraryStartup.repositoryStations

    val membershipRepository =
        musicRepository.memberships

    val savedPlaybackState =
        libraryStartup.savedPlaybackState

    val initialSavedSearchCategories =
        libraryStartup.savedSearchCategories

    val initiallyCurrentSearch =
        libraryStartup.initiallyCurrentSearch

    val initialCategory =
        libraryStartup.initialCategory

    val initialStation =
        libraryStartup.initialStation

    val initialStations =
        libraryStartup.initialStations

    val shouldResumePlayback =
        libraryStartup.shouldResumePlayback

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

    val stationOperations = remember {
        StationOperations(
            preferences = appPreferences,
            musicRepository = musicRepository,
            sessionController = sessionController
        )
    }

    val session = remember {
        Session(
            preferences = appPreferences,
            musicRepository = musicRepository,
            sessionController = sessionController,
            navigationCoordinator = navigationCoordinator
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

    val playback = remember {
        Playback(
            radioPlayer = radioPlayer,
            sessionController = sessionController
        )
    }

    var navigationStatusMessage by remember {
        mutableStateOf<String?>(null)
    }

    var searchFeedbackMessage by remember {
        mutableStateOf<String?>(null)
    }

    var startupRestoreComplete by remember {
        mutableStateOf(false)
    }

    val announcementManager = remember {
        AnnouncementManager(context.applicationContext)
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

    var showSettings by remember {
        mutableStateOf(false)
    }

    var searchResultLimit by remember {
        mutableIntStateOf(
            appPreferences.loadSearchResultLimit()
        )
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

    var stationPendingDelete by remember {
        mutableStateOf<com.coppersmith.music1chat.models.Station?>(null)
    }

    var stationPendingDeleteCategoryId by remember {
        mutableStateOf<Long?>(null)
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

// Searches currently being prepared invisibly for category navigation.
    val searchPrefetchesInProgress = remember {
        mutableSetOf<String>()
    }

    var savedSearchCategories by remember {
        mutableStateOf(initialSavedSearchCategories)
    }

    val libraryHasCategories =
        musicRepository.categories.getAll().isNotEmpty() ||
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

    val playbackRequested =
        radioPlayer.playbackRequested

    val currentPermanentCategory =
        sessionState.categoryId?.let { categoryId ->
            musicRepository.categories.getById(categoryId)
        }

    val effectiveSearchQuery =
        sessionState.categoryName
            .trim()
            .ifBlank { activeSearchQuery.orEmpty().trim() }

    val effectiveCategoryStationCount =
        if (sessionState.isSearch) {
            displayedStationCount
        } else {
            currentPermanentCategory?.let { category ->
                membershipRepository
                    .getStationsForCategory(category.id)
                    .size
            } ?: displayedStationCount
        }

    val effectiveCategoryDisplayName =
        if (sessionState.isSearch) {
            if (effectiveSearchQuery.isBlank()) {
                "Search ($effectiveCategoryStationCount)"
            } else {
                "Search: $effectiveSearchQuery ($effectiveCategoryStationCount)"
            }
        } else {
            "${sessionState.categoryName} ($effectiveCategoryStationCount)"
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

    if (BuildConfig.DEBUG) {
        LaunchedEffect(Unit) {
            RideLogger.startAutomatically(
                context.applicationContext
            )

            RideLogger.log(
                "INITIAL_STATE " +
                        "playing=${radioPlayer.isPlaying} " +
                        "category='${sessionStateRef.value.categoryDisplayName}' " +
                        "station='${sessionStateRef.value.currentStation?.name.orEmpty()}'"
            )
        }
    }

    fun normalizedSearchKey(query: String): String =
        searchCoordinator.normalizedKey(query)

    fun savedSearchFor(query: String): SavedSearchCategory? =
        searchCoordinator.savedSearchFor(
            savedSearches = savedSearchCategories,
            query = query
        )

    fun replaceSavedSearches(
        updated: List<SavedSearchCategory>
    ) {
        /*
         * Prevent malformed persisted records from re-entering
         * Compose state after an AppPreferences operation.
         */
        updated
            .filter { savedSearch ->
                savedSearch.query.isBlank()
            }
            .forEach { malformedSearch ->
                appPreferences.removeSearchCategory(
                    malformedSearch.query
                )
            }

        savedSearchCategories =
            updated
                .filter { savedSearch ->
                    savedSearch.query.isNotBlank()
                }
                .map { savedSearch ->
                    savedSearch.copy(
                        query = savedSearch.query.trim()
                    )
                }
                .distinctBy { savedSearch ->
                    savedSearch.query.lowercase()
                }
    }


    fun publishSession(
        newState: PlaybackSessionState
    ) {
        val previousState = sessionStateRef.value

        sessionState = newState
        sessionStateRef.value = newState

        announcementManager.onSessionChanged(
            previousState = previousState,
            newState = newState,
            startupRestoreComplete = startupRestoreComplete
        )

        if (newState.isSearch && newState.hasEligibleStations) {
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
        val result = session.save(
            state = state,
            wasPlaying = wasPlaying,
            savedSearches = savedSearchCategories,
            activeSearchQuery = activeSearchQuery,
            searchAnchorCategoryId = searchAnchorCategoryId
        )

        replaceSavedSearches(result.savedSearches)
        activeSearchQuery = result.activeSearchQuery
    }

    fun playCurrentSessionStation(
        state: PlaybackSessionState
    ) {
        playback.playCurrentStation(state)
    }

    lateinit var runSearchAction:
                (String, Boolean, Boolean, (() -> Unit)?) -> Unit

    fun restoreSearch(
        query: String,
        startPlayback: Boolean = true,
        onComplete: (() -> Unit)? = null
    ) {
        val restoredState = session.restoreCachedSearch(
            query = query,
            cachedSessions = searchSessionStates,
            startPlayback = startPlayback
        )

        if (restoredState == null) {
            runSearchAction(
                query,
                startPlayback,
                true,
                onComplete
            )
            return
        }

        publishSession(restoredState)
        navigationStatusMessage = null

        saveCurrentState(
            state = restoredState,
            wasPlaying = startPlayback
        )

        if (startPlayback) {
            playCurrentSessionStation(restoredState)
        }

        onComplete?.invoke()
    }

    fun selectCategory(
        category: Category,
        preferredStationId: Long? = null,
        startPlayback: Boolean = true
    ) {
        val newState = session.selectCategory(
            category = category,
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
            "CATEGORY_COMMAND direction=$direction " +
                    "beforeCategory='${beforeState.categoryDisplayName}' " +
                    "beforeStation='${beforeState.currentStation?.name.orEmpty()}'"
        )

        val navigationRequest = session.requestCategoryNavigation(
            direction = direction,
            currentState = beforeState,
            savedSearches = savedSearchCategories,
            cachedSessions = searchSessionStates
        )

        when (navigationRequest) {
            CategoryNavigationRequest.Empty -> {
                radioPlayer.stop()
                publishSession(
                    sessionController.stop()
                )

                navigationStatusMessage =
                    "No playable categories are available."
            }

            is CategoryNavigationRequest.Queued -> {
                RideLogger.log(
                    "CATEGORY_COMMAND_QUEUED reason='activation pending' " +
                            "direction=${navigationRequest.direction} " +
                            "target='${navigationRequest.pendingKey}'"
                )
            }

            is CategoryNavigationRequest.Activate -> {
                val selectedKey = navigationRequest.selectedKey
                val selectedSearchQuery =
                    navigationRequest.searchQuery

                RideLogger.log(
                    "CATEGORY_TARGET currentKey='${navigationRequest.currentKey}' " +
                            "selectedKey='$selectedKey' " +
                            "cached=${navigationRequest.searchCached}"
                )

                fun finishNavigationAttempt() {
                    val afterState = sessionStateRef.value
                    val completion =
                        session.finishCategoryNavigation(
                            requestedKey = selectedKey,
                            activatedState = afterState
                        )

                    RideLogger.log(
                        "CATEGORY_RESULT success=${completion.succeeded} " +
                                "requestedKey='$selectedKey' " +
                                "activatedKey='${completion.activatedKey}' " +
                                "afterCategory='${afterState.categoryDisplayName}' " +
                                "afterStation='${afterState.currentStation?.name.orEmpty()}'"
                    )

                    if (!completion.succeeded) {
                        navigationStatusMessage =
                            "Unable to load the next category."
                    }

                    completion.queuedDirection?.let { queuedDirection ->
                        RideLogger.log(
                            "CATEGORY_COMMAND_DEQUEUED direction=$queuedDirection"
                        )

                        changeCategory(
                            direction = queuedDirection
                        )
                    }
                }

                if (selectedSearchQuery != null) {
                    restoreSearch(
                        query = selectedSearchQuery,
                        startPlayback = true,
                        onComplete = {
                            finishNavigationAttempt()
                        }
                    )
                    return
                }

                val categoryId = navigationRequest.categoryId
                val category =
                    session.categoryById(categoryId)

                if (category != null) {
                    selectCategory(
                        category = category,
                        startPlayback = true
                    )
                }

                finishNavigationAttempt()
            }
        }
    }

    fun moveStation(
        direction: Int
    ) {
        val result = playback.moveStation(direction)

        if (!result.changed) {
            return
        }

        publishSession(result.state)
        navigationStatusMessage = null

        saveCurrentState(
            state = result.state,
            wasPlaying = true
        )

        playCurrentSessionStation(result.state)
    }

    fun stopPlayback() {
        val newState = playback.stop()
        publishSession(newState)

        saveCurrentState(
            state = newState,
            wasPlaying = false
        )
    }

    fun startPlayback() {
        val result = playback.start()
        publishSession(result.state)

        if (!result.canPlay) {
            navigationStatusMessage =
                if (musicRepository.categories.getAll().isEmpty()) {
                    "No categories are available. Search to add a category and stations."
                } else {
                    "No stations are available."
                }
            return
        }

        saveCurrentState(
            state = result.state,
            wasPlaying = true
        )

        playCurrentSessionStation(result.state)
    }

    fun runSearch(
        query: String,
        startPlayback: Boolean,
        preserveAnchor: Boolean,
        onComplete: (() -> Unit)? = null
    ) {
        val searchQuery = query.trim()

        if (searchQuery.isBlank()) {
            return
        }

        searchAnchorCategoryId =
            searchCoordinator.resolveAnchorCategoryId(
                preserveAnchor = preserveAnchor,
                stateBeforeSearch = sessionStateRef.value,
                existingSavedSearch = savedSearchFor(searchQuery),
                currentAnchorCategoryId = searchAnchorCategoryId
            )

        showGenreMenu = false
        focusManager.clearFocus()
        searchText = ""
        latestSearchRequest++

        val thisSearchRequest = latestSearchRequest
        searchFeedbackMessage = "Searching for “$searchQuery”…"

        coroutineScope.launch {
            try {
                val outcome =
                    searchCoordinator.executeWorkflow(
                        query = searchQuery,
                        limit = appPreferences.getSearchResultLimit(),
                        repositoryStations = repositoryStations,
                        savedSearches = savedSearchCategories,
                        cachedSessions = searchSessionStates,
                        anchorCategoryId = searchAnchorCategoryId,
                        startPlayback = startPlayback,
                        sessionController = sessionController
                    )

                if (thisSearchRequest != latestSearchRequest) {
                    return@launch
                }

                when (outcome) {
                    is SearchWorkflowOutcome.Success -> {
                        Log.d(
                            "KenCheck",
                            "Search submitted='$searchQuery', " +
                                    "local=${outcome.localCount}, " +
                                    "live=${outcome.liveCount}, " +
                                    "results=${outcome.state.stationCount}"
                        )

                        radioPlayer.stop()
                        publishSession(outcome.state)
                        replaceSavedSearches(
                            appPreferences.upsertSearchCategory(
                                outcome.savedSearch
                            )
                        )
                        activeSearchQuery = searchQuery
                        appPreferences.saveWasPlaying(startPlayback)
                        searchFeedbackMessage = null
                        navigationStatusMessage = null

                        if (startPlayback) {
                            playCurrentSessionStation(outcome.state)
                        }
                    }

                    is SearchWorkflowOutcome.CachedFallback -> {
                        publishSession(outcome.state)
                        activeSearchQuery = searchQuery
                        searchFeedbackMessage =
                            "Search refresh failed. Showing the previous results."

                        if (startPlayback) {
                            playCurrentSessionStation(outcome.state)
                        }
                    }

                    SearchWorkflowOutcome.Empty -> {
                        searchFeedbackMessage =
                            "No stations found for “$searchQuery”."
                    }
                }
            } finally {
                onComplete?.invoke()
            }
        }
    }

    fun prefetchSearch(
        query: String
    ) {
        val searchQuery = query.trim()

        if (searchQuery.isBlank()) {
            return
        }

        val normalizedKey = normalizedSearchKey(searchQuery)

        if (
            searchSessionStates[normalizedKey]?.hasStations == true ||
            normalizedKey in searchPrefetchesInProgress
        ) {
            return
        }

        searchPrefetchesInProgress.add(normalizedKey)
        RideLogger.log("SEARCH_PREFETCH_START query='$searchQuery'")

        coroutineScope.launch {
            try {
                val prefetchedState =
                    searchCoordinator.prefetchWorkflow(
                        query = searchQuery,
                        limit = appPreferences.getSearchResultLimit(),
                        repositoryStations = repositoryStations,
                        savedSearches = savedSearchCategories
                    )

                if (prefetchedState == null) {
                    RideLogger.log("SEARCH_PREFETCH_EMPTY query='$searchQuery'")
                    return@launch
                }

                searchSessionStates =
                    searchSessionStates +
                            (normalizedKey to prefetchedState)

                RideLogger.log(
                    "SEARCH_PREFETCH_READY " +
                            "query='$searchQuery' " +
                            "stations=${prefetchedState.stationCount}"
                )
            } finally {
                searchPrefetchesInProgress.remove(normalizedKey)
            }
        }
    }

    runSearchAction = { query,
                        startPlayback,
                        preserveAnchor,
                        onComplete ->

        runSearch(
            query = query,
            startPlayback = startPlayback,
            preserveAnchor = preserveAnchor,
            onComplete = onComplete
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
        categoryCatalog.stationsForKey(
            key = key,
            searchSessionStates = searchSessionStates
        )

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
                    true,
                    null
                )
            }
        }
    }

    fun categoryDisplayNameForKey(
        key: String
    ): String =
        categoryCatalog.displayNameForKey(
            key = key,
            searchSessionStates = searchSessionStates
        )

    fun deleteCategory(
        key: String
    ) {
        val wasPlayingBeforeDelete = radioPlayer.isPlaying

        fun orderedCategoryKeys(): List<String> =
            navigationCoordinator.navigationKeys(
                permanentCategories =
                    musicRepository.categories.getNavigationCategories(),
                savedSearches = savedSearchCategories,
                categoryHasStations = { categoryId ->
                    membershipRepository
                        .getNavigationStationsForCategory(categoryId)
                        .isNotEmpty()
                }
            )

        val deletionPlan =
            CategoryOperations.plan(
                deletingKey = key,
                currentState = sessionStateRef.value,
                orderedKeys = orderedCategoryKeys()
            )

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
            val fallbackPermanentCategory =
                musicRepository.categories
                    .getAll()
                    .firstOrNull { category ->
                        membershipRepository
                            .getStationsForCategory(category.id)
                            .isNotEmpty()
                    }

            if (fallbackPermanentCategory != null) {
                radioPlayer.stop()

                selectCategory(
                    category = fallbackPermanentCategory,
                    startPlayback = false
                )

                showCategoryList = false
                stationListCategoryKey = null
                navigationStatusMessage =
                    "No navigation-enabled categories remain. " +
                            "Showing ${fallbackPermanentCategory.name}."

                return
            }

            val fallbackSearch =
                savedSearchCategories
                    .firstOrNull { savedSearch ->
                        val cachedState =
                            searchSessionStates[
                                normalizedSearchKey(savedSearch.query)
                            ]

                        cachedState?.hasStations == true
                    }

            if (fallbackSearch != null) {
                radioPlayer.stop()

                restoreSearch(
                    query = fallbackSearch.query,
                    startPlayback = false,
                    onComplete = {
                        showCategoryList = false
                        stationListCategoryKey = null
                        navigationStatusMessage =
                            "No navigation-enabled categories remain. " +
                                    "Showing ${fallbackSearch.query}."
                    }
                )

                return
            }

            radioPlayer.stop()

            val emptyState = sessionController.clear()
            publishSession(emptyState)

            showCategoryList = false
            stationListCategoryKey = null
            navigationStatusMessage =
                "No categories with stations are available."

            appPreferences.savePlaybackState(
                categoryId = null,
                stationId = null,
                wasPlaying = false
            )

            return
        }

        if (!deletionPlan.deletingCurrent) {
            return
        }

        radioPlayer.stop()

        val replacementKey =
            CategoryOperations.replacementKey(
                plan = deletionPlan,
                remainingKeys = remainingKeys
            ) ?: return

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

    fun deleteStationFromPermanentCategory(
        station: com.coppersmith.music1chat.models.Station,
        categoryId: Long
    ) {
        val result =
            stationOperations.deleteStation(
                station = station,
                categoryId = categoryId,
                currentState = sessionStateRef.value,
                wasPlaying = radioPlayer.isPlaying
            )

        result.refreshedState?.let { refreshedState ->
            publishSession(refreshedState)
            saveCurrentState(
                state = refreshedState,
                wasPlaying = result.shouldContinuePlaying
            )

            when {
                result.shouldStopPlayback -> {
                    radioPlayer.stop()
                    navigationStatusMessage = result.statusMessage
                }

                result.shouldStartPlayback -> {
                    playCurrentSessionStation(refreshedState)
                }
            }
        }

        stationPendingDelete = null
        stationPendingDeleteCategoryId = null
        stationStateVersion++
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
                    if (radioPlayer.playbackRequested) {
                        stopPlayback()
                    } else {
                        startPlayback()
                    }
                }

                MediaButtonCommand.NEXT_STATION -> {
                    moveStation(direction = 1)
                }

                MediaButtonCommand.PREVIOUS_STATION -> {
                    moveStation(direction = -1)
                }

                MediaButtonCommand.NEXT_CATEGORY -> {
                    changeCategory(direction = 1)
                }

                MediaButtonCommand.PREVIOUS_CATEGORY -> {
                    changeCategory(direction = -1)
                }
            }
        }
    }

    DisposableEffect(
        radioPlayer,
        sessionController,
        announcementManager
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
                    playback.markCurrentStationFailedAndAdvance()

                publishSession(newState)

                if (newState.hasEligibleStations) {
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
                wasPlaying = radioPlayer.playbackRequested
            )

            radioPlayer.onStationFailed = null
            radioPlayer.release()
            announcementManager.shutdown()
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

            StationList(
                stationListKey = stationListKey,
                categoryName =
                    categoryDisplayNameForKey(
                        stationListKey
                    ),
                stations = stationListStations,
                currentCategoryId = sessionState.categoryId,
                currentStationId = displayedStation?.id,
                currentSessionIsSearch = sessionState.isSearch,
                effectiveSearchQuery = effectiveSearchQuery,
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

                    stationListCategoryKey = null
                },
                onNavigationToggle = { station ->
                    val refreshedState =
                        stationOperations.toggleNavigation(station)

                    refreshedState?.let { state ->
                        publishSession(state)
                        saveCurrentState(
                            state = state,
                            wasPlaying = radioPlayer.isPlaying
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
                        val refreshedState =
                            stationOperations.moveStation(
                                categoryId = categoryId,
                                station = station,
                                newPosition = newPosition,
                                currentState = sessionStateRef.value,
                                wasPlaying = radioPlayer.isPlaying
                            )

                        refreshedState?.let { state ->
                            publishSession(state)
                            saveCurrentState(
                                state = state,
                                wasPlaying = radioPlayer.isPlaying
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
                        deleteStationFromPermanentCategory(
                            station = station,
                            categoryId = categoryId
                        )
                    }
                }
            )
        } else if (showCategoryList) {
            val categoryRows =
                categoryCatalog.categoryRows(
                    permanentCategories =
                        musicRepository.categories.getAll(),
                    savedSearches = savedSearchCategories,
                    searchSessionStates = searchSessionStates
                )

            CategoryListScreen(
                categories = categoryRows,
                selectedCategoryKey =
                    if (sessionState.isSearch) {
                        "search:$effectiveSearchQuery"
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
            Content(
                showSettings = showSettings,
                searchResultLimit = searchResultLimit,
                rideLogActive = rideLogActive,
                rideLogAvailable = rideLogAvailable,
                searchText = searchText,
                searchSuggestions = searchSuggestions,
                showGenreMenu = showGenreMenu,
                effectiveSearchQuery = effectiveSearchQuery,
                searchFeedbackMessage = searchFeedbackMessage,
                effectiveCategoryDisplayName = effectiveCategoryDisplayName,
                categoryIncludedInNavigation =
                    currentPermanentCategory?.includedInNavigation
                        ?: activeSearchQuery
                            ?.let { query ->
                                savedSearchFor(query)?.navigationEnabled
                            }
                        ?: false,
                displayedStation = displayedStation,
                songTitle = radioPlayer.nowPlayingTitle,
                songArtist = radioPlayer.nowPlayingArtist,
                artworkUri = radioPlayer.nowPlayingArtworkUri,
                displayedStationIndex = displayedStationIndex,
                displayedStationCount = displayedStationCount,
                categoryIsSearch = sessionState.isSearch,
                isPlaying = isPlaying,
                playbackRequested = playbackRequested,
                startupRestoreComplete = startupRestoreComplete,
                libraryHasCategories = libraryHasCategories,
                sessionHasStations = sessionState.hasStations,
                visibleStatusMessage =
                    radioPlayer.errorMessage ?: navigationStatusMessage,
                onSettingsClick = {
                    showSettings = true
                },
                onPowerClick = {
                    radioPlayer.stop()
                    appPreferences.saveWasPlaying(false)
                    navigationStatusMessage = "Playback stopped."
                },
                onSearchResultLimitChanged = { newLimit ->
                    searchResultLimit = newLimit
                    appPreferences.saveSearchResultLimit(newLimit)
                },
                onSettingsDismiss = {
                    showSettings = false
                },
                onRideLogToggle = {
                },
                onShareRideLog = {
                    RideLogger.share(context)
                        .onFailure { error ->
                            navigationStatusMessage =
                                error.message ?: "Unable to share ride log."
                        }
                },
                onSearchTextChanged = { newText ->
                    searchText = newText
                    showGenreMenu = true
                },
                onSearchDropdownClick = {
                    showGenreMenu = !showGenreMenu
                },
                onSearchClick = submitSearch,
                onSearchMenuDismiss = {
                    showGenreMenu = false
                },
                onSearchSuggestionSelected = { suggestion ->
                    showGenreMenu = false
                    focusManager.clearFocus()
                    runSearch(
                        query = suggestion,
                        startPlayback = true,
                        preserveAnchor = false
                    )
                },
                onSearchChipSelected = { genre ->
                    runSearch(
                        query = genre,
                        startPlayback = true,
                        preserveAnchor = false
                    )
                },
                onCategoryNavigationToggle = {
                    val category = currentPermanentCategory

                    if (category != null) {
                        musicRepository.categories.setNavigation(
                            category.id,
                            !category.includedInNavigation
                        )
                        stationStateVersion++
                    } else {
                        val query = activeSearchQuery
                            ?: return@Content
                        val saved = savedSearchFor(query)
                            ?: return@Content

                        replaceSavedSearches(
                            appPreferences.setSearchNavigation(
                                query = query,
                                enabled = !saved.navigationEnabled
                            )
                        )
                        stationStateVersion++
                    }
                },
                onCategoryClick = {
                    val hasAnyCategories =
                        musicRepository.categories.getAll().isNotEmpty() ||
                                savedSearchCategories.isNotEmpty()

                    if (hasAnyCategories) {
                        showCategoryList = true
                    }
                },
                onCategoryListClick = {
                    val key =
                        if (sessionState.isSearch) {
                            "search:$effectiveSearchQuery"
                        } else {
                            "category:${sessionState.categoryId}"
                        }
                    openStationList(key)
                },
                onCategoryDeleteClick = {
                    deleteCategoryKey =
                        if (sessionState.isSearch) {
                            "search:$effectiveSearchQuery"
                        } else {
                            "category:${sessionState.categoryId}"
                        }
                },
                onStationNavigationToggle = {
                    val station = displayedStation
                        ?: return@Content
                    station.includedInNavigation =
                        !station.includedInNavigation

                    val refreshedState = sessionController.getState()
                    publishSession(refreshedState)
                    saveCurrentState(
                        state = refreshedState,
                        wasPlaying = radioPlayer.isPlaying
                    )
                    stationStateVersion++
                },
                onStationSaveOrMoveClick = {
                    val sourceStation = displayedStation
                        ?: return@Content
                    stationToSaveElsewhere = sourceStation
                    destinationCategoryPickerTitle =
                        if (sessionState.isSearch) {
                            "Save to category"
                        } else {
                            "Move to category"
                        }
                    destinationCategorySearchText =
                        sessionState.categoryName
                            .ifBlank { sourceStation.genre }
                            .trim()
                },
                onStationCopyClick = {
                    val station = displayedStation
                        ?: return@Content
                    destinationCategoryPickerTitle =
                        "Save to another category"
                    destinationCategorySearchText = ""
                    stationToSaveElsewhere = station
                },
                onStationDeleteClick = {
                    val station = displayedStation
                        ?: return@Content
                    val categoryId = currentPermanentCategory?.id

                    if (categoryId != null && !sessionState.isSearch) {
                        stationPendingDelete = station
                        stationPendingDeleteCategoryId = categoryId
                    }
                },
                onPreviousCategoryClick = {
                    changeCategory(direction = -1)
                },
                onPreviousStationClick = {
                    moveStation(direction = -1)
                },
                onPlayPauseClick = {
                    if (playbackRequested) {
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
        }

        val pendingSaveStation =
            stationToSaveElsewhere

        if (pendingSaveStation != null) {
            CategoryPicker(
                title = destinationCategoryPickerTitle,
                searchText = destinationCategorySearchText,
                categories = musicRepository.categories.getAll(),
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

        Dialogs(
            pendingStation = stationPendingDelete,
            pendingStationCategoryId = stationPendingDeleteCategoryId,
            pendingStationCategoryName =
                stationPendingDeleteCategoryId
                    ?.let { categoryId ->
                        musicRepository.categories
                            .getById(categoryId)
                            ?.name
                    }
                    .orEmpty(),
            pendingCategoryKey = deleteCategoryKey,
            pendingCategoryDisplayName =
                deleteCategoryKey
                    ?.let(::categoryDisplayNameForKey)
                    .orEmpty(),
            onDismissStationDelete = {
                stationPendingDelete = null
                stationPendingDeleteCategoryId = null
            },
            onConfirmStationDelete = { station, categoryId ->
                deleteStationFromPermanentCategory(
                    station = station,
                    categoryId = categoryId
                )
            },
            onDismissCategoryDelete = {
                deleteCategoryKey = null
            },
            onConfirmCategoryDelete = { categoryKey ->
                deleteCategory(categoryKey)
            }
        )
    }
}