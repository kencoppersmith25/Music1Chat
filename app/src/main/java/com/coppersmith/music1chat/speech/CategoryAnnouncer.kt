package com.coppersmith.music1chat.speech

// Music1Chat revision: 2026-07-27 v01
//
// Adds support for:
// - loading the user's saved Android voice,
// - temporarily selecting a voice for testing,
// - falling back safely to the phone's default voice.

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.coppersmith.music1chat.persistence.AppPreferences
import java.util.Locale

/**
 * Owns the Android TextToSpeech engine used for category announcements.
 *
 * MainScreen decides when an announcement is appropriate. This class:
 * - initializes and releases TextToSpeech,
 * - loads the saved phone voice,
 * - supports temporary voice selection for Settings tests,
 * - speaks supplied category names,
 * - suppresses accidental duplicate announcements.
 */
class CategoryAnnouncer(
    context: Context
) : TextToSpeech.OnInitListener {

    private val appPreferences =
        AppPreferences(
            context.applicationContext
        )

    private var textToSpeech: TextToSpeech? = null

    private var initializationComplete = false
    private var initializationSucceeded = false

    private var pendingUtterance: PendingUtterance? = null

    private var lastAnnouncedText: String? = null
    private var lastAnnouncementTimeMillis: Long = 0L

    private var phoneDefaultVoice: Voice? = null

    private var requestedVoiceSelectionSet = false
    private var requestedVoiceId: String? = null

    private var lastAppliedSavedVoiceId: String? = null

    init {
        textToSpeech =
            TextToSpeech(
                context.applicationContext,
                this
            )
    }

    override fun onInit(
        status: Int
    ) {
        initializationComplete = true
        initializationSucceeded =
            status == TextToSpeech.SUCCESS

        android.util.Log.d(
            "KenVoice",
            "TTS init status=$status success=$initializationSucceeded"
        )

        android.util.Log.d(
            "KenVoice",
            "TTS init status = $status"
        )

        if (!initializationSucceeded) {
            pendingUtterance = null
            return
        }

        val engine =
            textToSpeech ?: return

        val languageResult =
            engine.setLanguage(
                Locale.getDefault()
            )

        if (
            languageResult ==
            TextToSpeech.LANG_MISSING_DATA ||
            languageResult ==
            TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            engine.setLanguage(Locale.US)
        }

        /*
         * Capture the engine's normal phone-default voice before applying
         * the Music1Chat preference. Choosing "Phone Default" can then
         * reliably restore this voice.
         */
        phoneDefaultVoice = engine.voice

        if (requestedVoiceSelectionSet) {
            applyVoiceSelection(
                engine = engine,
                voiceId = requestedVoiceId
            )
        } else {
            applySavedVoice(engine)
        }

        engine.setSpeechRate(0.92f)
        engine.setPitch(1.0f)

        pendingUtterance?.let { pending ->
            pendingUtterance = null

            speakInternal(
                text = pending.text,
                force = pending.force,
                refreshSavedVoice =
                    pending.refreshSavedVoice
            )
        }
    }

    /**
     * Speaks the supplied category name unless it is an accidental,
     * immediate duplicate of the previous announcement.
     */
    fun announceCategory(
        categoryName: String
    ) {
        speak(
            text = categoryName,
            force = false,
            refreshSavedVoice = true
        )
    }

    /**
     * Tests the voice currently configured in this announcer.
     */
    fun testVoice(
        sampleText: String = "Classical"
    ) {
        speak(
            text = sampleText,
            force = true,
            refreshSavedVoice = true
        )
    }

    /**
     * Temporarily selects a voice for this CategoryAnnouncer instance.
     *
     * Pass null to select the phone's default voice.
     *
     * Settings uses this before calling testVoice(). The selection is not
     * permanently stored until Settings calls AppPreferences.
     */
    fun selectVoiceForSession(
        voiceId: String?
    ) {
        requestedVoiceSelectionSet = true
        requestedVoiceId =
            voiceId
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

        val engine =
            textToSpeech ?: return

        if (
            initializationComplete &&
            initializationSucceeded
        ) {
            applyVoiceSelection(
                engine = engine,
                voiceId = requestedVoiceId
            )
        }
    }

    private fun speak(
        text: String,
        force: Boolean,
        refreshSavedVoice: Boolean
    ) {
        val cleanedText =
            text.trim()

        if (cleanedText.isBlank()) {
            return
        }

        if (!initializationComplete) {
            pendingUtterance =
                PendingUtterance(
                    text = cleanedText,
                    force = force,
                    refreshSavedVoice =
                        refreshSavedVoice
                )

            return
        }

        if (!initializationSucceeded) {
            return
        }

        speakInternal(
            text = cleanedText,
            force = force,
            refreshSavedVoice =
                refreshSavedVoice
        )
    }

    private fun speakInternal(
        text: String,
        force: Boolean,
        refreshSavedVoice: Boolean
    ) {
        val engine =
            textToSpeech ?: return

        /*
         * MainScreen may keep this CategoryAnnouncer alive while Settings
         * changes the preference. Rechecking before a real announcement
         * lets the new saved voice take effect without restarting the app.
         */
        if (refreshSavedVoice) {
            requestedVoiceSelectionSet = false
            applySavedVoice(engine)
        }

        val now =
            System.currentTimeMillis()

        val isImmediateDuplicate =
            !force &&
                    text.equals(
                        lastAnnouncedText,
                        ignoreCase = true
                    ) &&
                    now - lastAnnouncementTimeMillis <
                    DUPLICATE_WINDOW_MILLIS

        if (isImmediateDuplicate) {
            return
        }

        lastAnnouncedText = text
        lastAnnouncementTimeMillis = now

        val result =
            engine.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "music1chat-category-$now"
            )

        android.util.Log.d(
            "KenVoice",
            "speak() returned $result for '$text'"
        )
    }

    private fun applySavedVoice(
        engine: TextToSpeech
    ) {
        val savedVoiceId =
            appPreferences
                .loadCategoryAnnouncementVoiceId()

        if (
            savedVoiceId ==
            lastAppliedSavedVoiceId
        ) {
            return
        }

        applyVoiceSelection(
            engine = engine,
            voiceId = savedVoiceId
        )

        lastAppliedSavedVoiceId =
            savedVoiceId
    }

    private fun applyVoiceSelection(
        engine: TextToSpeech,
        voiceId: String?
    ) {
        if (voiceId.isNullOrBlank()) {
            phoneDefaultVoice?.let { defaultVoice ->
                engine.voice = defaultVoice
            }

            return
        }

        val matchingVoice =
            engine.voices
                .orEmpty()
                .firstOrNull { voice ->
                    voice.name == voiceId
                }

        if (matchingVoice != null) {
            engine.voice = matchingVoice
        } else {
            phoneDefaultVoice?.let { defaultVoice ->
                engine.voice = defaultVoice
            }
        }
    }

    /**
     * The owner must call this when it permanently leaves composition.
     */
    fun shutdown() {
        pendingUtterance = null

        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        initializationComplete = true
        initializationSucceeded = false
    }

    private data class PendingUtterance(
        val text: String,
        val force: Boolean,
        val refreshSavedVoice: Boolean
    )

    private companion object {
        const val DUPLICATE_WINDOW_MILLIS =
            1_500L
    }
}