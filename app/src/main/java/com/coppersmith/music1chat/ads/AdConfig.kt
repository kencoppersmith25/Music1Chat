package com.coppersmith.music1chat.ads

/**
 * Official AdMob Configuration for No Hands Radio
 * Reconciled: August 27, 2026
 */
object AdConfig {
    // --- MASTER CONTROLS ---
    const val USE_TEST_ADS = true // SET TO FALSE FOR PRODUCTION
    const val SHOW_INTERSTITIALS = true
    
    // Timer control (set to 5 minutes for testing)
    const val MINIMUM_INTERSTITIAL_INTERVAL_MS = 5L * 60L * 1_000L

    // --- ANDROID ---
    const val ANDROID_APP_ID = "ca-app-pub-6232643827829257~1041107129"
    
    // Official Google Test IDs for Banners and Interstitials
    private const val ANDROID_TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val ANDROID_TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    
    // Your Live Android IDs
    private const val ANDROID_LIVE_BANNER = "ca-app-pub-6232643827829257/3475698773"
    const val ANDROID_LIVE_INTERSTITIAL = "" // Add your ID here when ready

    val ANDROID_BANNER_ID: String
        get() = if (USE_TEST_ADS) ANDROID_TEST_BANNER else ANDROID_LIVE_BANNER

    val ANDROID_INTERSTITIAL_ID: String
        get() = if (USE_TEST_ADS) ANDROID_TEST_INTERSTITIAL else ANDROID_LIVE_INTERSTITIAL

    // --- IOS ---
    const val IOS_APP_ID = "ca-app-pub-6232643827829257~8341240587"
    const val IOS_BANNER_ID = "ca-app-pub-6232643827829257/2486278674"
    const val APPLE_APP_STORE_ID = "6802332060"
}
