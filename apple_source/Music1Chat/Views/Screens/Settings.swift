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

        var displayTitle: String {
            switch self {
            case .nextCategory:
                return "Previous category"
            case .previousStation:
                return "Previous station in category"
            }
        }
    }

    private enum Keys {
        static let maximumSearchResults = "maximum_search_results"
        static let categoryAnnouncementsEnabled = "category_announcements_enabled"
        static let categoryVoiceIdentifier = "category_voice_identifier"
        static let previousTrackBehavior = "previous_track_behavior"
        static let feedbackSoundsEnabled = "feedback_sound_enabled"
        static let developerModeActive = "developer_mode_active"
    }

    private let defaults: UserDefaults

    @Published var maximumSearchResults: Int {
        didSet { defaults.set(maximumSearchResults, forKey: Keys.maximumSearchResults) }
    }

    @Published var categoryAnnouncementsEnabled: Bool {
        didSet { defaults.set(categoryAnnouncementsEnabled, forKey: Keys.categoryAnnouncementsEnabled) }
    }

    @Published var categoryVoiceIdentifier: String? {
        didSet {
            if let id = categoryVoiceIdentifier {
                defaults.set(id, forKey: Keys.categoryVoiceIdentifier)
            } else {
                defaults.removeObject(forKey: Keys.categoryVoiceIdentifier)
            }
        }
    }

    @Published var previousTrackBehavior: PreviousTrackBehavior {
        didSet { defaults.set(previousTrackBehavior.rawValue, forKey: Keys.previousTrackBehavior) }
    }

    @Published var feedbackSoundsEnabled: Bool {
        didSet { defaults.set(feedbackSoundsEnabled, forKey: Keys.feedbackSoundsEnabled) }
    }

    @Published var developerModeActive: Bool {
        didSet { defaults.set(developerModeActive, forKey: Keys.developerModeActive) }
    }

    var selectedVoiceDisplayName: String {
        guard let identifier = categoryVoiceIdentifier, !identifier.isEmpty,
              let voice = AVSpeechSynthesisVoice(identifier: identifier) else {
            return "Phone Default"
        }

        let languageName = Locale.current.localizedString(forIdentifier: voice.language)
            ?? voice.language
        return "\(languageName) • \(voice.name)"
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults

        let maxResults = defaults.integer(forKey: Keys.maximumSearchResults)
        self.maximumSearchResults = maxResults > 0 ? maxResults : 50

        self.categoryAnnouncementsEnabled = defaults.object(forKey: Keys.categoryAnnouncementsEnabled) as? Bool ?? true

        self.categoryVoiceIdentifier = defaults.string(forKey: Keys.categoryVoiceIdentifier)

        self.previousTrackBehavior = Music1ChatSettings.PreviousTrackBehavior(
            rawValue: defaults.string(forKey: Keys.previousTrackBehavior) ?? ""
        ) ?? .nextCategory

        self.feedbackSoundsEnabled = defaults.object(forKey: Keys.feedbackSoundsEnabled) as? Bool ?? true

        self.developerModeActive = defaults.bool(forKey: Keys.developerModeActive)
    }
}

@MainActor
final class CategoryAnnouncementSpeaker: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    private let synthesizer = AVSpeechSynthesizer()
    var onSpeechFinished: (() -> Void)?

    override init() {
        super.init()
        synthesizer.delegate = self
    }

    var isSpeaking: Bool {
        synthesizer.isSpeaking
    }

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
        voiceIdentifier: String?,
        completion: (() -> Void)? = nil
    ) {
        self.onSpeechFinished = completion
        speak(categoryName, voiceIdentifier: voiceIdentifier)
    }

    func previewVoice(identifier: String?, completion: (() -> Void)? = nil) {
        self.onSpeechFinished = completion
        speak("No Hands Radio category announcement", voiceIdentifier: identifier)
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

    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        onSpeechFinished?()
        onSpeechFinished = nil
    }

    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        onSpeechFinished?()
        onSpeechFinished = nil
    }
}

struct SettingsPanel: View {
    @ObservedObject var settings: Music1ChatSettings
    @ObservedObject var speaker: CategoryAnnouncementSpeaker
    let onDismiss: () -> Void

    @EnvironmentObject private var player: AudioPlayerService
    @State private var showVoicePicker = false
    @State private var showLogViewer = false
    @State private var versionTapCount = 0

