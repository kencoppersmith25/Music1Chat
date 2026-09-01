package com.coppersmith.music1chat.ads

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Central advertising policy for No Hands Radio.
 */
object AdManager {

    private const val STARTUP_GRACE_PERIOD_MS =
        8L * 1_000L

    private var applicationStartedAtElapsedMs: Long = 0L
    private var lastInterstitialShownAtElapsedMs: Long = Long.MIN_VALUE

    private val initialized = AtomicBoolean(false)
    private val interstitialShowing = AtomicBoolean(false)
    private var mInterstitialAd: InterstitialAd? = null

    private var isAppInForeground: () -> Boolean = { false }
    private var isScreenInteractive: () -> Boolean = { true }
    private var isPlaybackRequested: () -> Boolean = { false }

    fun initialize(
        application: Application,
        appInForegroundProvider: () -> Boolean,
        screenInteractiveProvider: () -> Boolean,
        playbackRequestedProvider: () -> Boolean
    ) {
        if (!initialized.compareAndSet(false, true)) {
            return
        }

        isAppInForeground = appInForegroundProvider
        isScreenInteractive = screenInteractiveProvider
        isPlaybackRequested = playbackRequestedProvider
        applicationStartedAtElapsedMs = SystemClock.elapsedRealtime()

        loadInterstitial(application)
    }

    private fun loadInterstitial(application: Application) {
        if (!AdConfig.SHOW_INTERSTITIALS) return
        
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(application, AdConfig.ANDROID_INTERSTITIAL_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }
            })
    }

    fun mayShowBanner(): Boolean {
        return initialized.get() &&
                isAppInForeground() &&
                isScreenInteractive()
    }

    fun maybeShowInterstitial(
        activity: Activity,
        reason: AdReason,
        onDismissed: () -> Unit = {}
    ): AdDecision {
        val decision = evaluateInterstitial(reason)

        if (decision != AdDecision.ALLOWED) {
            onDismissed()
            return decision
        }

        val ad = mInterstitialAd
        if (ad == null) {
            loadInterstitial(activity.application)
            onDismissed()
            return AdDecision.AD_NOT_READY
        }

        if (!interstitialShowing.compareAndSet(false, true)) {
            onDismissed()
            return AdDecision.ALREADY_SHOWING
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialShowing.set(false)
                mInterstitialAd = null
                loadInterstitial(activity.application)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                interstitialShowing.set(false)
                mInterstitialAd = null
                onDismissed()
            }
        }

        lastInterstitialShownAtElapsedMs = SystemClock.elapsedRealtime()
        ad.show(activity)

        return AdDecision.ALLOWED
    }

    private fun evaluateInterstitial(
        reason: AdReason
    ): AdDecision {
        if (!AdConfig.SHOW_INTERSTITIALS) {
            return AdDecision.REASON_DISABLED
        }
        
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

        if (now - applicationStartedAtElapsedMs < STARTUP_GRACE_PERIOD_MS) {
            return AdDecision.STARTUP_GRACE_PERIOD
        }

        if (lastInterstitialShownAtElapsedMs != Long.MIN_VALUE &&
            now - lastInterstitialShownAtElapsedMs < AdConfig.MINIMUM_INTERSTITIAL_INTERVAL_MS
        ) {
            return AdDecision.TOO_SOON
        }

        if (!reason.isEnabledByPolicy()) {
            return AdDecision.REASON_DISABLED
        }

        return AdDecision.ALLOWED
    }

    private fun AdReason.isEnabledByPolicy(): Boolean {
        return when (this) {
            AdReason.FIRST_MANUAL_PLAY -> true
            AdReason.RETURN_TO_APP -> true
            else -> false
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
