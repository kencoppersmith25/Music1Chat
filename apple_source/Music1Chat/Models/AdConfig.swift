import Foundation

/**
 * Official AdMob Configuration for No Hands Radio (iOS).
 * Centralized mission control for all advertising policy.
 */
struct AdConfig {
    // --- MASTER CONTROLS ---
    //setting this one to false now...
    static let useTestAds = false // SET TO FALSE FOR APP STORE RELEASE
    static let showInterstitials = true

    // Timer control (set to 20 mins for production, 5 mins for testing)
    static let minimumInterstitialInterval: TimeInterval = 5 * 60

    // --- IOS IDs ---
    static let appleAppID = "ca-app-pub-6232643827829257~8341240587"

    // Official Google Test IDs
    private static let testBannerID = "ca-app-pub-3940256099942544/2934735716"
    private static let testInterstitialID = "ca-app-pub-3940256099942544/4411468910"

    // Your Live Production IDs
    private static let liveBannerID = "ca-app-pub-6232643827829257/2486278674"
    private static let liveInterstitialID = "ca-app-pub-6232643827829257/1620348169"

    static var bannerID: String {
        return useTestAds ? testBannerID : liveBannerID
    }

    static var interstitialID: String {
        return useTestAds ? testInterstitialID : liveInterstitialID
    }
}
