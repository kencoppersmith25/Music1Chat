import SwiftUI

@main
struct Music1ChatApp: App {
    // Use the shared singleton instance so widget actions and UI state align perfectly
    @StateObject private var player = AudioPlayerService.shared
    @Environment(\.scenePhase) private var scenePhase

    init() {
        RideLogger.shared.log("APP_START: No Hands Radio (iOS)")
        MaintenanceService.scrub()
        
    #if DEBUG
        UserDefaults.standard.removePersistentDomain(forName: Bundle.main.bundleIdentifier!)
    #endif
    }

    var body: some Scene {
        WindowGroup {
            MainScreen()
                .environmentObject(player)
                .onOpenURL { url in
                    handleWidgetURL(url)
                }
                .onChange(of: scenePhase) { _, newPhase in
                    if newPhase == .active {
                        // Check for pending widget actions when returning to foreground
                        AudioPlayerService.shared.handlePendingWidgetAction()
                    }
                }
        }
    }

    private func handleWidgetURL(_ url: URL) {
        guard url.scheme == "music1chat",
              let components = URLComponents(url: url, resolvingAgainstBaseURL: true),
              let queryItems = components.queryItems,
              let actionType = queryItems.first(where: { $0.name == "type" })?.value else {
            return
        }

        let sharedDefaults = UserDefaults(suiteName: "group.com.coppersmith.music1chat")
        sharedDefaults?.set(Date(), forKey: "widgetActionTest_\(actionType)")
        sharedDefaults?.set(actionType, forKey: "pendingWidgetAction")
        
        print("🎯 Widget URL Action Handled: \(actionType)")
        
        // Instantly trigger check if app is already active in foreground
        AudioPlayerService.shared.handlePendingWidgetAction()
    }
}
