package com.coppersmith.music1chat.coordinator

import android.content.Context
import com.coppersmith.music1chat.persistence.AppPreferences
import com.coppersmith.music1chat.speech.CategoryAnnouncer

class AnnouncementManager(
    context: Context
) {

    private val appContext = context.applicationContext

    private val preferences =
        AppPreferences(appContext)

    private val announcer =
        CategoryAnnouncer(appContext)

    private var suppressNextAnnouncement = false

    init {
        announcer.selectVoiceForSession(
            preferences.loadCategoryAnnouncementVoiceId()
        )
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

        announcer.testVoice(categoryName)
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
}