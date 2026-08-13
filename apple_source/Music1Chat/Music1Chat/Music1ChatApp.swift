import SwiftUI

@main
struct Music1ChatApp: App {
    @StateObject private var player: AudioPlayerService

    init() {
        let player = AudioPlayerService()
        _player = StateObject(wrappedValue: player)

        Music1ChatIntentBridge.shared.connect(player: player)
    }

    var body: some Scene {
        WindowGroup {
            MainScreen()
                .environmentObject(player)
        }
    }
}
