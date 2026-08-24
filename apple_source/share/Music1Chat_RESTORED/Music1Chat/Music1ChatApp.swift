import SwiftUI

@main
struct Music1ChatApp: App {
    @StateObject private var player = AudioPlayerService()

    init() {
        // No-op init, StateObject handles the instance
    }

    var body: some Scene {
        WindowGroup {
            MainScreen()
                .environmentObject(player)
        }
    }
}
