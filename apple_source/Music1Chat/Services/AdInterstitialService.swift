import Foundation
import GoogleMobileAds
import UIKit

/**
 * Central Interstitial Ad management for iOS.
 * Verified compliant with modern Swift Concurrency and AdMob SDK naming.
 */
@MainActor
class AdInterstitialService: NSObject, FullScreenContentDelegate {
    static let shared = AdInterstitialService()

    // Using the centralized config for timer and IDs
    private var minimumInterval: TimeInterval { AdConfig.minimumInterstitialInterval }
    private var lastAdShownAt: Date?

    private var interstitial: InterstitialAd?
    private var currentAdUnitID: String { AdConfig.interstitialID }

    override init() {
        super.init()
        if AdConfig.showInterstitials {
            loadAd()
        }
    }

    func loadAd() {
        guard AdConfig.showInterstitials else { return }
        let request = Request()

        // Using the "with:" label which resolved your earlier error
        InterstitialAd.load(with: currentAdUnitID, request: request) { ad, error in
            if let error = error {
                print("Failed to load interstitial ad: \(error.localizedDescription)")
                return
            }

            // Hop back to the Main Actor to safely update the property and delegate
            Task { @MainActor in
                self.interstitial = ad
                self.interstitial?.fullScreenContentDelegate = self
            }
        }
    }

    func maybeShow(from rootViewController: UIViewController) {
        guard let ad = interstitial else {
            loadAd()
            return
        }

        if let lastShown = lastAdShownAt, Date().timeIntervalSince(lastShown) < minimumInterval {
            return
        }

        // Using "from:" label requested by modern SDK
        ad.present(from: rootViewController)
    }

    // MARK: - FullScreenContentDelegate

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        lastAdShownAt = Date()
        interstitial = nil
        loadAd() // Pre-load the next one
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        interstitial = nil
        loadAd()
    }
}
