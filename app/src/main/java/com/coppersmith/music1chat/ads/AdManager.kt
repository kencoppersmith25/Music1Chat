package com.coppersmith.music1chat.ads

import android.app.Application
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Central advertising policy for Music1Chat.
 *
 * This first version deliberately has no dependency on an advertising SDK.
 * It decides whether an ad is allowed and delegates actual display work to
 * callbacks supplied later by the AdMob integration layer.
 */
object AdManager {

    private const val MINIMUM_INTERSTITIAL_INTERVAL_MS =
        20L * 60L * 1_000L

    private const val STARTUP_GRACE_PERIOD_MS =
        8L * 1_000L

    private var applicationStartedAtElapsedMs: Long = 0L
    private var lastInterstitialShownAtElapsedMs: Long = Long.MIN_VALUE

    private val initialized = AtomicBoolean(false)
    private val interstitialShowing = AtomicBoolean(false)

    private var isAppInForeground: () -> Boolean = { false }
    private var isScreenInteractive: () -> Boolean = { true }
    private var isPlaybackRequested: () -> Boolean = { false }

    /**
     * Called once from Application.onCreate().
     */
    fun initialize(
        application: Application,
        appInForegroundProvider: () -> Boolean,
        screenInteractiveProvider: () -> Boolean,
        playbackRequestedProvider: () -> Boolean
    ) {
        if (!initialized.compareAndSet(false, true)) {
            return
        }

        // Retaining Application here is safe if the ad SDK later needs it.
        application.applicationContext

        isAppInForeground = appInForegroundProvider
        isScreenInteractive = screenInteractiveProvider
        isPlaybackRequested = playbackRequestedProvider
        applicationStartedAtElapsedMs = SystemClock.elapsedRealtime()
    }

    /**
     * Returns true when a persistent banner may be visible.
     *
     * Banners are allowed while music is playing because they do not interfere
     * with audio. They should only be attached to visible app screens.
     */
    fun mayShowBanner(): Boolean {
        return initialized.get() &&
                isAppInForeground() &&
                isScreenInteractive()
    }

    /**
     * Attempts to show a full-screen visual ad.
     *
     * Playback may continue underneath the ad. The callback must not pause,
     * stop, replace, or otherwise manipulate Music1Chat playback.
     */
    fun maybeShowInterstitial(
        reason: AdReason,
        showAd: (onDismissed: () -> Unit) -> Boolean
    ): AdDecision {
        val decision = evaluateInterstitial(reason)

        if (decision != AdDecision.ALLOWED) {
            return decision
        }

        if (!interstitialShowing.compareAndSet(false, true)) {
            return AdDecision.ALREADY_SHOWING
        }

        val accepted = try {
            showAd {
                interstitialShowing.set(false)
            }
        } catch (_: Throwable) {
            interstitialShowing.set(false)
            false
        }

        if (!accepted) {
            interstitialShowing.set(false)
            return AdDecision.AD_NOT_READY
        }

        lastInterstitialShownAtElapsedMs =
            SystemClock.elapsedRealtime()

        return AdDecision.ALLOWED
    }

    fun onInterstitialDismissed() {
        interstitialShowing.set(false)
    }

    private fun evaluateInterstitial(
        reason: AdReason
    ): AdDecision {
        if (!initialized.get()) {
            return AdDecision.NOT_INITIALIZED
        }

        if (interstitialShowing.get()) {
            return AdDecision.ALREADY_SHOWING
        }

        if (!isAppInForeground()) {
            return AdDecision.APP_NOT_FOREGROUND
        }

        if (!isScreenInteractive()) {
            return AdDecision.SCREEN_NOT_INTERACTIVE
        }

        val now = SystemClock.elapsedRealtime()

        if (
            now - applicationStartedAtElapsedMs <
            STARTUP_GRACE_PERIOD_MS
        ) {
            return AdDecision.STARTUP_GRACE_PERIOD
        }

        if (
            lastInterstitialShownAtElapsedMs != Long.MIN_VALUE &&
            now - lastInterstitialShownAtElapsedMs <
            MINIMUM_INTERSTITIAL_INTERVAL_MS
        ) {
            return AdDecision.TOO_SOON
        }

        if (!reason.isEnabledByPolicy()) {
            return AdDecision.REASON_DISABLED
        }

        /*
         * Music may continue playing beneath a visual interstitial. We do not
         * pause or stop Music1Chat playback here. The advertising SDK remains
         * responsible for the ad's own presentation and audio behavior.
         */
        isPlaybackRequested()

        return AdDecision.ALLOWED
    }

    private fun AdReason.isEnabledByPolicy(): Boolean {
        return when (this) {
            AdReason.FIRST_MANUAL_PLAY -> true
            AdReason.RETURN_TO_APP -> true

            // Keep these disabled initially. They can be enabled later without
            // changing MainScreen, Settings, or search code.
            AdReason.APP_START -> false
            AdReason.SEARCH_COMPLETED -> false
            AdReason.OPEN_SETTINGS -> false
            AdReason.STATION_CHANGE -> false
            AdReason.BLUETOOTH_COMMAND -> false
            AdReason.CAST_COMMAND -> false
        }
    }
}

enum class AdReason {
    APP_START,
    FIRST_MANUAL_PLAY,
    SEARCH_COMPLETED,
    OPEN_SETTINGS,
    RETURN_TO_APP,
    STATION_CHANGE,
    BLUETOOTH_COMMAND,
    CAST_COMMAND
}

enum class AdDecision {
    ALLOWED,
    NOT_INITIALIZED,
    APP_NOT_FOREGROUND,
    SCREEN_NOT_INTERACTIVE,
    STARTUP_GRACE_PERIOD,
    TOO_SOON,
    REASON_DISABLED,
    ALREADY_SHOWING,
    AD_NOT_READY
}