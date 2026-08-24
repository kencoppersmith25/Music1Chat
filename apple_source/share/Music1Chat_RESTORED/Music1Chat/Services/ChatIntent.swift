//
//  ChatIntent.swift
//  Music1Chat
//
//  Created by kenneth coppersmith on 8/10/26.
//

//import AppIntents
//import Foundation

import Foundation
import AppIntents

// MARK: - Next Station

struct NextStationIntent: AppIntent {
    static let title: LocalizedStringResource = "Next Station"
    static let description = IntentDescription(
        "Play the next available radio station in No Hands Radio."
    )

    static let openAppWhenRun = false

    @MainActor
    func perform() async throws -> some IntentResult {
        Music1ChatIntentBridge.shared.nextStation()
        return .result()
    }
}

// MARK: - Previous Station

struct PreviousStationIntent: AppIntent {
    static let title: LocalizedStringResource = "Previous Station"
    static let description = IntentDescription(
        "Play the previous available radio station in No Hands Radio."
    )

    static let openAppWhenRun = false

    @MainActor
    func perform() async throws -> some IntentResult {
        Music1ChatIntentBridge.shared.previousStation()
        return .result()
    }
}

// MARK: - Next Category

struct NextCategoryIntent: AppIntent {
    static let title: LocalizedStringResource = "Next Category"
    static let description = IntentDescription(
        "Move to the next radio category in No Hands Radio."
    )

    static let openAppWhenRun = false

    @MainActor
    func perform() async throws -> some IntentResult {
        Music1ChatIntentBridge.shared.nextCategory()
        return .result()
    }
}

// MARK: - Previous Category

struct PreviousCategoryIntent: AppIntent {
    static let title: LocalizedStringResource = "Previous Category"
    static let description = IntentDescription(
        "Move to the previous radio category in No Hands Radio."
    )

    static let openAppWhenRun = false

    @MainActor
    func perform() async throws -> some IntentResult {
        Music1ChatIntentBridge.shared.previousCategory()
        return .result()
    }
}

// MARK: - App Intent Bridge

@MainActor

final class Music1ChatIntentBridge {

    static let shared = Music1ChatIntentBridge()

    private weak var player: AudioPlayerService?

    private var nextCategoryAction: (() -> Void)?
    private var previousCategoryAction: (() -> Void)?

    private init() {}

    func connect(player: AudioPlayerService) {
        self.player = player
    }

    func connectCategoryActions(
        next: @escaping () -> Void,
        previous: @escaping () -> Void
    ) {
        nextCategoryAction = next
        previousCategoryAction = previous
    }

    func disconnectCategoryActions() {
        nextCategoryAction = nil
        previousCategoryAction = nil
    }

    func nextStation() {
        player?.next()
    }

    func previousStation() {
        player?.previous()
    }

    func nextCategory() {
        nextCategoryAction?()
    }

    func previousCategory() {
        previousCategoryAction?()
    }
}

// MARK: - Notifications

extension Notification.Name {
    static let noHandsRadioNextCategory =
        Notification.Name("NoHandsRadio.NextCategory")

    static let noHandsRadioPreviousCategory =
        Notification.Name("NoHandsRadio.PreviousCategory")
}


// MARK: - App Shortcuts

struct Music1ChatShortcuts: AppShortcutsProvider {

    @AppShortcutsBuilder
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: NextStationIntent(),
            phrases: [
                "Next station in \(.applicationName)",
                "Next track in \(.applicationName)",
                "Next category in \(.applicationName)",
                "Play the next station in \(.applicationName)"
            ],
            shortTitle: "Next Station",
            systemImageName: "forward.fill"
        )

        AppShortcut(
            intent: PreviousStationIntent(),
            phrases: [
                "Previous station in \(.applicationName)",
                "Previous track in \(.applicationName)",
                "Previous category in \(.applicationName)",
                "Play the previous station in \(.applicationName)"
            ],
            shortTitle: "Previous Station",
            systemImageName: "backward.fill"
        )

        // Keep specific category intents for advanced users/shortcuts
        AppShortcut(
            intent: NextCategoryIntent(),
            phrases: [
                "Skip category in \(.applicationName)",
                "Next genre in \(.applicationName)"
            ],
            shortTitle: "Next Category",
            systemImageName: "arrow.right.circle.fill"
        )

        AppShortcut(
            intent: PreviousCategoryIntent(),
            phrases: [
                "Previous category in \(.applicationName)",
                "Previous category in \(.applicationName)",
                "Go to the previous category in \(.applicationName)"
            ],
            shortTitle: "Previous Category",
            systemImageName: "arrow.left.circle.fill"
        )
    }
}
