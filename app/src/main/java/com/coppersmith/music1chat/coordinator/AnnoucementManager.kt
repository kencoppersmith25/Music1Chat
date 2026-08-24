package com.coppersmith.music1chat.coordinator

// Music1Chat coordinated release
// Date: 2026-07-30
// Release: 2026-07-30 v02
//
// Merges the category-change decision logic into the existing
// AnnouncementManager. No separate Announcement.kt is required.

import android.content.Context
import com.coppersmith.music1chat.persistence.AppPreferences
import com.coppersmith.music1chat.session.PlaybackSessionState
import com.coppersmith.music1chat.speech.CategoryAnnouncer

class AnnouncementManager(
    context: Context
) {

    private val appContext = context.applicationContext

    private val preferences =
        AppPreferences(appContext)

    private val announcer =
        CategoryAnnouncer(appContext)

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

        announceCategory(newState.categoryName)
    }

    fun announceCategory(
        categoryName: String
    ) {
        if (suppressNextAnnouncement) {
            suppressNextAnnouncement = false
            return
        }

        if (!preferences.loadAnnounceCategoryChanges()) {
            return
        }

        val cleanCategoryName =
            categoryName
                .removePrefix("Search:")
                .trim()

        if (cleanCategoryName.isBlank()) {
            return
        }

        speak(cleanCategoryName)
    }

    fun speak(text: String) {
        // Reload the saved voice in case it was changed in Settings while
        // this long-lived manager remained active.
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