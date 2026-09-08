import Foundation
import AVFoundation
import GoogleMobileAds
import UIKit

/**
 * Central Interstitial Ad management for iOS with Safety Watchdog Timer.
 * Verified compliant with modern Swift Concurrency and AdMob SDK naming.
 */
@MainActor
class AdInterstitialService: NSObject, FullScreenContentDelegate {
    static let shared = AdInterstitialService()

    // Callbacks to coordinate audio playback state with the player
    var onPausePlayback: (() -> Void)?
    var onResumePlayback: (() -> Void)?

    // Using the centralized config for timer and dynamic test/live ID switching
    private var minimumInterval: TimeInterval { AdConfig.minimumInterstitialInterval }
    private var lastAdShownAt: Date?

    private var interstitial: InterstitialAd?

    private var safetyTimer: Timer?

    override init() {
        super.init()
        if AdConfig.showInterstitials {
            loadAd()
        }
    }

    func loadAd() {
        guard AdConfig.showInterstitials else { return }
        let request = Request()

        InterstitialAd.load(with: AdConfig.interstitialID, request: request) { ad, error in
            if let error = error {
                print("Failed to load interstitial ad: \(error.localizedDescription)")
                return
            }

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

        // Start the safety watchdog timer (45 seconds) in case the ad gets stuck
        startSafetyTimer(from: rootViewController)

        // Using "from:" label requested by modern SDK
        ad.present(from: rootViewController)
    }

    // MARK: - FullScreenContentDelegate

    func adWillPresentFullScreenContent(_ ad: FullScreenPresentingAd) {
        onPausePlayback?()
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
         cancelSafetyTimer()
         lastAdShownAt = Date()
         interstitial = nil

         // Explicitly reactivate audio session to fix post-ad silence glitch
         try? AVAudioSession.sharedInstance().setActive(true, options: .notifyOthersOnDeactivation)

         onResumePlayback?()
         loadAd() // Pre-load the next one
     }

     func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
         cancelSafetyTimer()
         interstitial = nil

         // Ensure audio session recovers even if ad presentation fails
         try? AVAudioSession.sharedInstance().setActive(true, options: .notifyOthersOnDeactivation)

         onResumePlayback?()
         loadAd()
     }

    // MARK: - Watchdog Timer Safeguard

    private func startSafetyTimer(from rootViewController: UIViewController) {
        cancelSafetyTimer()
        safetyTimer = Timer.scheduledTimer(withTimeInterval: 45.0, repeats: false) { [weak self] _ in
            Task { @MainActor in
                print("⚠️ Safety timeout triggered: Interstitial ad appears stuck. Force-dismissing.")
                self?.cancelSafetyTimer()
                self?.interstitial = nil

                // Force-dismiss the presented modal overlay
                rootViewController.dismiss(animated: true) {
                    try? AVAudioSession.sharedInstance().setActive(true, options: .notifyOthersOnDeactivation)
                    self?.onResumePlayback?()
                    self?.loadAd()
                }
            }
        }
    }

    private func cancelSafetyTimer() {
        safetyTimer?.invalidate()
        safetyTimer = nil
    }
}