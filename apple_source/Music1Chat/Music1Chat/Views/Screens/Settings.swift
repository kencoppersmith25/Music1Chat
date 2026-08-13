import AVFoundation
import Combine
import Foundation
import SwiftUI

@MainActor
final class Music1ChatSettings: ObservableObject {
    enum PreviousTrackBehavior: String, CaseIterable, Identifiable {
        case nextCategory
        case previousStation

        var id: String { rawValue }
    }

    @Published var maximumSearchResults: Int {
        didSet {
            let clamped = min(max(maximumSearchResults, 5), 100)
            if clamped != maximumSearchResults {
                maximumSearchResults = clamped
                return
            }
            defaults.set(maximumSearchResults, forKey: Keys.maximumSearchResults)
        }
    }

    @Published var categoryAnnouncementsEnabled: Bool {
        didSet {
            defaults.set(categoryAnnouncementsEnabled, forKey: Keys.categoryAnnouncementsEnabled)
        }
    }

    @Published var categoryVoiceIdentifier: String? {
        didSet {
            defaults.set(categoryVoiceIdentifier, forKey: Keys.categoryVoiceIdentifier)
        }
    }

    @Published var previousTrackBehavior: PreviousTrackBehavior {
        didSet {
            defaults.set(previousTrackBehavior.rawValue, forKey: Keys.previousTrackBehavior)
        }
    }

    private let defaults: UserDefaults

    private enum Keys {
        static let maximumSearchResults = "Music1Chat.Settings.MaximumSearchResults"
        static let categoryAnnouncementsEnabled = "Music1Chat.Settings.CategoryAnnouncementsEnabled"
        static let categoryVoiceIdentifier = "Music1Chat.Settings.CategoryVoiceIdentifier"
        static let previousTrackBehavior = "Music1Chat.Settings.PreviousTrackBehavior"
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults

        let savedMaximum = defaults.integer(forKey: Keys.maximumSearchResults)
        maximumSearchResults = savedMaximum == 0 ? 50 : min(max(savedMaximum, 5), 100)

        if defaults.object(forKey: Keys.categoryAnnouncementsEnabled) == nil {
            categoryAnnouncementsEnabled = true
        } else {
            categoryAnnouncementsEnabled = defaults.bool(forKey: Keys.categoryAnnouncementsEnabled)
        }

        categoryVoiceIdentifier = defaults.string(forKey: Keys.categoryVoiceIdentifier)

        previousTrackBehavior = PreviousTrackBehavior(
            rawValue: defaults.string(forKey: Keys.previousTrackBehavior) ?? ""
        ) ?? .nextCategory
    }

    var selectedVoiceDisplayName: String {
        guard let identifier = categoryVoiceIdentifier,
              let voice = AVSpeechSynthesisVoice(identifier: identifier) else {
            return "Phone Default"
        }

        let languageName = Locale.current.localizedString(forIdentifier: voice.language)
            ?? voice.language
        return "\(languageName) • \(voice.name)"
    }
}

@MainActor
final class CategoryAnnouncementSpeaker: ObservableObject {
    private let synthesizer = AVSpeechSynthesizer()

    var availableEnglishVoices: [AVSpeechSynthesisVoice] {
        AVSpeechSynthesisVoice.speechVoices()
            .filter { voice in
                voice.language.lowercased().hasPrefix("en")
            }
            .sorted { lhs, rhs in
                let languageCompare = lhs.language.localizedCaseInsensitiveCompare(rhs.language)
                if languageCompare == .orderedSame {
                    return lhs.name.localizedCaseInsensitiveCompare(rhs.name) == .orderedAscending
                }
                return languageCompare == .orderedAscending
            }
    }

    func announceCategory(
        _ categoryName: String,
        voiceIdentifier: String?
    ) {
        speak(categoryName, voiceIdentifier: voiceIdentifier)
    }

    func previewVoice(identifier: String?) {
        speak("Music1Chat category announcement", voiceIdentifier: identifier)
    }

    private func speak(
        _ text: String,
        voiceIdentifier: String?
    ) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }

        let utterance = AVSpeechUtterance(string: trimmed)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate
        utterance.volume = 1.0

        if let identifier = voiceIdentifier,
           let voice = AVSpeechSynthesisVoice(identifier: identifier) {
            utterance.voice = voice
        }

        synthesizer.speak(utterance)
    }
}

struct SettingsPanel: View {
    @ObservedObject var settings: Music1ChatSettings
    @ObservedObject var speaker: CategoryAnnouncementSpeaker
    let onDismiss: () -> Void

    @State private var showVoicePicker = false

