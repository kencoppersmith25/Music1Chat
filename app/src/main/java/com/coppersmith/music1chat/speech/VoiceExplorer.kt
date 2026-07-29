package com.coppersmith.music1chat.speech

// Music1Chat revision: 2026-07-27 v01
//
// Supplies the installed English Android voices for the Settings voice picker.
// Voice IDs remain internal; Settings displays friendly names.

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

object VoiceExplorer {

    data class VoiceOption(
        val voiceId: String,
        val displayName: String,
        val localeName: String,
        val isNetworkVoice: Boolean
    )

    /**
     * Loads the installed English voices exposed by the phone's current
     * TextToSpeech engine.
     *
     * The temporary TextToSpeech instance is shut down immediately after
     * the catalog has been collected.
     */
    fun loadInstalledEnglishVoices(
        context: Context,
        onLoaded: (List<VoiceOption>) -> Unit
    ) {
        var temporaryEngine: TextToSpeech? = null

        temporaryEngine =
            TextToSpeech(
                context.applicationContext
            ) { status ->
                val engine = temporaryEngine

                if (
                    status != TextToSpeech.SUCCESS ||
                    engine == null
                ) {
                    engine?.shutdown()
                    temporaryEngine = null
                    onLoaded(emptyList())
                    return@TextToSpeech
                }

                val installedEnglishVoices =
                    engine.voices
                        .orEmpty()
                        .filter { voice: Voice ->
                            voice.locale.language == "en"
                        }
                        .sortedWith(
                            compareBy<Voice>(
                                { voice ->
                                    voice.locale.toLanguageTag()
                                },
                                { voice ->
                                    voice.isNetworkConnectionRequired
                                },
                                { voice ->
                                    voice.name
                                }
                            )
                        )

                val counters =
                    mutableMapOf<String, Int>()

                val options =
                    installedEnglishVoices.map { voice ->
                        val localeName =
                            voice.locale.getDisplayName(
                                Locale.getDefault()
                            )

                        val sourceName =
                            if (
                                voice.isNetworkConnectionRequired
                            ) {
                                "Network"
                            } else {
                                "Local"
                            }

                        val counterKey =
                            "${voice.locale.toLanguageTag()}-$sourceName"

                        val voiceNumber =
                            counters.getOrDefault(
                                counterKey,
                                0
                            ) + 1

                        counters[counterKey] = voiceNumber

                        VoiceOption(
                            voiceId = voice.name,
                            displayName =
                                "$localeName • $sourceName Voice $voiceNumber",
                            localeName = localeName,
                            isNetworkVoice =
                                voice.isNetworkConnectionRequired
                        )
                    }

                engine.shutdown()
                temporaryEngine = null

                onLoaded(options)
            }
    }
}