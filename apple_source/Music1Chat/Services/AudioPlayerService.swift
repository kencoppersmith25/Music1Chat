import AVFoundation
import AudioToolbox
import Combine
import Foundation
import MediaPlayer
import Network
import UIKit
import WidgetKit

private final class StreamMetadataDelegate: NSObject, AVPlayerItemMetadataOutputPushDelegate, @unchecked Sendable {
    var onItems: (([AVMetadataItem]) -> Void)?

    func metadataOutput(
        _ output: AVPlayerItemMetadataOutput,
        didOutputTimedMetadataGroups groups: [AVTimedMetadataGroup],
        from track: AVPlayerItemTrack?
    ) {
        let items = groups.flatMap(\.items)
        guard !items.isEmpty else { return }
        DispatchQueue.main.async { [weak self] in
            self?.onItems?(items)
        }
    }
}

// MARK: - Feedback Type Definition
enum FeedbackType {
    case stationChange
    case categoryChange
}

@MainActor
final class AudioPlayerService: ObservableObject {
    
    // 1. Shared singleton instance
    static let shared = AudioPlayerService()
    
    enum PlaybackState {
        case stopped
        case resolving
        case connecting
        case playing
        case paused
        case failed
    }
    
    // 2. Published state so your views (and optional app group updates) can track it
    @Published var playbackState: PlaybackState = .stopped {
        didSet {
            // Optional: Automatically sync state to your App Group for the widget
            saveStateToAppGroup()
        }
    }
    
    private func saveStateToAppGroup() {
        if let sharedDefaults = UserDefaults(suiteName: "group.com.coppersmith.music1chat") {
            let isPlaying = (playbackState == .playing)
            sharedDefaults.set(isPlaying, forKey: "isPlaying")
            
            let stateString: String
            switch playbackState {
            case .stopped: stateString = "Stopped"
            case .resolving: stateString = "Resolving..."
            case .connecting: stateString = "Connecting..."
            case .playing: stateString = "Playing"
            case .paused: stateString = "Paused"
            case .failed: stateString = "Failed"
            }
            sharedDefaults.set(stateString, forKey: "playbackStateDescription")
        }
    }
    
    struct SavedSearchQueue: Codable, Hashable, Identifiable {
        var id: String { name.lowercased() }
        let name: String
        let stations: [Station]
    }
    
    private struct PersistedPlaybackState: Codable {
        let savedSearchQueues: [SavedSearchQueue]
        let activeQueue: [Station]
        let activeQueueName: String?
        let currentQueueIndex: Int?
        let shouldResume: Bool
        let searchNavigation: [String: Bool]?
        let categoryNavigation: [String: Bool]?
        let stationNavigation: [String: Bool]?
        let lastPlayedIndices: [String: Int]?
    }
    
    private struct PreparedStream {
        let key: String
        let queueName: String?
        let queue: [Station]
        let requestedStartAt: Int
        let step: Int
        let resolvedIndex: Int
        let station: Station
        let url: URL
    }
    
    // MARK: - Published Properties
    @Published private(set) var currentStation: Station?
    @Published private(set) var errorMessage: String?
    @Published private(set) var failedStationID: UUID?
    
    @Published private(set) var activeQueue: [Station] = []
    @Published private(set) var currentQueueIndex: Int?
    @Published private(set) var activeQueueName: String?
    @Published private(set) var activeLibraryCategoryID: UUID?
    @Published private(set) var savedSearchQueues: [SavedSearchQueue] = []
    @Published private(set) var navigationRevision = 0
    @Published private(set) var nowPlayingTitle: String?
    @Published private(set) var nowPlayingArtist: String?
    @Published private(set) var auditionStatusMessage: String?
    @Published var feedbackSoundsEnabled = true
    @Published var currentTrackTitle: String = ""
    @Published var currentArtist: String = ""
    @Published var currentCategory: String = ""
    
    
    private var metadataObserver: NSKeyValueObservation?
    // MARK: - Callbacks
    var onPlaybackFailed: ((Station) -> Void)?
    var onAuditionSucceeded: (() -> Void)?
    var onAuditionFailed: ((String) -> Void)?
    var onPreviousTrackCommand: (() -> Void)? {
        didSet { updateRemoteCommandAvailability() }
    }
    var onNextTrackCommand: (() -> Void)? {
        didSet { updateRemoteCommandAvailability() }
    }
    
    // MARK: - Player & State Observers
    private var player: AVPlayer?
    private var timeControlObservation: NSKeyValueObservation?
    private var itemStatusObservation: NSKeyValueObservation?
    private var bufferEmptyObservation: NSKeyValueObservation?
    private var likelyToKeepUpObservation: NSKeyValueObservation?
    private var playbackStalledObserver: NSObjectProtocol?
    
    
    private var prefetchTasks: [String: Task<Void, Never>] = [:]
    private var progressCheckTask: Task<Void,Never>?
    private var stallRecoveryTask: Task<Void,Never>?
    private var lastObservedPosition: Double = -1
    private var positionStallCount = 0
    private var startupTask: Task<Void,Never>?
    private var resolutionTask: Task<Void,Never>?
    private var metadataOutput: AVPlayerItemMetadataOutput?
    private var metadataDelegate: StreamMetadataDelegate?
    private var audioRouteChangeObserver: NSObjectProtocol?
    private var audioInterruptionObserver: NSObjectProtocol?
    private var wasPlayingBeforeInterruption = false
    private var currentResolvedURL: URL?
    private var auditionResolvedURL: URL?
    
    static var lastNavigationTime: Date = .distantPast
    static let navigationCooldown: TimeInterval = 2.0
    var isNavigationInputActive = false
    
    private var lastCommandExecutionTime: Date = .distantPast
    private let throttleInterval: TimeInterval = 0.4
    
    private var preparedStreams: [String: PreparedStream] = [:]
    
    private var playbackGeneration = 0
    private var autoAdvanceOnFailure = true
    private var failedQueueIndices: Set<Int> = []
    
    // MARK: - Audition State
    private var auditionPlayer: AVPlayer?
    private var auditionTimeControlObservation: NSKeyValueObservation?
    private var auditionItemStatusObservation: NSKeyValueObservation?
    private var auditionStartupTask: Task<Void,Never>?
    private var auditionCompletion: (() -> Void)?
    private var auditionFailure: (() -> Void)?
    private var auditionResolutionTask: Task<Void,Never>?
    private var auditionGeneration = 0
    private var auditionQueue: [Station] = []
    private var auditionQueueName: String?
    private var auditionLibraryCategoryID: UUID?
    private var auditionQueueIndex = 0
    private var auditionFailedIndices: Set<Int> = []
    private var auditionStep = 1
    private var auditionShouldSaveQueue = true
    
    // MARK: - Dependencies & Configuration
    private let resolver = StreamResolverService()
    private let persistenceKey = "Music1Chat.PlaybackState.v1"
    private var isRestoringPersistedState = false
    
    private let networkMonitor = NWPathMonitor()
    private var isNetworkAvailable = true
    
    private let startupTimeoutNanoseconds: UInt64 = 4_500_000_000
    private let auditionTimeoutNanoseconds: UInt64 = 3_500_000_000
    
    private var searchNavigation: [String: Bool] = [:]
    private var categoryNavigation: [String: Bool] = [:]
    private var stationNavigation: [String: Bool] = [:]
    private var lastPlayedIndices: [String: Int] = [:]
    
    private static let handleDarwinNotification: CFNotificationCallback = { _, observer, notificationName, _, _ in
        guard let notificationName = notificationName else { return }
        let name = notificationName.rawValue as String
        
        // Add this log to see if the notification arrives while warm!
        RideLogger.shared.log("DARWIN_NOTIFICATION_RECEIVED: \(name)")
        
        Task { @MainActor in
            switch name {
            case "WidgetNextStation":
                AudioPlayerService.shared.next()
            case "WidgetPreviousStation":
                AudioPlayerService.shared.previous()
            case "WidgetTogglePlayPause":
                AudioPlayerService.shared.togglePlayback()
            case "WidgetNextCategory":
                AudioPlayerService.shared.switchToNextCategory()
            case "WidgetPreviousCategory":
                AudioPlayerService.shared.switchToPreviousCategory()
            default:
                break
            }
        }
    }
    
