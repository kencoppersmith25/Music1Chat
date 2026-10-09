import AppIntents
import Foundation

nonisolated private func savePendingAction(_ action: String) {
    let sharedDefaults = UserDefaults(suiteName: "group.com.coppersmith.music1chat")
    sharedDefaults?.set(action, forKey: "pendingWidgetAction")
}

struct NextStationIntent: AppIntent {
    static var title: LocalizedStringResource = "Next Station"
    static var description = IntentDescription("Switches to the next radio station.")
    static var openAppWhenRun: Bool = true

    func perform() async throws -> some IntentResult {
        savePendingAction("nextStation")
        return .result()
    }
}

struct PreviousStationIntent: AppIntent {
    static var title: LocalizedStringResource = "Previous Station"
    static var description = IntentDescription("Switches to the previous radio station.")
    static var openAppWhenRun: Bool = true

    func perform() async throws -> some IntentResult {
        savePendingAction("previousStation")
        return .result()
    }
}

struct TogglePlaybackIntent: AppIntent {
    static var title: LocalizedStringResource = "Play / Pause"
    static var description = IntentDescription("Toggle Play / Pause")
    static var openAppWhenRun: Bool = true

    func perform() async throws -> some IntentResult {
        savePendingAction("playPause")
        return .result()
    }
}

struct NextCategoryIntent: AppIntent {
    static var title: LocalizedStringResource = "Next Category"
    static var description = IntentDescription("Switches to the next music category.")
    static var openAppWhenRun: Bool = true

    func perform() async throws -> some IntentResult {
        savePendingAction("nextCategory")
        return .result()
    }
}

struct PreviousCategoryIntent: AppIntent {
    static var title: LocalizedStringResource = "Previous Category"
    static var description = IntentDescription("Switches to the previous music category.")
    static var openAppWhenRun: Bool = true

    func perform() async throws -> some IntentResult {
        savePendingAction("previousCategory")
        return .result()
    }
}
