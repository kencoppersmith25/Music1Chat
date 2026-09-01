import SwiftUI

@main
struct Music1ChatApp: App {
    @StateObject private var player = AudioPlayerService()

    init() {
        RideLogger.shared.log("APP_START: No Hands Radio (iOS)")
        MaintenanceService.scrub()
    }

    var body: some Scene {
        WindowGroup {
            MainScreen()
                .environmentObject(player)
        }
    }
}