    init() {
        configureAudioSession()
        startNetworkMonitoring()
        
        // 1. Restore the saved state synchronously so the queue & stations exist immediately
        restorePersistedState()
        
        let observerPointer = Unmanaged.passUnretained(self).toOpaque()
        let widgetNotifications = [
            "WidgetNextStation",
            "WidgetPreviousStation",
            "WidgetTogglePlayPause",
            "WidgetNextCategory",
            "WidgetPreviousCategory"
        ]
        
        // 2. Register your Darwin notification observers for when the app is warm
        for notificationName in widgetNotifications {
            CFNotificationCenterAddObserver(
                CFNotificationCenterGetDarwinNotifyCenter(),
                observerPointer,
                Self.handleDarwinNotification,
                notificationName as CFString,
                nil,
                .deliverImmediately
            )
        }
        
        // 3. Now handle any pending widget action since the queue is fully loaded!
        handlePendingWidgetAction()
        
        // 4. Perform remaining background setup tasks
        Task {
            observeAudioRouteChanges()
            observeAudioInterruptions()
            configureRemoteCommands()
            updateRemoteCommandAvailability()
            updateNowPlayingInfo()
        }
    }
    
    var isPlaying: Bool {
        playbackState == .playing
    }
    
    var isConnecting: Bool {
        playbackState == .resolving || playbackState == .connecting
    }
    
    
    // ==========================================
    // 3. METADATA FUNCTIONS (Place after init)
    // ==========================================
    
    func setupMetadataObserver(for playerItem: AVPlayerItem) {
            metadataObserver = playerItem.observe(\.timedMetadata, options: [.new]) { [weak self] item, _ in
                guard let timedMetadata = item.timedMetadata else { return }
               
                for metadata in timedMetadata {
                    if let value = metadata.stringValue {
                        // Check if the stream packs both artist and title into a single string (e.g., "Artist - Title")
                        if value.contains(" - ") {
                            let parts = value.components(separatedBy: " - ")
                            if parts.count >= 2 {
                                let artist = parts[0].trimmingCharacters(in: .whitespaces)
                                let title = parts[1..<parts.count].joined(separator: " - ").trimmingCharacters(in: .whitespaces)
                                
                                // Explicitly update both individual fields if your model uses them
                                DispatchQueue.main.async {
                                    self?.nowPlayingArtist = artist
                                    self?.nowPlayingTitle = title
                                }
                            }
                        }
                        
                        // Fall back to your original string parser for everything else
                        self?.parseMetadataString(value)
                    }
                }
            }
        }
    
