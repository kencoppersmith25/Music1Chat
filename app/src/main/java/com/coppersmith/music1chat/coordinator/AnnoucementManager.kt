package com.coppersmith.music1chat.coordinator

// Music1Chat coordinated release
// Date: 2026-09-01
// Release: 2026-09-01 v03
//
// Updates category announcements to forward search state to CategoryAnnouncer.

import android.content.Context
import com.coppersmith.music1chat.persistence.AppPreferences
import com.coppersmith.music1chat.session.PlaybackSessionState
import com.coppersmith.music1chat.speech.CategoryAnnouncer
import com.coppersmith.music1chat.session.PlaybackSessionMode


class AnnouncementManager(
    context: Context
) {
    private val appContext = context.applicationContext
    private val preferences = AppPreferences(appContext)
    private val announcer = CategoryAnnouncer(appContext)

    var onSpeechStatusChanged: ((isSpeaking: Boolean) -> Unit)? = null
        set(value) {
            field = value
            announcer.onSpeechStatusChanged = value
        }

    private var suppressNextAnnouncement = false

    init {
        announcer.selectVoiceForSession(
            preferences.loadCategoryAnnouncementVoiceId()
        )
    }

    /**
     * Called by MainScreen after publishing a new playback-session state.
     *
     * Startup restoration and station-only changes remain silent. A real
     * category change is announced only when the preference is enabled.
     */
    fun onSessionChanged(
        previousState: PlaybackSessionState,
        newState: PlaybackSessionState,
        startupRestoreComplete: Boolean
    ) {
        if (!startupRestoreComplete) {
            return
        }

        if (!categoryChanged(previousState, newState)) {
            return
        }

        // A category is ONLY a search queue if its mode is explicitly SEARCH
        val isSearchQueue = newState.mode == PlaybackSessionMode.SEARCH

        announceCategory(
            categoryName = newState.categoryName,
            isSearchQueue = isSearchQueue
        )
    }

    fun announceCategory(
        categoryName: String,
        isSearchQueue: Boolean = false
    ) {
        if (suppressNextAnnouncement) {
            suppressNextAnnouncement = false
            return
        }

        if (!preferences.loadAnnounceCategoryChanges()) {
            return
        }

        // If this is NOT a search queue, aggressively strip any accidental "Search:" prefix
        // and any text following a colon or parenthesis to get the raw category name.
        val cleanCategoryName = if (!isSearchQueue) {
            categoryName.substringAfter("Search:").substringBefore("(").trim()
        } else {
            categoryName.removePrefix("Search:").trim()
        }

        if (cleanCategoryName.isBlank()) {
            return
        }

        announcer.selectVoiceForSession(
            preferences.loadCategoryAnnouncementVoiceId()
        )

        announcer.announceCategory(
            categoryName = cleanCategoryName,
            isSearchQueue = isSearchQueue
        )
    }

    fun speak(text: String) {
        announcer.selectVoiceForSession(
            preferences.loadCategoryAnnouncementVoiceId()
        )
        announcer.testVoice(text)
    }

    fun previewVoice(
        voiceId: String?
    ) {
        announcer.selectVoiceForSession(voiceId)
        announcer.testVoice("Classical")
    }

    fun selectVoice(
        voiceId: String?
    ) {
        preferences.saveCategoryAnnouncementVoiceId(voiceId)
        announcer.selectVoiceForSession(voiceId)
    }

    fun suppressStartupAnnouncement() {
        suppressNextAnnouncement = true
    }

    fun shutdown() {
        announcer.shutdown()
    }

    private fun categoryChanged(
        previousState: PlaybackSessionState,
        newState: PlaybackSessionState
    ): Boolean {
        if (previousState.isSearch != newState.isSearch) {
            return true
        }

        return if (newState.isSearch) {
            !previousState.categoryName.equals(
                newState.categoryName,
                ignoreCase = true
            )
        } else {
            previousState.categoryId != newState.categoryId
        }
    }
}