    var body: some View {
        ZStack {
            VStack(alignment: .leading, spacing: 18) {
                Text("Settings")
                    .font(.system(size: 28, weight: .bold))

                ScrollView {
                    VStack(alignment: .leading, spacing: 22) {
                        searchSection
                        Divider()
                        voiceSection
                        Divider()
                        previousTrackSection
                    }
                    .padding(.bottom, 8)
                }
            }
            .padding(20)
            .frame(maxWidth: 360, maxHeight: 570)
            .background(
                RoundedRectangle(cornerRadius: 24)
                    .fill(Color(.secondarySystemBackground))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 24)
                    .stroke(Color.white.opacity(0.92), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 24))

            if showVoicePicker {
                voicePickerOverlay
            }
        }
    }

    private var searchSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Search")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(.purple)

            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Maximum search results")
                        .font(.system(size: 18, weight: .semibold))
                    Text("Maximum number of live stations returned by a search.")
                        .font(.system(size: 14))
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Text("\(settings.maximumSearchResults)")
                    .font(.system(size: 27, weight: .bold))
                    .foregroundStyle(.purple)
            }

            Slider(
                value: Binding(
                    get: { Double(settings.maximumSearchResults) },
                    set: { settings.maximumSearchResults = Int($0.rounded()) }
                ),
                in: 5...100,
                step: 5
            )

            HStack {
                Text("5")
                Spacer()
                Text("100")
            }
            .font(.caption)
            .foregroundStyle(.secondary)
        }
    }

    private var voiceSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Voice")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(.purple)

            Toggle(isOn: $settings.categoryAnnouncementsEnabled) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Category announcements")
                        .font(.system(size: 18, weight: .semibold))
                    Text("Speak the category name after the category changes.")
                        .font(.system(size: 14))
                        .foregroundStyle(.secondary)
                }
            }

            Button {
                showVoicePicker = true
            } label: {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Category announcement voice")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(.primary)
                        Text(settings.selectedVoiceDisplayName)
                            .font(.system(size: 14))
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .foregroundStyle(.secondary)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .disabled(!settings.categoryAnnouncementsEnabled)
            .opacity(settings.categoryAnnouncementsEnabled ? 1.0 : 0.55)
        }
    }

    private var previousTrackSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Voice Previous Track")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(.purple)

            Text("Choose what happens when a headset, car, Lock Screen, or voice control sends Previous Track.")
                .font(.system(size: 14))
                .foregroundStyle(.secondary)

            radioRow(
                behavior: .nextCategory,
                title: "Next Category",
                detail: "Recommended. Previous Track advances to the next category."
            )

            Divider()

            radioRow(
                behavior: .previousStation,
                title: "Previous Station",
                detail: "Previous Track returns to the previous station."
            )
        }
    }

    private func radioRow(
        behavior: Music1ChatSettings.PreviousTrackBehavior,
        title: String,
        detail: String
    ) -> some View {
        Button {
            settings.previousTrackBehavior = behavior
        } label: {
            HStack(alignment: .top, spacing: 14) {
                Image(
                    systemName: settings.previousTrackBehavior == behavior
                        ? "largecircle.fill.circle"
                        : "circle"
                )
                .font(.system(size: 23))
                .foregroundStyle(settings.previousTrackBehavior == behavior ? .purple : .secondary)
                .padding(.top, 3)

                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.primary)
                    Text(detail)
                        .font(.system(size: 14))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.leading)
                }

                Spacer()
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private var voicePickerOverlay: some View {
        ZStack {
            Color.black.opacity(0.45)
                .ignoresSafeArea()
                .onTapGesture { showVoicePicker = false }

            VStack(alignment: .leading, spacing: 12) {
                Text("Category Announcement Voice")
                    .font(.system(size: 24, weight: .bold))

                Text("Tap a voice to hear it immediately. Your choice is saved automatically.")
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)

                ScrollView {
                    LazyVStack(spacing: 0) {
                        voiceRow(
                            identifier: nil,
                            title: "Phone Default",
                            detail: "Use the phone’s normal Text-to-Speech voice."
                        )

                        ForEach(speaker.availableEnglishVoices, id: \.identifier) { voice in
                            Divider()
                            voiceRow(
                                identifier: voice.identifier,
                                title: voiceTitle(voice),
                                detail: voiceQualityText(voice)
                            )
                        }
                    }
                }
            }
            .padding(18)
            .frame(maxWidth: 350, maxHeight: 570)
            .background(
                RoundedRectangle(cornerRadius: 24)
                    .fill(Color(.tertiarySystemBackground))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 24)
                    .stroke(Color.white.opacity(0.92), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .shadow(radius: 24)
            .padding(.horizontal, 20)
        }
    }

    private func voiceRow(
        identifier: String?,
        title: String,
        detail: String
    ) -> some View {
        let isSelected = settings.categoryVoiceIdentifier == identifier

        return Button {
            settings.categoryVoiceIdentifier = identifier
            speaker.previewVoice(identifier: identifier)
        } label: {
            HStack(alignment: .top, spacing: 14) {
                Image(systemName: isSelected ? "largecircle.fill.circle" : "circle")
                    .font(.system(size: 22))
                    .foregroundStyle(isSelected ? .purple : .secondary)
                    .padding(.top, 4)

                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 17, weight: .medium))
                        .foregroundStyle(.primary)
                        .multilineTextAlignment(.leading)
                    Text(detail)
                        .font(.system(size: 13))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.leading)
                }

                Spacer()
            }
            .padding(.vertical, 10)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func voiceTitle(_ voice: AVSpeechSynthesisVoice) -> String {
        let languageName = Locale.current.localizedString(forIdentifier: voice.language)
            ?? voice.language
        return "\(languageName) • \(voice.name)"
    }

    private func voiceQualityText(_ voice: AVSpeechSynthesisVoice) -> String {
        switch voice.quality {
        case .enhanced:
            return "Enhanced voice"
        default:
            return "Available on this iPhone"
        }
    }
}