    private func parseMetadataString(_ rawString: String) {
        let cleaned = rawString.replacingOccurrences(of: "StreamTitle=", with: "")
                             .trimmingCharacters(in: CharacterSet(charactersIn: "\"'"))
        
        let artist: String
        let title: String
        
        let components = cleaned.components(separatedBy: " - ")
        if components.count >= 2 {
            artist = components[0].trimmingCharacters(in: .whitespaces)
            title = components[1...].joined(separator: " - ").trimmingCharacters(in: .whitespaces)
        } else {
            artist = ""
            title = cleaned.isEmpty ? "Live Stream" : cleaned
        }
        
        // Ensure UI updates and widget sync happen safely on the main thread
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            // Update your internal tracking variables
            self.currentArtist = artist
            self.currentTrackTitle = title
            
            // Crucial: Update the properties that MainScreen actually binds to!
            self.nowPlayingArtist = artist.isEmpty ? nil : artist
            self.nowPlayingTitle = title
            
            self.syncMetadataToAppGroup()
        }
    }
    
    private func syncMetadataToAppGroup() {
        guard let sharedDefaults = UserDefaults(suiteName: "group.com.coppersmith.music1chat") else { return }
        
        sharedDefaults.set(currentTrackTitle, forKey: "live_track_title")
        sharedDefaults.set(currentArtist, forKey: "live_artist")
        sharedDefaults.set(Date(), forKey: "last_updated")
        
        WidgetCenter.shared.reloadAllTimelines()
    }
    
    
    /// Throttles incoming control actions on the @MainActor to eliminate race conditions
    func executeThrottledAction(_ action: () -> Void) {
        let now = Date()
        guard now.timeIntervalSince(lastCommandExecutionTime) >= throttleInterval else { return }
        lastCommandExecutionTime = now
        action()
    }
    
    func playFeedbackSound(type: FeedbackType = .stationChange) {
        guard feedbackSoundsEnabled else { return }
        
        DispatchQueue.main.async {
            switch type {
            case .stationChange:
                AudioServicesPlaySystemSound(1057)
                
            case .categoryChange:
                AudioServicesPlaySystemSound(1057)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.08) {
                    AudioServicesPlaySystemSound(1057)
                }
            }
        }
    }
    
    func lastKnownIndex(categoryID: UUID? = nil, queueName: String? = nil, count: Int) -> Int {
        guard count > 0 else { return 0 }
        let key: String
        if let categoryID {
            key = "cat:\(categoryID.uuidString)"
        } else if let queueName, !queueName.isEmpty {
            key = "search:\(normalizedName(queueName))"
        } else {
            return 0
        }
        
        if let saved = lastPlayedIndices[key], saved >= 0, saved < count {
            return saved
        }
        return 0
    }
    
    func recordLastPlayedIndex(index: Int) {
        let key: String
        if let activeLibraryCategoryID {
            key = "cat:\(activeLibraryCategoryID.uuidString)"
        } else if let activeQueueName, !activeQueueName.isEmpty {
            key = "search:\(normalizedName(activeQueueName))"
        } else {
            return
        }
        lastPlayedIndices[key] = index
    }
    
    func play(station: Station) {
        cancelAudition()
        
        // Update active station & category state immediately
        currentStation = station
        currentCategory = activeQueueName ?? "Live Radio"
        currentTrackTitle = station.name
        currentArtist = activeQueueName ?? "Live Radio"
        
        // Populate UI/Widget properties so labels aren't blank or generic
        nowPlayingTitle = station.name
        nowPlayingArtist = activeQueueName ?? "Live Radio"
        
        syncMetadataToAppGroup()

        if let index = activeQueue.firstIndex(where: { $0.id == station.id }) {
            startQueueItem(at: index)
        } else {
            play(
                queue: [station],
                name: activeQueueName,
                startAt: 0,
                autoAdvanceOnFailure: true
            )
        }
    }
     
    func play(
        queue: [Station],
        name: String? = nil,
        libraryCategoryID: UUID? = nil,
        startAt index: Int = 0,
        autoAdvanceOnFailure: Bool = true
    ) {
        cancelStartupWatchdog()
        cancelStallRecovery()
        cancelAudition()
         
        guard !queue.isEmpty else {
            stop(clearQueue: true)
            return
        }
         
        let safeIndex = min(max(index, 0), queue.count - 1)
        stopCurrentAttempt()
         
        activeQueue = queue
        activeQueueName = name
        activeLibraryCategoryID = libraryCategoryID
        currentQueueIndex = safeIndex
         
        // Update current station and category from the active queue index
        let selectedStation = queue[safeIndex]
        currentStation = selectedStation
        currentCategory = name ?? activeQueueName ?? "Live Radio"
        currentTrackTitle = selectedStation.name
        currentArtist = currentCategory
        
        // Populate UI/Widget properties so labels reflect the station and category immediately
        nowPlayingTitle = selectedStation.name
        nowPlayingArtist = currentCategory
        
        failedQueueIndices.removeAll()
        self.autoAdvanceOnFailure = true
        updateRemoteCommandAvailability()
        persistState(shouldResume: true)
         
        // Sync these initial states to widgets/App Group
        syncMetadataToAppGroup()
         
        startQueueItem(at: safeIndex)
        isNavigationInputActive = false
    }

    func prefetch(queue: [Station], name: String?, startAt: Int, step: Int) {
        let key = prefetchKey(queue: queue, name: name, startAt: startAt, step: step)
        
        // Cancel any existing prefetch task for this key
        prefetchTasks[key]?.cancel()
        
        let task = Task { @MainActor [weak self] in
            guard let self else { return }
            defer { self.prefetchTasks[key] = nil }
            
            let count = queue.count
            guard count > 0 else { return }
            let targetIndex = ((startAt % count) + count) % count
            let station = queue[targetIndex]
            
            let result = await self.resolver.resolve(station: station)
            guard !Task.isCancelled, result.success, let resolvedURL = result.resolvedURL, let url = URL(string: resolvedURL) else { return }
            
            let prepared = PreparedStream(
                key: key,
                queueName: name,
                queue: queue,
                requestedStartAt: startAt,
                step: step,
                resolvedIndex: targetIndex,
                station: station,
                url: url
            )
            self.preparedStreams[key] = prepared
        }
        
        prefetchTasks[key] = task
    }
    
    func prefetchNextStation() {
        guard !activeQueue.isEmpty else { return }
        let count = activeQueue.count
        let current = currentQueueIndex ?? 0
        for distance in 1...count {
            let index = (current + distance) % count
            let station = activeQueue[index]
            if isStationNavigationEnabled(stationID: station.id, inQueueNamed: activeQueueName) {
                prefetch(queue: activeQueue, name: activeQueueName, startAt: index, step: 1)
                return
            }
        }
    }
    
    func audition(
        
        queue: [Station],
        name: String? = nil,
        libraryCategoryID: UUID? = nil,
        startAt index: Int = 0,
        step: Int = 1,
        saveAsSearch: Bool = true,
        statusMessage: String? = "Finding a playable station…",
        onSuccess: (() -> Void)? = nil,
        onFailure: (() -> Void)? = nil
    ) {
        cancelAudition()
        guard !queue.isEmpty else {
            onAuditionFailed?("No stations were found.")
            return
        }
        auditionQueue = queue
        auditionQueueName = name
        auditionLibraryCategoryID = libraryCategoryID
        auditionQueueIndex = min(max(index, 0), queue.count - 1)
        auditionFailedIndices.removeAll()
        auditionStep = step < 0 ? -1 : 1
        auditionShouldSaveQueue = saveAsSearch
        auditionStatusMessage = statusMessage
        auditionCompletion = onSuccess
        auditionFailure = onFailure
        auditionGeneration += 1
        
        if !startPreparedAuditionIfAvailable(requestedStartAt: auditionQueueIndex) {
            startAuditionItem(at: auditionQueueIndex)
        }
    }
    
    func auditionNextStation() {
        auditionRelativeStation(by: 1)
    }
    
    func auditionPreviousStation() {
        auditionRelativeStation(by: -1)
    }
    
    private func auditionRelativeStation(by offset: Int) {
        playFeedbackSound(type: .stationChange)
        
        let now = Date()
        guard now.timeIntervalSince(lastCommandExecutionTime) >= throttleInterval else { return }
        lastCommandExecutionTime = now
        
        guard now.timeIntervalSince(AudioPlayerService.lastNavigationTime) >= AudioPlayerService.navigationCooldown else { return }
        AudioPlayerService.lastNavigationTime = now
        
        guard !isNavigationInputActive else { return }
        isNavigationInputActive = true
        defer { isNavigationInputActive = false }
        
        cancelStartupWatchdog()
        cancelStallRecovery()
        cancelAudition()
        
        // Fully wipe out any existing player attempts
        stopCurrentAttempt(killPlayer: true)
        
        guard !activeQueue.isEmpty else { return }
        
        let count = activeQueue.count
        let current = currentQueueIndex ?? 0
        let direction = offset < 0 ? -1 : 1
        
        var target: Int?
        for step in 1...count {
            let raw = current + direction * step
            let candidate = ((raw % count) + count) % count
            let station = activeQueue[candidate]
            if isStationNavigationEnabled(stationID: station.id, inQueueNamed: activeQueueName) {
                target = candidate
                break
            }
        }
        guard let target else { return }
        
        auditionQueue = activeQueue
        auditionQueueName = activeQueueName
        auditionLibraryCategoryID = activeLibraryCategoryID
        auditionQueueIndex = target
        auditionFailedIndices = [current]
        auditionStep = direction
        auditionShouldSaveQueue = false
        auditionStatusMessage = direction > 0 ? "Finding next station…" : "Finding previous station…"
        auditionGeneration += 1
        
        // Instantly update UI state so it shows loading/resolving instead of stopped
        playbackState = .resolving
        
        if !startPreparedAuditionIfAvailable(requestedStartAt: target) {
            startAuditionItem(at: target)
        }
    }
    
    func auditionSavedSearchQueue(named name: String) {
        guard let saved = savedSearchQueues.first(where: { $0.name.caseInsensitiveCompare(name) == .orderedSame }) else { return }
        let startIndex = lastKnownIndex(categoryID: nil, queueName: saved.name, count: saved.stations.count)
        audition(queue: saved.stations, name: saved.name, startAt: startIndex, step: 1, saveAsSearch: false, statusMessage: "Finding next category…")
    }
    
    func cancelQueueAudition() {
        cancelAudition()
    }
    
    func playSavedSearchQueue(named name: String) {
        guard let saved = savedSearchQueues.first(where: { $0.name.caseInsensitiveCompare(name) == .orderedSame }) else { return }
        let startIndex = lastKnownIndex(categoryID: nil, queueName: saved.name, count: saved.stations.count)
        play(queue: saved.stations, name: saved.name, startAt: startIndex, autoAdvanceOnFailure: true)
    }
    
    func removeSavedSearchQueue(named name: String) {
        let key = normalizedName(name)
        let removingActiveQueue = normalizedName(activeQueueName ?? "") == key
        savedSearchQueues.removeAll { normalizedName($0.name) == key }
        if removingActiveQueue { activeQueueName = nil }
        searchNavigation.removeValue(forKey: key)
        lastPlayedIndices.removeValue(forKey: "search:\(key)")
        stationNavigation = stationNavigation.filter { !$0.key.hasPrefix("search:\(key)|") }
        navigationRevision += 1
        persistState(shouldResume: isPlaying || isConnecting)
    }
    
    func removeStationFromActiveQueue(stationID: UUID, categoryID: UUID? = nil) {
        guard let activeID = activeLibraryCategoryID else { return }
        if let categoryID, activeID != categoryID { return }
        guard let removedIndex = activeQueue.firstIndex(where: { $0.id == stationID }) else { return }
        
        let wasCurrentStation = currentStation?.id == stationID
        activeQueue.remove(at: removedIndex)
        if activeQueue.isEmpty {
            stop(clearQueue: true)
            return
        }
        
        if wasCurrentStation {
            let nextIndex = removedIndex % activeQueue.count
            currentQueueIndex = nextIndex
            currentStation = activeQueue[nextIndex]
            recordLastPlayedIndex(index: nextIndex)
            startQueueItem(at: nextIndex)
        } else if let currentIndex = currentQueueIndex {
            if removedIndex < currentIndex {
                currentQueueIndex = currentIndex - 1
            } else if removedIndex == currentIndex {
                currentQueueIndex = min(currentIndex, activeQueue.count - 1)
            }
            if let idx = currentQueueIndex { recordLastPlayedIndex(index: idx) }
        }
        updateNowPlayingInfo()
        updateRemoteCommandAvailability()
        persistState(shouldResume: isPlaying || isConnecting)
    }
    
    func removeStationFromSavedSearchQueue(named name: String, stationID: UUID) {
        guard let index = savedSearchQueues.firstIndex(where: { normalizedName($0.name) == normalizedName(name) }) else { return }
        let existing = savedSearchQueues[index]
        let remaining = existing.stations.filter { $0.id != stationID }
        if remaining.isEmpty {
            removeSavedSearchQueue(named: name)
            return
        }
        savedSearchQueues[index] = SavedSearchQueue(name: existing.name, stations: remaining)
        
        if normalizedName(activeQueueName ?? "") == normalizedName(name) {
            guard let removedIndex = activeQueue.firstIndex(where: { $0.id == stationID }) else {
                navigationRevision += 1
                persistState(shouldResume: isPlaying || isConnecting)
                return
            }
            let wasCurrentStation = currentStation?.id == stationID
            activeQueue.remove(at: removedIndex)
            if activeQueue.isEmpty {
                stop(clearQueue: true)
                return
            }
            if wasCurrentStation {
                let nextIndex = min(removedIndex, activeQueue.count - 1)
                failedQueueIndices.removeAll()
                currentQueueIndex = nextIndex
                recordLastPlayedIndex(index: nextIndex)
                navigationRevision += 1
                startQueueItem(at: nextIndex)
                persistState(shouldResume: true)
                return
            }
            if let currentQueueIndex, removedIndex < currentQueueIndex {
                self.currentQueueIndex = currentQueueIndex - 1
                recordLastPlayedIndex(index: self.currentQueueIndex!)
            }
            updateNowPlayingInfo()
            updateRemoteCommandAvailability()
        }
        navigationRevision += 1
        persistState(shouldResume: isPlaying || isConnecting)
    }
    
    func isSearchNavigationEnabled(named name: String) -> Bool {
        searchNavigation[normalizedName(name)] ?? true
    }
    
    func toggleSearchNavigation(named name: String) {
        let key = normalizedName(name)
        searchNavigation[key] = !(searchNavigation[key] ?? true)
        navigationRevision += 1
        persistState(shouldResume: isPlaying || isConnecting)
    }
    
    func isCategoryNavigationEnabled(categoryID: UUID) -> Bool {
        categoryNavigation[categoryID.uuidString] ?? true
    }
    
    func toggleCategoryNavigation(categoryID: UUID) {
        let key = categoryID.uuidString
        categoryNavigation[key] = !(categoryNavigation[key] ?? true)
        navigationRevision += 1
        persistState(shouldResume: isPlaying || isConnecting)
    }
    
    func isStationNavigationEnabled(stationID: UUID, inQueueNamed queueName: String?) -> Bool {
        stationNavigation[stationNavigationKey(queueName: queueName, stationID: stationID)] ?? true
    }
    
    func toggleStationNavigation(stationID: UUID, inQueueNamed queueName: String?) {
        let key = stationNavigationKey(queueName: queueName, stationID: stationID)
        stationNavigation[key] = !(stationNavigation[key] ?? true)
        navigationRevision += 1
        updateRemoteCommandAvailability()
        persistState(shouldResume: isPlaying || isConnecting)
    }
    
    func next() {
        executeThrottledAction {
            self.auditionNextStation()
        }
    }
    
    func previous() {
        executeThrottledAction {
            self.auditionPreviousStation()
        }
    }
    
    func restartCurrentQueueItem() {
        RideLogger.shared.log("RESTART_QUEUE: index=\(String(describing: currentQueueIndex)), activeQueueCount=\(activeQueue.count), currentStation=\(String(describing: currentStation?.name))")
        
        cancelAudition()
        if let index = currentQueueIndex, activeQueue.indices.contains(index) {
            startQueueItem(at: index)
        } else if let station = currentStation {
            play(station: station)
        } else {
            RideLogger.shared.log("RESTART_QUEUE: FAILED - All queue and station references are nil/empty on widget launch!")
        }
    }
    
    func pause() {
        cancelAudition()
        player?.pause()
        cancelStartupWatchdog()
        cancelStallRecovery()
        playbackState = .paused
        RideLogger.shared.log("PLAYER_PAUSE: station='\(self.currentStation?.name ?? "none")'")
        updateNowPlayingInfo()
        persistState(shouldResume: false)
    }
    
    func resume() {
        cancelAudition()
        guard let player, let station = currentStation else {
            restartCurrentQueueItem()
            return
        }
        playFeedbackSound()
        errorMessage = nil
        playbackState = .connecting
        RideLogger.shared.log("PLAYER_RESUME: station='\(station.name)'")
        player.play()
        updateNowPlayingInfo()
        persistState(shouldResume: true)
        startStartupWatchdog(station: station, generation: playbackGeneration)
    }
    
    func stop(clearQueue: Bool = false) {
        cancelAudition()
        playbackGeneration += 1
        
        // Instantly halt and destroy the player instance via stopCurrentAttempt
        stopCurrentAttempt(killPlayer: true)
        
        failedStationID = nil
        errorMessage = nil
        playbackState = .stopped
        RideLogger.shared.log("PLAYER_STOP: station='\(self.currentStation?.name ?? "none")' clearQueue=\(clearQueue)")
        updateNowPlayingInfo()
        
        if clearQueue {
            currentStation = nil
            activeQueue = []
            currentQueueIndex = nil
            activeQueueName = nil
            activeLibraryCategoryID = nil
            failedQueueIndices.removeAll()
            autoAdvanceOnFailure = true
            updateRemoteCommandAvailability()
            clearNowPlayingInfo()
        } else {
            updateRemoteCommandAvailability()
        }
        persistState(shouldResume: false)
    }
    
    func togglePlayback() {
        RideLogger.shared.log("TOGGLE_PLAYBACK: called (isPlaying=\(self.isPlaying), isConnecting=\(self.isConnecting))")
        
        // Explicitly activate the audio session to bypass Usage State 2 errors
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try session.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            RideLogger.shared.log("FAILED to activate audio session: \(error.localizedDescription)")
        }
        
        if isPlaying || isConnecting {
            stop()
        } else {
            restartCurrentQueueItem()
        }
    }
    
    func setVolume(_ volume: Float) {
        player?.volume = volume
    }
    
    private func startQueueItem(at index: Int) {
        guard activeQueue.indices.contains(index) else { return }
        stopCurrentAttempt()
        
        playFeedbackSound()
        
        playbackGeneration += 1
        let generation = playbackGeneration
        let station = activeQueue[index]
        currentQueueIndex = index
        currentStation = station
        recordLastPlayedIndex(index: index)
        updateNowPlayingInfo()
        failedStationID = nil
        errorMessage = nil
        playbackState = .resolving
        persistState(shouldResume: true)
        
        RideLogger.shared.log("PLAY_REQUEST: station='\(station.name)' category='\(self.activeQueueName ?? "none")' index=\(index)")
        
        resolutionTask = Task { [weak self] in
            guard let self else { return }
            let result = await self.resolver.resolve(station: station)
            guard !Task.isCancelled, generation == self.playbackGeneration else { return }
            
            guard result.success, let resolvedURL = result.resolvedURL, let url = URL(string: resolvedURL) else {
                RideLogger.shared.log("STATION_RESOLVE_FAILED: station='\(station.name)' error='\(result.errorMessage ?? "unknown")'")
                self.handlePlaybackFailure(station: station, message: result.errorMessage ?? "No playable stream was found.")
                return
            }
            RideLogger.shared.log("STATION_RESOLVED: station='\(station.name)' url='\(resolvedURL)'")
            self.beginPlayback(station: station, url: url, generation: generation)
        }
    }
    
    private func beginPlayback(station: Station, url: URL, generation: Int) {
        guard generation == playbackGeneration else { return }
        
        try? AVAudioSession.sharedInstance().setActive(true)
        UIApplication.shared.beginReceivingRemoteControlEvents()
        
        playbackState = .connecting
        nowPlayingTitle = nil
        nowPlayingArtist = nil
        currentResolvedURL = url
        
        let item = AVPlayerItem(url: url)
        attachMetadataObserver(to: item)
        
        // Check if player already exists; if so, swap the item instead of recreating the player
        if let existingPlayer = player {
            existingPlayer.pause()
            existingPlayer.replaceCurrentItem(with: item)
        } else {
            let newPlayer = AVPlayer(playerItem: item)
            newPlayer.allowsExternalPlayback = false
            newPlayer.automaticallyWaitsToMinimizeStalling = true
            newPlayer.isMuted = false
            newPlayer.volume = 1.0
            player = newPlayer
        }
        
        guard let activePlayer = player else { return }
        
        observePlayerItem(item: item, station: station, generation: generation)
        observePlaybackState(player: activePlayer, station: station, generation: generation)
        observeStallNotifications(item: item, station: station, generation: generation)
        
        activePlayer.play()
        startStartupWatchdog(station: station, generation: generation)
    }
    
    private func observePlayerItem(item: AVPlayerItem, station: Station, generation: Int) {
        itemStatusObservation = item.observe(\.status, options: [.initial, .new]) { [weak self] observedItem, _ in
            Task { @MainActor [weak self] in
                guard let self, generation == self.playbackGeneration else { return }
                switch observedItem.status {
                case .unknown, .readyToPlay:
                    if self.playbackState != .playing { self.playbackState = .connecting }
                case .failed:
                    self.handlePlaybackFailure(station: station, message: "Unable to play \(station.name). \(self.description(for: observedItem.error))")
                @unknown default:
                    self.handlePlaybackFailure(station: station, message: "Unknown stream status.")
                }
            }
        }
        
        bufferEmptyObservation = item.observe(\.isPlaybackBufferEmpty, options: [.new]) { [weak self] observedItem, _ in
            Task { @MainActor [weak self] in
                guard let self, generation == self.playbackGeneration else { return }
                if observedItem.isPlaybackBufferEmpty {
                    RideLogger.shared.log("BUFFER_EMPTY: station='\(station.name)'")
                    self.scheduleStallRecovery(station: station, generation: generation)
                }
            }
        }
        
        likelyToKeepUpObservation = item.observe(\.isPlaybackLikelyToKeepUp, options: [.new]) { [weak self] observedItem, _ in
            Task { @MainActor [weak self] in
                guard let self, generation == self.playbackGeneration else { return }
                if observedItem.isPlaybackLikelyToKeepUp {
                    RideLogger.shared.log("BUFFER_LIKELY_TO_KEEP_UP: station='\(station.name)'")
                    self.cancelStallRecovery()
                }
            }
        }
    }
    
    private func observeStallNotifications(item: AVPlayerItem, station: Station, generation: Int) {
        if let existing = playbackStalledObserver {
            NotificationCenter.default.removeObserver(existing)
        }
        playbackStalledObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemPlaybackStalled,
            object: item,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor [weak self] in
                guard let self, generation == self.playbackGeneration else { return }
                RideLogger.shared.log("AVPlayerItemPlaybackStalled notification for '\(station.name)'")
                self.scheduleStallRecovery(station: station, generation: generation)
            }
        }
    }
    
    private func scheduleStallRecovery(station: Station, generation: Int) {
        stallRecoveryTask?.cancel()
        stallRecoveryTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 3_500_000_000)
            guard !Task.isCancelled, generation == self.playbackGeneration else { return }
            if self.isPlaying || self.playbackState == .connecting {
                RideLogger.shared.log("STALL_WATCHDOG_TRIGGERED: station='\(station.name)'")
                self.handlePlaybackFailure(station: station, message: "Stream stalled.")
            }
        }
    }
    
    func cancelStallRecovery() {
        stallRecoveryTask?.cancel()
        stallRecoveryTask = nil
    }
    
    private func observePlaybackState(player: AVPlayer, station: Station, generation: Int) {
        timeControlObservation = player.observe(\.timeControlStatus, options: [.initial, .new]) { [weak self] observedPlayer, _ in
            Task { @MainActor [weak self] in
                guard let self, generation == self.playbackGeneration else { return }
                switch observedPlayer.timeControlStatus {
                case .playing:
                    self.cancelStartupWatchdog()
                    self.cancelStallRecovery()
                    self.startProgressCheckTask(generation: generation, station: station)
                    self.playbackState = .playing
                    self.errorMessage = nil
                    RideLogger.shared.log("STATION_PLAYING: station='\(station.name)'")
                    self.updateNowPlayingInfo()
                    self.persistState(shouldResume: true)
                case .waitingToPlayAtSpecifiedRate:
                    if self.playbackState == .playing {
                        self.scheduleStallRecovery(station: station, generation: generation)
                    } else {
                        self.playbackState = .connecting
                    }
                    self.updateNowPlayingInfo()
                case .paused:
                    if self.playbackState == .playing {
                        self.playbackState = .paused
                        self.updateNowPlayingInfo()
                    }
                @unknown default:
                    self.handlePlaybackFailure(station: station, message: "Unknown playback state.")
                }
            }
        }
    }
    
    private func startStartupWatchdog(station: Station, generation: Int) {
        cancelStartupWatchdog()
        startupTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: startupTimeoutNanoseconds)
            guard !Task.isCancelled, generation == self.playbackGeneration, self.playbackState != .playing else { return }
            RideLogger.shared.log("STARTUP_WATCHDOG_TIMEOUT: station='\(station.name)'")
            self.handlePlaybackFailure(station: station, message: "Timeout waiting for \(station.name).")
        }
    }
    
    private func handlePlaybackFailure(station: Station, message: String) {
        cancelStartupWatchdog()
        cancelStallRecovery()
        player?.pause()
        player = nil
        invalidateObservations()
        
        if let index = currentQueueIndex {
            failedQueueIndices.insert(index)
        }
        
        RideLogger.shared.log("STATION_FAILURE_AUTO_ADVANCE: station='\(station.name)' error='\(message)'")
        
        guard isNetworkAvailable else {
            playbackState = .failed
            updateNowPlayingInfo()
            failedStationID = station.id
            errorMessage = "Network lost. Waiting..."
            RideLogger.shared.log("STATION_FAILURE_HALT_OFFLINE: category='\(self.activeQueueName ?? "none")'")
            onPlaybackFailed?(station)
            return
        }
        
        if let nextIndex = nextUnfailedQueueIndex() {
            errorMessage = "Connecting next station…"
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
                self?.startQueueItem(at: nextIndex)
            }
            return
        }
        
        playbackState = .failed
        updateNowPlayingInfo()
        failedStationID = station.id
        errorMessage = isNetworkAvailable ? message : "Network lost. Waiting..."
        RideLogger.shared.log("STATION_FAILURE_HALT: category='\(self.activeQueueName ?? "none")'")
        onPlaybackFailed?(station)
    }
    
    private func startNetworkMonitoring() {
        networkMonitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                let isAvailable = (path.status == .satisfied)
                if isAvailable != self?.isNetworkAvailable {
                    RideLogger.shared.log("NETWORK_CHANGE: available=\(isAvailable)")
                }
                self?.isNetworkAvailable = isAvailable
                
                if isAvailable && self?.errorMessage == "Network lost. Waiting..." {
                    self?.errorMessage = nil
                }
            }
        }
        networkMonitor.start(queue: DispatchQueue.global(qos: .background))
    }
    
    private func nextUnfailedQueueIndex() -> Int? {
        guard !activeQueue.isEmpty else { return nil }
        let start = currentQueueIndex ?? -1
        let count = activeQueue.count
        for step in 1...count {
            let candidate = (start + step + count) % count
            let station = activeQueue[candidate]
            if !failedQueueIndices.contains(candidate), isStationNavigationEnabled(stationID: station.id, inQueueNamed: activeQueueName) { return candidate }
        }
        return nil
    }
    
    private func prefetchKey(queue: [Station], name: String?, startAt: Int, step: Int) -> String {
        let ids = queue.map { $0.id.uuidString }.joined(separator: ",")
        return "\(normalizedName(name ?? ""))|\(startAt)|\(step < 0 ? -1 : 1)|\(ids)"
    }
    
    private func startPreparedAuditionIfAvailable(requestedStartAt: Int) -> Bool {
        let key = prefetchKey(queue: auditionQueue, name: auditionQueueName, startAt: requestedStartAt, step: auditionStep)
        guard let prepared = preparedStreams.removeValue(forKey: key), auditionQueue.indices.contains(prepared.resolvedIndex) else { return false }
        auditionQueueIndex = prepared.resolvedIndex
        auditionGeneration += 1
        beginAuditionPlayback(station: prepared.station, url: prepared.url, index: prepared.resolvedIndex, generation: auditionGeneration)
        return true
    }
    
    private func startAuditionItem(at index: Int) {
        guard auditionQueue.indices.contains(index) else { finishAuditionFailure(); return }
        stopAuditionAttempt()
        
        auditionGeneration += 1
        let generation = auditionGeneration
        let station = auditionQueue[index]
        auditionResolutionTask = Task { [weak self] in
            guard let self else { return }
            let result = await resolver.resolve(station: station)
            guard !Task.isCancelled, generation == auditionGeneration else { return }
            guard result.success, let resolvedURL = result.resolvedURL, let url = URL(string: resolvedURL) else {
                handleAuditionFailure(station: station, message: result.errorMessage ?? "No playable stream was found.")
                return
            }
            beginAuditionPlayback(station: station, url: url, index: index, generation: generation)
        }
    }
    
    private func beginAuditionPlayback(station: Station, url: URL, index: Int, generation: Int) {
        guard generation == auditionGeneration else { return }
        auditionResolvedURL = url
        let item = AVPlayerItem(url: url)
        let newPlayer = AVPlayer(playerItem: item)
        newPlayer.allowsExternalPlayback = false
        newPlayer.volume = 0.0
        newPlayer.automaticallyWaitsToMinimizeStalling = false
        auditionPlayer = newPlayer
        observeAuditionPlayerItem(item: item, station: station, generation: generation)
        observeAuditionPlaybackState(player: newPlayer, station: station, index: index, generation: generation)
        newPlayer.play()
        startAuditionStartupWatchdog(station: station, generation: generation)
    }
    
    private func observeAuditionPlayerItem(item: AVPlayerItem, station: Station, generation: Int) {
        auditionItemStatusObservation = item.observe(\.status, options: [.initial, .new]) { [weak self] observedItem, _ in
            Task { @MainActor [weak self] in
                guard let self, generation == self.auditionGeneration else { return }
                if observedItem.status == .failed {
                    self.handleAuditionFailure(station: station, message: "Unable to audition \(station.name).")
                }
            }
        }
    }
    
    private func observeAuditionPlaybackState(player: AVPlayer, station: Station, index: Int, generation: Int) {
        auditionTimeControlObservation = player.observe(\.timeControlStatus, options: [.initial, .new]) { [weak self] observedPlayer, _ in
            Task { @MainActor [weak self] in
                guard let self, generation == self.auditionGeneration else { return }
                if observedPlayer.timeControlStatus == .playing {
                    self.promoteAuditionToActivePlayback(station: station, index: index, url: self.auditionResolvedURL)
                }
            }
        }
    }
    
    // MARK: - Category Navigation
    
    //    /// Fetches all available category targets (library categories + saved search queues).
    //    /// Adjust the source arrays to match your actual model names.
    //    func fetchAllCategoryTargets() -> [CategoryTarget] {
    //        // Combine your library categories and saved search queues into a single unified list
    //        let libraryTargets = libraryCategories.map { CategoryTarget.library($0) }
    //        let searchTargets = savedSearchQueues.map { CategoryTarget.savedSearch($0) }
    //        return libraryTargets + searchTargets
    //    }
    
    /// Switches to the next category target in the rotation.
    // MARK: - Category Navigation (Saved Search Queues)
    
    func switchToNextCategory() {
        guard !savedSearchQueues.isEmpty else {
            return
        }
        
        let currentIndex: Int
        if let currentName = activeQueueName,
           let found = savedSearchQueues.firstIndex(where: { $0.name.caseInsensitiveCompare(currentName) == .orderedSame }) {
            currentIndex = found
        } else {
            currentIndex = -1
        }
        
        let nextIndex = (currentIndex + 1) % savedSearchQueues.count
        let targetQueue = savedSearchQueues[nextIndex]
        
        auditionSavedSearchQueue(named: targetQueue.name)
    }
    
    func switchToPreviousCategory() {
        guard !savedSearchQueues.isEmpty else { return }
        
        let currentIndex: Int
        if let currentName = activeQueueName,
           let found = savedSearchQueues.firstIndex(where: { $0.name.caseInsensitiveCompare(currentName) == .orderedSame }) {
            currentIndex = found
        } else {
            currentIndex = 0
        }
        
        let prevIndex = (currentIndex - 1 + savedSearchQueues.count) % savedSearchQueues.count
        let targetQueue = savedSearchQueues[prevIndex]
        auditionSavedSearchQueue(named: targetQueue.name)
    }
    
    private func promoteAuditionToActivePlayback(station: Station, index: Int, url: URL?) {
        guard let newPlayer = self.auditionPlayer else { return }
        
        // --- INSTANTLY KILL OLD PLAYER AUDIO & BUFFER BLEED ---
        self.player?.volume = 0.0
        self.player?.rate = 0.0
        self.player?.pause()
        self.player?.replaceCurrentItem(with: nil)
        self.player = nil
        
        // Clean up audition tasks and observers
        self.auditionResolutionTask?.cancel()
        self.auditionResolutionTask = nil
        self.auditionStartupTask?.cancel()
        self.auditionStartupTask = nil
        self.auditionTimeControlObservation?.invalidate()
        self.auditionTimeControlObservation = nil
        self.auditionItemStatusObservation?.invalidate()
        self.auditionItemStatusObservation = nil
        self.auditionPlayer = nil
        self.auditionResolvedURL = nil
        
        playbackGeneration += 1
        
        // Configure and promote the new player to active
        try? AVAudioSession.sharedInstance().setActive(true)
        newPlayer.isMuted = false
        newPlayer.volume = 1.0
        newPlayer.automaticallyWaitsToMinimizeStalling = true
        self.player = newPlayer
        self.currentResolvedURL = url
        
        if let item = newPlayer.currentItem {
            attachMetadataObserver(to: item)
            observePlayerItem(item: item, station: station, generation: playbackGeneration)
            observeStallNotifications(item: item, station: station, generation: playbackGeneration)
        }
        observePlaybackState(player: newPlayer, station: station, generation: playbackGeneration)
        
        activeQueue = auditionQueue
        activeQueueName = auditionQueueName
        activeLibraryCategoryID = auditionLibraryCategoryID
        currentQueueIndex = index
        currentStation = station
        recordLastPlayedIndex(index: index)
        failedQueueIndices.removeAll()
        playbackState = .playing
        errorMessage = nil
        
        if auditionShouldSaveQueue, let name = auditionQueueName, !name.isEmpty {
            saveOrUpdateSearchQueue(named: name, stations: auditionQueue)
        }
        
        let completion = auditionCompletion
        self.auditionStatusMessage = nil
        self.auditionQueue = []
        self.auditionQueueName = nil
        self.auditionLibraryCategoryID = nil
        self.auditionCompletion = nil
        self.auditionFailure = nil
        
        updateNowPlayingInfo()
        updateRemoteCommandAvailability()
        persistState(shouldResume: true)
        
        onAuditionSucceeded?()
        completion?()
    }
    
    private func handleAuditionFailure(station: Station, message: String) {
        stopAuditionAttempt()
        auditionFailedIndices.insert(auditionQueueIndex)
        
        guard isNetworkAvailable else {
            finishAuditionFailure(message: "Network lost. Waiting...")
            return
        }
        
        if let nextIndex = nextUnfailedAuditionIndex() {
            auditionQueueIndex = nextIndex
            startAuditionItem(at: nextIndex)
        } else {
            finishAuditionFailure(message: message)
        }
    }
    
    private func nextUnfailedAuditionIndex() -> Int? {
        guard !auditionQueue.isEmpty else { return nil }
        let start = auditionQueueIndex
        let count = auditionQueue.count
        for step in 1...count {
            let raw = start + (auditionStep * step)
            let candidate = ((raw % count) + count) % count
            let station = auditionQueue[candidate]
            if !auditionFailedIndices.contains(candidate), isStationNavigationEnabled(stationID: station.id, inQueueNamed: auditionQueueName) { return candidate }
        }
        return nil
    }
    
    private func finishAuditionFailure(message: String = "No playable stations found.") {
        let failureCallback = auditionFailure
        cancelAudition()
        onAuditionFailed?(message)
        failureCallback?()
    }
    
    private func startAuditionStartupWatchdog(station: Station, generation: Int) {
        auditionStartupTask?.cancel()
        auditionStartupTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: auditionTimeoutNanoseconds)
            guard !Task.isCancelled, generation == self.auditionGeneration else { return }
            self.handleAuditionFailure(station: station, message: "Audition timed out.")
        }
    }
    
    private func stopAuditionAttempt() {
        auditionResolutionTask?.cancel()
        auditionResolutionTask = nil
        auditionStartupTask?.cancel()
        auditionStartupTask = nil
        auditionTimeControlObservation?.invalidate()
        auditionTimeControlObservation = nil
        auditionItemStatusObservation?.invalidate()
        auditionItemStatusObservation = nil
        
        // --- ADD THESE LINES TO FULLY KILL THE AUDITION ITEM & PLAYER ---
        auditionPlayer?.replaceCurrentItem(with: nil)
        auditionPlayer?.pause()
        auditionPlayer = nil
    }
    
    func cancelAudition() {
        stopAuditionAttempt()
        auditionGeneration += 1
        auditionQueue = []
        auditionQueueName = nil
        auditionLibraryCategoryID = nil
        auditionQueueIndex = 0
        auditionFailedIndices.removeAll()
        auditionStatusMessage = nil
        auditionCompletion = nil
        auditionFailure = nil
        auditionResolvedURL = nil
    }
    
    func setAuditionStatus(message: String?) {
        auditionStatusMessage = message
        if message != nil { errorMessage = nil }
    }
    
    func updateSavedSearchQueue(named name: String, stations: [Station]) {
        saveOrUpdateSearchQueue(named: name, stations: stations)
    }
    
    private func saveOrUpdateSearchQueue(named name: String, stations: [Station]) {
        let key = normalizedName(name)
        if let existingIndex = savedSearchQueues.firstIndex(where: { normalizedName($0.name) == key }) {
            let previousCount = savedSearchQueues[existingIndex].stations.count
            if stations.count < (previousCount / 2) && previousCount > 10 { return }
            savedSearchQueues[existingIndex] = SavedSearchQueue(name: savedSearchQueues[existingIndex].name, stations: stations)
        } else {
            savedSearchQueues.append(SavedSearchQueue(name: name, stations: stations))
        }
    }
    
    private func startProgressCheckTask(generation: Int, station: Station) {
        progressCheckTask?.cancel()
        lastObservedPosition = -1
        positionStallCount = 0
        
        progressCheckTask = Task { @MainActor in
            while !Task.isCancelled && generation == self.playbackGeneration {
                try? await Task.sleep(nanoseconds: 1_500_000_000)
                guard !Task.isCancelled, generation == self.playbackGeneration, let player = self.player, self.isPlaying else { continue }
                
                let currentPosition = player.currentTime().seconds
                if currentPosition == lastObservedPosition && currentPosition > 0 {
                    positionStallCount += 1
                } else {
                    positionStallCount = 0
                }
                lastObservedPosition = currentPosition
                
                if positionStallCount >= 3 {
                    RideLogger.shared.log("AUDIO_STALL: detected for '\(station.name)'")
                    self.handlePlaybackFailure(station: station, message: "Stream stalled.")
                    break
                }
            }
        }
    }
    
    private func stopCurrentAttempt(killPlayer: Bool = true) {
        resolutionTask?.cancel()
        resolutionTask = nil
        cancelStartupWatchdog()
        cancelStallRecovery()
        progressCheckTask?.cancel()
        progressCheckTask = nil
        invalidateObservations()
        
        if killPlayer {
            player?.volume = 0.0
            player?.rate = 0.0
            player?.pause()
            player?.replaceCurrentItem(with: nil)
            player = nil
        }
        
        // ALWAYS instantly silence and kill the audition player too!
        auditionPlayer?.volume = 0.0
        auditionPlayer?.rate = 0.0
        auditionPlayer?.pause()
        auditionPlayer?.replaceCurrentItem(with: nil)
        auditionPlayer = nil
        
        currentResolvedURL = nil
    }
    
    func cancelStartupWatchdog() {
        startupTask?.cancel()
        startupTask = nil
    }
    
    private func invalidateObservations() {
        timeControlObservation?.invalidate()
        timeControlObservation = nil
        itemStatusObservation?.invalidate()
        itemStatusObservation = nil
        bufferEmptyObservation?.invalidate()
        bufferEmptyObservation = nil
        likelyToKeepUpObservation?.invalidate()
        likelyToKeepUpObservation = nil
        if let observer = playbackStalledObserver {
            NotificationCenter.default.removeObserver(observer)
            playbackStalledObserver = nil
        }
    }
    
    private func normalizedName(_ name: String) -> String {
        name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }
    
    private func stationNavigationKey(queueName: String?, stationID: UUID) -> String {
        let queueKey = normalizedName(queueName ?? "default")
        return "search:\(queueKey)|\(stationID.uuidString)"
    }
    
    private func attachMetadataObserver(to item: AVPlayerItem) {
        let output = AVPlayerItemMetadataOutput(identifiers: nil)
        let delegate = StreamMetadataDelegate()
        delegate.onItems = { [weak self] items in
            Task { @MainActor [weak self] in self?.handleMetadataItems(items) }
        }
        output.setDelegate(delegate, queue: .main)
        item.add(output)
        self.metadataOutput = output
        self.metadataDelegate = delegate
    }
    
    private func handleMetadataItems(_ items: [AVMetadataItem]) {
        for item in items {
            guard let value = item.stringValue else { continue }
            if item.commonKey == .commonKeyTitle { nowPlayingTitle = value }
            else if item.commonKey == .commonKeyArtist { nowPlayingArtist = value }
        }
        updateNowPlayingInfo()
    }
    
    private func configureAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default, options: [])
            try session.setActive(true)
        } catch {
            print("Failed to configure audio session: \(error)")
        }
    }
    
    private func observeAudioRouteChanges() {
        audioRouteChangeObserver = NotificationCenter.default.addObserver(
            forName: AVAudioSession.routeChangeNotification,
            object: nil,
            queue: .main
        ){
            [weak self] notification in
            guard let self = self,
                  let userInfo = notification.userInfo,
                  let reasonValue = userInfo[AVAudioSessionRouteChangeReasonKey] as? UInt,
                  let reason = AVAudioSession.RouteChangeReason(rawValue: reasonValue) else { return }
            
            // Your switch statement continues here...
            // Since queue is .main, we are already on the main thread
            switch reason {
            case .oldDeviceUnavailable:
                RideLogger.shared.log("AUDIO_ROUTE_CHANGE: Earbuds disconnected. Executing full stop.")
                self.stop(clearQueue: false)
                
            case .newDeviceAvailable:
                RideLogger.shared.log("AUDIO_ROUTE_CHANGE: New device connected.")
                do {
                    try AVAudioSession.sharedInstance().setActive(true)
                } catch {
                    RideLogger.shared.log("AUDIO_ROUTE_CHANGE: Failed to activate session: \(error.localizedDescription)")
                }
                
            default:
                RideLogger.shared.log("AUDIO_ROUTE_CHANGE: Reason rawValue=\(reasonValue)")
                break
            }
        }
    }
    
    private func observeAudioInterruptions() {
        audioInterruptionObserver = NotificationCenter.default.addObserver(
            forName: AVAudioSession.interruptionNotification,
            object: AVAudioSession.sharedInstance(),
            queue: .main
        ) { [weak self] notification in
            guard let self,
                  let userInfo = notification.userInfo,
                  let typeValue = userInfo[AVAudioSessionInterruptionTypeKey] as? UInt,
                  let type = AVAudioSession.InterruptionType(rawValue: typeValue) else { return }
            
            Task { @MainActor in
                switch type {
                case .began:
                    self.wasPlayingBeforeInterruption = self.isPlaying || self.isConnecting
                    RideLogger.shared.log("AUDIO_INTERRUPTION: Began (wasPlaying=\(self.wasPlayingBeforeInterruption))")
                    
                case .ended:
                    RideLogger.shared.log("AUDIO_INTERRUPTION: Ended")
                    guard self.wasPlayingBeforeInterruption else { return }
                    self.wasPlayingBeforeInterruption = false
                    
                    let optionsValue = userInfo[AVAudioSessionInterruptionOptionKey] as? UInt ?? 0
                    let options = AVAudioSession.InterruptionOptions(rawValue: optionsValue)
                    
                    if options.contains(.shouldResume) {
                        try? AVAudioSession.sharedInstance().setActive(true)
                        self.resume()
                    }
                    
                @unknown default:
                    break
                }
            }
        }
    }
    
    private func configureRemoteCommands() {
        let commandCenter = MPRemoteCommandCenter.shared()
        
        commandCenter.seekForwardCommand.isEnabled = false
        commandCenter.seekBackwardCommand.isEnabled = false
        commandCenter.changePlaybackPositionCommand.isEnabled = false
        
        commandCenter.playCommand.removeTarget(nil)
        commandCenter.playCommand.isEnabled = true
        commandCenter.playCommand.addTarget { [weak self] _ in
            RideLogger.shared.log("REMOTE_COMMAND: playCommand received")
            Task { @MainActor [weak self] in
                self?.resume()
            }
            return .success
        }
        
        commandCenter.pauseCommand.removeTarget(nil)
        commandCenter.pauseCommand.isEnabled = true
        commandCenter.pauseCommand.addTarget { [weak self] _ in
            RideLogger.shared.log("REMOTE_COMMAND: pauseCommand received")
            Task { @MainActor [weak self] in
                self?.pause()
            }
            return .success
        }
        
        commandCenter.togglePlayPauseCommand.removeTarget(nil)
        commandCenter.togglePlayPauseCommand.isEnabled = true
        commandCenter.togglePlayPauseCommand.addTarget { [weak self] _ in
            RideLogger.shared.log("REMOTE_COMMAND: togglePlayPauseCommand received")
            Task { @MainActor [weak self] in
                self?.togglePlayback()
            }
            return .success
        }
        
        commandCenter.nextTrackCommand.removeTarget(nil)
        commandCenter.nextTrackCommand.addTarget { [weak self] _ in
            RideLogger.shared.log("REMOTE_COMMAND: nextTrackCommand received")
            Task { @MainActor [weak self] in
                if let onNext = self?.onNextTrackCommand {
                    onNext()
                } else {
                    self?.next()
                }
            }
            return .success
        }
        
        commandCenter.previousTrackCommand.removeTarget(nil)
        commandCenter.previousTrackCommand.addTarget { [weak self] _ in
            RideLogger.shared.log("REMOTE_COMMAND: previousTrackCommand received")
            Task { @MainActor [weak self] in
                if let onPrevious = self?.onPreviousTrackCommand {
                    onPrevious()
                } else {
                    self?.previous()
                }
            }
            return .success
        }
    }
    
    private func updateRemoteCommandAvailability() {
        let commandCenter = MPRemoteCommandCenter.shared()
        let hasQueue = !activeQueue.isEmpty
        
        commandCenter.nextTrackCommand.isEnabled = hasQueue
        commandCenter.previousTrackCommand.isEnabled = hasQueue
        commandCenter.playCommand.isEnabled = true
        commandCenter.pauseCommand.isEnabled = true
        commandCenter.togglePlayPauseCommand.isEnabled = true
    }
    
    private func updateNowPlayingInfo() {
        var info = [String: Any]()
        if let station = currentStation {
            info[MPMediaItemPropertyTitle] = nowPlayingTitle ?? station.name
            info[MPMediaItemPropertyArtist] = nowPlayingArtist ?? activeQueueName ?? "Live Stream"
        }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
        syncWidgetData()
    }
    
    private func clearNowPlayingInfo() { MPNowPlayingInfoCenter.default().nowPlayingInfo = nil }
    
    private func persistState(shouldResume: Bool) {
        guard !isRestoringPersistedState else { return }
        let state = PersistedPlaybackState(
            savedSearchQueues: savedSearchQueues,
            activeQueue: activeQueue,
            activeQueueName: activeQueueName,
            currentQueueIndex: currentQueueIndex,
            shouldResume: shouldResume,
            searchNavigation: searchNavigation,
            categoryNavigation: categoryNavigation,
            stationNavigation: stationNavigation,
            lastPlayedIndices: lastPlayedIndices
        )
        
        // Use the shared App Group suite so the widget can access it on cold launch
        let sharedDefaults = UserDefaults(suiteName: "group.com.coppersmith.music1chat")
        if let data = try? JSONEncoder().encode(state) {
            sharedDefaults?.set(data, forKey: persistenceKey)
        }
    }
    
    private func restorePersistedState() {
        isRestoringPersistedState = true
        defer { isRestoringPersistedState = false }
        
        let sharedDefaults = UserDefaults(suiteName: "group.com.coppersmith.music1chat")
        
        // 1. Try loading from the shared App Group first
        var data = sharedDefaults?.data(forKey: persistenceKey)
        
        // 2. Fallback & Migrate: If not in App Group yet, grab it from UserDefaults.standard and move it over!
        if data == nil, let standardData = UserDefaults.standard.data(forKey: persistenceKey) {
            data = standardData
            sharedDefaults?.set(standardData, forKey: persistenceKey)
            RideLogger.shared.log("RESTORE_STATE: Migrated state from standard defaults to App Group")
        }
        
        guard let data = data else {
            RideLogger.shared.log("RESTORE_STATE: No data found anywhere for key \(persistenceKey)")
            return
        }
        
        guard let state = try? JSONDecoder().decode(PersistedPlaybackState.self, from: data) else {
            RideLogger.shared.log("RESTORE_STATE: Failed to decode PersistedPlaybackState")
            return
        }
        
        RideLogger.shared.log("RESTORE_STATE: Successfully restored queue with items count: \(state.activeQueue.count), index: \(String(describing: state.currentQueueIndex))")
        
        savedSearchQueues = state.savedSearchQueues
        activeQueue = state.activeQueue
        activeQueueName = state.activeQueueName
        currentQueueIndex = state.currentQueueIndex
        searchNavigation = state.searchNavigation ?? [:]
        categoryNavigation = state.categoryNavigation ?? [:]
        stationNavigation = state.stationNavigation ?? [:]
        lastPlayedIndices = state.lastPlayedIndices ?? [:]
        
        if let index = currentQueueIndex, activeQueue.indices.contains(index) {
            currentStation = activeQueue[index]
            
            // Only auto-resume if there is NO pending widget action waiting.
            // If a widget action exists, let handlePendingWidgetAction() take care of starting it!
            let sharedDefaults = UserDefaults(suiteName: "group.com.coppersmith.music1chat")
            let hasWidgetAction = sharedDefaults?.string(forKey: "pendingWidgetAction") != nil
            
            if state.shouldResume && !hasWidgetAction {
                self.startQueueItem(at: index)
            }
        }
    }
    
    func terminateSession() {
        stop(clearQueue: false)
    }
    
    private func description(for error: Error?) -> String { error?.localizedDescription ?? "An unknown error occurred." }
    
    func syncWidgetData() {
            let appGroupIdentifier = "group.com.coppersmith.music1chat"
            guard let sharedDefaults = UserDefaults(suiteName: appGroupIdentifier) else { return }
            
            let category = activeQueueName ?? (currentCategory.isEmpty ? "No Hands Radio" : currentCategory)
            let stationName = currentStation?.name ?? "Live Station"
            
            let song: String
            if let title = nowPlayingTitle, !title.isEmpty {
                song = title
            } else if !currentTrackTitle.isEmpty {
                song = currentTrackTitle
            } else {
                song = isPlaying ? "Live Radio" : "Ready to Play"
            }
            
            let isPlayingState = (playbackState == .playing)
            
            // Write to the keys the widget actually listens for
            sharedDefaults.set(category, forKey: "current_category_name")
            sharedDefaults.set(stationName, forKey: "current_station_name")
            sharedDefaults.set(song, forKey: "current_song_name")
            sharedDefaults.set(isPlayingState, forKey: "isPlaying")
            sharedDefaults.set(Date(), forKey: "last_updated")
            
            WidgetCenter.shared.reloadAllTimelines()
        }
        
        // MARK: - Widget Action Handler
        func handlePendingWidgetAction() {
            let sharedDefaults = UserDefaults(suiteName: "group.com.coppersmith.music1chat")
            if let action = sharedDefaults?.string(forKey: "pendingWidgetAction") {
                RideLogger.shared.log("DEBUG: Checking shared defaults for pending action... Found: Optional(\"\(action)\")")
                sharedDefaults?.removeObject(forKey: "pendingWidgetAction")
                
                switch action {
                case "playPause":
                    if !isPlaying && !isConnecting {
                        restartCurrentQueueItem()
                    } else {
                        stop()
                    }
                case "nextStation":
                    auditionNextStation()
                case "previousStation":
                    auditionPreviousStation()
                case "nextCategory":
                    switchToNextCategory()
                case "previousCategory":
                    switchToPreviousCategory()
                default:
                    RideLogger.shared.log("DEBUG: Unknown widget action received: \(action)")
                    break
                }
            } else {
                RideLogger.shared.log("DEBUG: Checking shared defaults for pending action... Found: nil")
            }
        }
    }