    var body: some View {
        ZStack {
            VStack(alignment: .leading, spacing: 18) {
                HStack {
                    Text("Settings")
                        .font(.system(size: 24, weight: .bold))
                    Spacer()
                    Button {
                        onDismiss()
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 28))
                            .foregroundStyle(.secondary)
                    }
                    .buttonStyle(.plain)
                }

                ScrollView {
                    VStack(alignment: .leading, spacing: 22) {
                        searchSection
                        Divider()
                        audioSection
                        Divider()
                        voiceSection
                        Divider()
                        previousTrackSection

                        if settings.developerModeActive {
                            Divider()
                            diagnosticSection
                                .transition(.opacity.combined(with: .move(edge: .bottom)))
                        }

                        Divider()
                        aboutSection
                    }
                    .padding(.bottom, 40)
                }
            }
            .padding(20)
            .frame(maxWidth: 360, maxHeight: 520)
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
        .sheet(isPresented: $showLogViewer) {
            LogViewerModalView()
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

    private var audioSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Audio")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(.purple)

            Toggle(isOn: $settings.feedbackSoundsEnabled) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("System feedback sounds")
                        .font(.system(size: 18, weight: .semibold))
                    Text("Play a gentle sound when skipping stations or starting playback.")
                        .font(.system(size: 14))
                        .foregroundStyle(.secondary)
                }
            }
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
            Text("Previous (Voice or Bluetooth button)")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(.purple)

            Text("Choose what happens when you say “Siri, Previous” or press the Previous button on a Bluetooth device.")
                .font(.system(size: 14))
                .foregroundStyle(.secondary)

            radioRow(
                behavior: .nextCategory,
                title: "Next Category",
                detail: "Recommended. Advancing to the next category."
            )

            Divider()

            radioRow(
                behavior: .previousStation,
                title: "Previous Station",
                detail: "Returning to the previous station."
            )
        }
    }

    private var diagnosticSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Diagnostics")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(.purple)

            Button {
                showLogViewer = true
            } label: {
                HStack {
                    Image(systemName: "doc.text.magnifyingglass")
                        .font(.system(size: 20))
                    VStack(alignment: .leading, spacing: 3) {
                        Text("View Ride Log")
                            .font(.system(size: 18, weight: .semibold))
                        Text("Inspect technical logs directly on device.")
                            .font(.system(size: 14))
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .foregroundStyle(.secondary)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            Button {
                shareLog()
            } label: {
                HStack {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 20))
                    VStack(alignment: .leading, spacing: 3) {
                        Text("Share Ride Log")
                            .font(.system(size: 18, weight: .semibold))
                        Text("Send technical logs to the developer for troubleshooting.")
                            .font(.system(size: 14))
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Image(systemName: "square.and.arrow.up")
                        .foregroundStyle(.secondary)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

//             Button(role: .destructive) {
//                 RideLogger.shared.clearLog()
//             } label: {
//                 HStack {
//                     Image(systemName: "trash.fill")
//                     Text("Clear Log")
//                 }
//             }
            .buttonStyle(.bordered)
            .padding(.top, 4)
        }
    }

    private var aboutSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("About")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(.purple)

                Spacer()

                if settings.developerModeActive {
                    Text("Dev Mode Active")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(.purple)
                }
            }
            .contentShape(Rectangle())
            .allowsHitTesting(true)
            .onTapGesture {
                versionTapCount += 1
                print("DEBUG: About header tapped count = \(versionTapCount)")

                if versionTapCount >= 5 {
                    Task { @MainActor in
                        withAnimation {
                            settings.developerModeActive.toggle()
                        }
                    }
                    versionTapCount = 0
                }
            }

            VStack(alignment: .leading, spacing: 3) {
                Text("No Hands Radio")
                    .font(.system(size: 18, weight: .semibold))
                Text("The world’s most accessible radio app, designed for the ride.")
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.leading)

                Text("Version \(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0") (\(Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"))")
                    .font(.system(size: 13))
                    .foregroundStyle(.secondary)
                    .padding(.vertical, 4)
            }
        }
    }

    private func shareLog() {
        guard let url = RideLogger.shared.getLogURL() else { return }

        let activityVC = UIActivityViewController(activityItems: [url], applicationActivities: nil)

        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let rootVC = windowScene.windows.first(where: { $0.isKeyWindow })?.rootViewController {

            if let popover = activityVC.popoverPresentationController {
                popover.sourceView = rootVC.view
                popover.sourceRect = CGRect(x: rootVC.view.bounds.midX, y: rootVC.view.bounds.midY, width: 0, height: 0)
                popover.permittedArrowDirections = []
            }

            rootVC.present(activityVC, animated: true)
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
                            detail: "Use the phone’s normal Text-to-Speech voice.",
                            player: player
                        )

                        ForEach(speaker.availableEnglishVoices, id: \.identifier) { voice in
                            Divider()
                            voiceRow(
                                identifier: voice.identifier,
                                title: voiceTitle(voice),
                                detail: voiceQualityText(voice),
                                player: player
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
        detail: String,
        player: AudioPlayerService
    ) -> some View {
        let isSelected = settings.categoryVoiceIdentifier == identifier

        return Button {
            settings.categoryVoiceIdentifier = identifier
            player.setVolume(0.15)
            speaker.previewVoice(identifier: identifier) {
                player.setVolume(1.0)
            }
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

// MARK: - Log Viewer Modal
struct LogViewerModalView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var logContent: String = ""

    var body: some View {
        NavigationView {
            ScrollView {
                Text(logContent.isEmpty ? "No log entries recorded." : logContent)
                    .font(.system(.caption, design: .monospaced))
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .navigationTitle("Ride Log")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button("Clear") {
                        RideLogger.shared.clearLog()
                        loadLog()
                    }
                }
            }
            .onAppear {
                loadLog()
            }
        }
    }

    private func loadLog() {
        if let url = RideLogger.shared.getLogURL(),
           let text = try? String(contentsOf: url, encoding: .utf8) {
            logContent = text
        } else {
            logContent = "No log file found."
        }
    }
}