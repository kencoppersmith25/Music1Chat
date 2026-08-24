import AVFoundation
import Combine
import Foundation
import MediaPlayer
import Network

private final class StreamMetadataDelegate: NSObject, AVPlayerItemMetadataOutputPushDelegate {
    var onItems: (([AVMetadataItem]) -> Void)?

    func metadataOutput(
        _ output: AVPlayerItemMetadataOutput,
        didOutputTimedMetadataGroups groups: [AVTimedMetadataGroup],
        from track: AVPlayerItemTrack?
    ) {
        let items = groups.flatMap(\.items)
        guard !items.isEmpty else { return }
        onItems?(items)
    }
}

@MainActor
final class AudioPlayerService: ObservableObject {
    enum PlaybackState {
        case stopped
        case resolving
        case connecting
        case playing
        case paused
        case failed
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
    }

    @Published private(set) var currentStation: Station?
    @Published private(set) var playbackState: PlaybackState = .stopped
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

    var onPlaybackFailed: ((Station) -> Void)?
    var onAuditionSucceeded: (() -> Void)?
    var onAuditionFailed: ((String) -> Void)?
    var onPreviousTrackCommand: (() -> Void)? {
        didSet { updateRemoteCommandAvailability() }
    }
    var onNextTrackCommand: (() -> Void)? {
        didSet { updateRemoteCommandAvailability() }
    }

    private var player: AVPlayer?
    private var timeControlObservation: NSKeyValueObservation?
    private var itemStatusObservation: NSKeyValueObservation?
    private var startupTask: Task<Void, Never>?
    private var resolutionTask: Task<Void, Never>?
    private var metadataOutput: AVPlayerItemMetadataOutput?
    private var metadataDelegate: StreamMetadataDelegate?
    private var audioRouteChangeObserver: NSObjectProtocol?
    private var currentResolvedURL: URL?
    private var auditionResolvedURL: URL?

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

    private var preparedStreams: [String: PreparedStream] = [:]
    private var prefetchTasks: [String: Task<Void, Never>] = [:]

    private var playbackGeneration = 0
    private var autoAdvanceOnFailure = false
    private var failedQueueIndices = Set<Int>()

    private var auditionPlayer: AVPlayer?
    private var auditionTimeControlObservation: NSKeyValueObservation?
    private var auditionItemStatusObservation: NSKeyValueObservation?
    private var auditionStartupTask: Task<Void, Never>?
    private var auditionCompletion: (() -> Void)?
    private var auditionFailure: (() -> Void)?
    private var auditionResolutionTask: Task<Void, Never>?
    private var auditionGeneration = 0
    private var auditionQueue: [Station] = []
    private var auditionQueueName: String?
    private var auditionLibraryCategoryID: UUID?
    private var auditionQueueIndex = 0
    private var auditionFailedIndices = Set<Int>()
    private var auditionStep = 1
    private var auditionShouldSaveQueue = true

    private let resolver = StreamResolverService()
    private let persistenceKey = "Music1Chat.PlaybackState.v1"
    private var isRestoringPersistedState = false

    private let networkMonitor = NWPathMonitor()
    private var isNetworkAvailable = true

    private let startupTimeoutNanoseconds: UInt64 = 8_000_000_000 // Relaxed to 8s
    private let auditionTimeoutNanoseconds: UInt64 = 6_000_000_000

    private var searchNavigation: [String: Bool] = [:]
    private var categoryNavigation: [String: Bool] = [:]
    private var stationNavigation: [String: Bool] = [:]

    init() {
        // Setup session IMMEDIATELY without delay
        configureAudioSession()
        startNetworkMonitoring()

        Task {
            observeAudioRouteChanges()
            configureRemoteCommands()
            restorePersistedState()
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

    func play(station: Station) {
        cancelAudition()
        if let index = activeQueue.firstIndex(where: { $0.id == station.id }) {
            startQueueItem(at: index)
        } else {
            play(
                queue: [station],
                name: activeQueueName,
                startAt: 0,
                autoAdvanceOnFailure: false
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
        failedQueueIndices.removeAll()
        self.autoAdvanceOnFailure = autoAdvanceOnFailure
        updateRemoteCommandAvailability()
        persistState(shouldResume: true)
        startQueueItem(at: safeIndex)
    }

    func prefetch(
        queue: [Station],
        name: String? = nil,
        startAt index: Int = 0,
        step: Int = 1
    ) {
        guard !queue.isEmpty else { return }
        let direction = step < 0 ? -1 : 1
        let safeStart = min(max(index, 0), queue.count - 1)
        let key = prefetchKey(queue: queue, name: name, startAt: safeStart, step: direction)
        guard preparedStreams[key] == nil, prefetchTasks[key] == nil else { return }

        let task = Task { @MainActor [weak self] in
            guard let self else { return }
            defer { self.prefetchTasks[key] = nil }

            for attempt in 0..<queue.count {
                guard !Task.isCancelled else { return }
                let raw = safeStart + direction * attempt
                let candidateIndex = ((raw % queue.count) + queue.count) % queue.count
                let station = queue[candidateIndex]

                guard self.isStationNavigationEnabled(stationID: station.id, inQueueNamed: name) else { continue }
                let result = await self.resolver.resolve(station: station)
                guard !Task.isCancelled else { return }

                if result.success, let resolvedURL = result.resolvedURL, let url = URL(string: resolvedURL) {
                    self.preparedStreams[key] = PreparedStream(
                        key: key, queueName: name, queue: queue, requestedStartAt: safeStart,
                        step: direction, resolvedIndex: candidateIndex, station: station, url: url
                    )
                    if self.preparedStreams.count > 6 {
                        self.preparedStreams.removeValue(forKey: self.preparedStreams.keys.first(where: { $0 != key }) ?? key)
                    }
                    return
                }
            }
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

        cancelAudition()
        auditionQueue = activeQueue
        auditionQueueName = activeQueueName
        auditionLibraryCategoryID = activeLibraryCategoryID
        auditionQueueIndex = target
        auditionFailedIndices = [current]
        auditionStep = direction
        auditionShouldSaveQueue = false
        auditionStatusMessage = direction > 0 ? "Finding next station…" : "Finding previous station…"
        auditionGeneration += 1
        if !startPreparedAuditionIfAvailable(requestedStartAt: target) {
            startAuditionItem(at: target)
        }
    }

    func auditionSavedSearchQueue(named name: String) {
        guard let saved = savedSearchQueues.first(where: { $0.name.caseInsensitiveCompare(name) == .orderedSame }) else { return }
        audition(queue: saved.stations, name: saved.name, startAt: 0, step: 1, saveAsSearch: false, statusMessage: "Finding next category…")
    }

    func cancelQueueAudition() {
        cancelAudition()
    }

    func playSavedSearchQueue(named name: String) {
        guard let saved = savedSearchQueues.first(where: { $0.name.caseInsensitiveCompare(name) == .orderedSame }) else { return }
        let startIndex: Int
        if activeQueueName?.caseInsensitiveCompare(name) == .orderedSame, let currentQueueIndex {
            startIndex = min(currentQueueIndex, max(saved.stations.count - 1, 0))
        } else {
            startIndex = 0
        }
        play(queue: saved.stations, name: saved.name, startAt: startIndex, autoAdvanceOnFailure: true)
    }

    func removeSavedSearchQueue(named name: String) {
        let key = normalizedName(name)
        let removingActiveQueue = normalizedName(activeQueueName ?? "") == key
        savedSearchQueues.removeAll { normalizedName($0.name) == key }
        if removingActiveQueue { activeQueueName = nil }
        searchNavigation.removeValue(forKey: key)
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
            startQueueItem(at: nextIndex)
        } else if let currentIndex = currentQueueIndex {
            if removedIndex < currentIndex {
                currentQueueIndex = currentIndex - 1
            } else if removedIndex == currentIndex {
                currentQueueIndex = min(currentIndex, activeQueue.count - 1)
            }
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
                navigationRevision += 1
                startQueueItem(at: nextIndex)
                persistState(shouldResume: true)
                return
            }
            if let currentQueueIndex, removedIndex < currentQueueIndex {
                self.currentQueueIndex = currentQueueIndex - 1
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

    func next() { auditionNextStation() }
    func previous() { auditionPreviousStation() }

    func restartCurrentQueueItem() {
        cancelAudition()
        if let index = currentQueueIndex, activeQueue.indices.contains(index) {
            startQueueItem(at: index)
        } else if let station = currentStation {
            play(station: station)
        }
    }

    func pause() {
        cancelAudition()
        player?.pause()
        cancelStartupWatchdog()
        playbackState = .paused
        updateNowPlayingInfo()
        persistState(shouldResume: false)
    }

    func resume() {
        cancelAudition()
        guard let player, let station = currentStation else {
            restartCurrentQueueItem()
            return
        }
        errorMessage = nil
        playbackState = .connecting
        player.play()
        updateNowPlayingInfo()
        persistState(shouldResume: true)
        startStartupWatchdog(station: station, generation: playbackGeneration)
    }

    func stop(clearQueue: Bool = false) {
        cancelAudition()
        playbackGeneration += 1
        stopCurrentAttempt()
        failedStationID = nil
        errorMessage = nil
        playbackState = .stopped
        updateNowPlayingInfo()
        if clearQueue {
            currentStation = nil
            activeQueue = []
            currentQueueIndex = nil
            activeQueueName = nil
            activeLibraryCategoryID = nil
            failedQueueIndices.removeAll()
            autoAdvanceOnFailure = false
            updateRemoteCommandAvailability()
            clearNowPlayingInfo()
        } else {
            updateRemoteCommandAvailability()
        }
        persistState(shouldResume: false)
    }

    func togglePlayback() {
        if isPlaying || isConnecting { stop() } else { restartCurrentQueueItem() }
    }

    func setVolume(_ volume: Float) {
        player?.volume = volume
    }

    private func startQueueItem(at index: Int) {
        guard activeQueue.indices.contains(index) else { return }
        stopCurrentAttempt()
        playbackGeneration += 1
        let generation = playbackGeneration
        let station = activeQueue[index]
        currentQueueIndex = index
        currentStation = station
        updateNowPlayingInfo()
        failedStationID = nil
        errorMessage = nil
        playbackState = .resolving
        persistState(shouldResume: true)

        resolutionTask = Task { [weak self] in
            guard let self else { return }
            let result = await resolver.resolve(station: station)
            guard !Task.isCancelled, generation == playbackGeneration else { return }

            guard result.success, let resolvedURL = result.resolvedURL, let url = URL(string: resolvedURL) else {
                handlePlaybackFailure(station: station, message: result.errorMessage ?? "No playable stream was found.")
                return
            }
            beginPlayback(station: station, url: url, generation: generation)
        }
    }

    private func beginPlayback(station: Station, url: URL, generation: Int) {
        guard generation == playbackGeneration else { return }

        // Ensure session is ACTIVE before creating player
        try? AVAudioSession.sharedInstance().setActive(true)

        playbackState = .connecting
        nowPlayingTitle = nil
        nowPlayingArtist = nil
        currentResolvedURL = url
        let item = AVPlayerItem(url: url)
        attachMetadataObserver(to: item)
        let newPlayer = AVPlayer(playerItem: item)
        newPlayer.allowsExternalPlayback = true
        newPlayer.automaticallyWaitsToMinimizeStalling = true
        newPlayer.volume = 1.0
        player = newPlayer
        observePlayerItem(item: item, station: station, generation: generation)
        observePlaybackState(player: newPlayer, station: station, generation: generation)
        newPlayer.play()
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
    }

    private func observePlaybackState(player: AVPlayer, station: Station, generation: Int) {
        timeControlObservation = player.observe(\.timeControlStatus, options: [.initial, .new]) { [weak self] observedPlayer, _ in
            Task { @MainActor [weak self] in
                guard let self, generation == self.playbackGeneration else { return }
                switch observedPlayer.timeControlStatus {
                case .playing:
                    self.cancelStartupWatchdog()
                    self.playbackState = .playing
                    self.errorMessage = nil
                    self.updateNowPlayingInfo()
                    self.persistState(shouldResume: true)
                case .waitingToPlayAtSpecifiedRate:
                    self.playbackState = .connecting
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
            self.handlePlaybackFailure(station: station, message: "Timeout waiting for \(station.name).")
        }
    }

    private func handlePlaybackFailure(station: Station, message: String) {
        cancelStartupWatchdog()
        player?.pause()
        player = nil
        invalidateObservations()

        // THE LIE DETECTOR: If we have internet bars, don't just sit there.
        if isNetworkAvailable && autoAdvanceOnFailure {
            errorMessage = "Station unavailable. Finding next..."
            if let index = currentQueueIndex { failedQueueIndices.insert(index) }
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
                if let nextIndex = self?.nextUnfailedQueueIndex() {
                    self?.startQueueItem(at: nextIndex)
                }
            }
            return
        }

        if let index = currentQueueIndex { failedQueueIndices.insert(index) }
        if autoAdvanceOnFailure, let nextIndex = nextUnfailedQueueIndex() {
            startQueueItem(at: nextIndex)
            return
        }
        playbackState = .failed
        updateNowPlayingInfo()
        failedStationID = station.id
        errorMessage = isNetworkAvailable ? message : "Network lost. Waiting..."
        onPlaybackFailed?(station)
    }

    private func startNetworkMonitoring() {
        networkMonitor.pathUpdateHandler = { [weak self] path in
            Task { @MainActor in
                self?.isNetworkAvailable = path.status == .satisfied
            }
        }
        networkMonitor.start(queue: DispatchQueue.global())
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
        newPlayer.volume = 0.0
        newPlayer.automaticallyWaitsToMinimizeStalling = true
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

    private func promoteAuditionToActivePlayback(station: Station, index: Int, url: URL?) {
        guard let newPlayer = self.auditionPlayer else { return }
        self.player?.pause()
        self.stopCurrentAttempt(killPlayer: true)
        playbackGeneration += 1
        newPlayer.volume = 1.0
        self.player = newPlayer
        self.currentResolvedURL = url
        if let item = newPlayer.currentItem {
            attachMetadataObserver(to: item)
            observePlayerItem(item: item, station: station, generation: playbackGeneration)
        }
        observePlaybackState(player: newPlayer, station: station, generation: playbackGeneration)
        activeQueue = auditionQueue
        activeQueueName = auditionQueueName
        activeLibraryCategoryID = auditionLibraryCategoryID
        currentQueueIndex = index
        currentStation = station
        failedQueueIndices.removeAll()
        playbackState = .playing
        errorMessage = nil
        if auditionShouldSaveQueue, let name = auditionQueueName, !name.isEmpty { saveOrUpdateSearchQueue(named: name, stations: auditionQueue) }
        let completion = auditionCompletion
        self.auditionResolutionTask?.cancel()
        self.auditionResolutionTask = nil
        self.auditionStartupTask?.cancel()
        self.auditionStartupTask = nil
        self.auditionTimeControlObservation?.invalidate()
        self.auditionTimeControlObservation = nil
        self.auditionItemStatusObservation?.invalidate()
        self.auditionItemStatusObservation = nil
        self.auditionPlayer = nil
        self.auditionGeneration += 1
        self.auditionQueue = []
        self.auditionQueueName = nil
        self.auditionLibraryCategoryID = nil
        self.auditionStatusMessage = nil
        self.auditionResolvedURL = nil
        updateNowPlayingInfo()
        updateRemoteCommandAvailability()
        persistState(shouldResume: true)
        onAuditionSucceeded?()
        completion?()
    }

    private func handleAuditionFailure(station: Station, message: String) {
        stopAuditionAttempt()
        auditionFailedIndices.insert(auditionQueueIndex)
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

    private func stopCurrentAttempt(killPlayer: Bool = true) {
        resolutionTask?.cancel()
        resolutionTask = nil
        cancelStartupWatchdog()
        invalidateObservations()
        if killPlayer {
            player?.pause()
            player = nil
        }
        currentResolvedURL = nil
    }

    private func cancelStartupWatchdog() {
        startupTask?.cancel()
        startupTask = nil
    }

    private func invalidateObservations() {
        timeControlObservation?.invalidate()
        timeControlObservation = nil
        itemStatusObservation?.invalidate()
        itemStatusObservation = nil
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
            // CLASSIC SETUP: No overrides, let iOS manage the routes
            try session.setCategory(.playback, mode: .default, options: [.allowAirPlay, .allowBluetooth])
            try session.setActive(true)
        } catch {
            print("Failed to configure AVAudioSession: \(error)")
        }
    }

    private func observeAudioRouteChanges() {
        audioRouteChangeObserver = NotificationCenter.default.addObserver(
            forName: AVAudioSession.routeChangeNotification, object: nil, queue: .main
        ) { [weak self] notification in
            guard let self, let userInfo = notification.userInfo,
                  let reasonValue = userInfo[AVAudioSessionRouteChangeReasonKey] as? UInt,
                  let reason = AVAudioSession.RouteChangeReason(rawValue: reasonValue) else { return }
            Task { @MainActor in
                switch reason {
                case .newDeviceAvailable, .routeConfigurationChange, .oldDeviceUnavailable:
                    // FORCE RESUME: If we were playing, we MUST stay playing on the new route
                    if self.isPlaying || self.playbackState == .paused {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
                            self.resume()
                        }
                    }
                default:
                    break
                }
            }
        }
    }

    private func configureRemoteCommands() {
        let commandCenter = MPRemoteCommandCenter.shared()
        commandCenter.playCommand.addTarget { [weak self] _ in Task { @MainActor in self?.resume() }; return .success }
        commandCenter.pauseCommand.addTarget { [weak self] _ in Task { @MainActor in self?.pause() }; return .success }
        commandCenter.togglePlayPauseCommand.addTarget { [weak self] _ in Task { @MainActor in self?.togglePlayback() }; return .success }
        commandCenter.nextTrackCommand.addTarget { [weak self] _ in
            Task { @MainActor in if let onNext = self?.onNextTrackCommand { onNext() } else { self?.next() } }
            return .success
        }
        commandCenter.previousTrackCommand.addTarget { [weak self] _ in
            Task { @MainActor in if let onPrevious = self?.onPreviousTrackCommand { onPrevious() } else { self?.previous() } }
            return .success
        }
    }

    private func updateRemoteCommandAvailability() {
        let commandCenter = MPRemoteCommandCenter.shared()
        commandCenter.nextTrackCommand.isEnabled = !activeQueue.isEmpty
        commandCenter.previousTrackCommand.isEnabled = !activeQueue.isEmpty
    }

    private func updateNowPlayingInfo() {
        var info = [String: Any]()
        if let station = currentStation {
            info[MPMediaItemPropertyTitle] = nowPlayingTitle ?? station.name
            info[MPMediaItemPropertyArtist] = nowPlayingArtist ?? activeQueueName ?? "Live Stream"
        }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    private func clearNowPlayingInfo() { MPNowPlayingInfoCenter.default().nowPlayingInfo = nil }

    private func persistState(shouldResume: Bool) {
        guard !isRestoringPersistedState else { return }
        let state = PersistedPlaybackState(
            savedSearchQueues: savedSearchQueues, activeQueue: activeQueue, activeQueueName: activeQueueName,
            currentQueueIndex: currentQueueIndex, shouldResume: shouldResume,
            searchNavigation: searchNavigation, categoryNavigation: categoryNavigation, stationNavigation: stationNavigation
        )
        if let data = try? JSONEncoder().encode(state) { UserDefaults.standard.set(data, forKey: persistenceKey) }
    }

    private func restorePersistedState() {
        isRestoringPersistedState = true
        defer { isRestoringPersistedState = false }
        guard let data = UserDefaults.standard.data(forKey: persistenceKey),
              let state = try? JSONDecoder().decode(PersistedPlaybackState.self, from: data) else { return }

        savedSearchQueues = state.savedSearchQueues
        activeQueue = state.activeQueue
        activeQueueName = state.activeQueueName
        currentQueueIndex = state.currentQueueIndex
        searchNavigation = state.searchNavigation ?? [:]
        categoryNavigation = state.categoryNavigation ?? [:]
        stationNavigation = state.stationNavigation ?? [:]

        if let index = currentQueueIndex, activeQueue.indices.contains(index) {
            currentStation = activeQueue[index]
            if state.shouldResume {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) { self.startQueueItem(at: index) }
            }
        }
    }

    func terminateSession() {
        // Stop playing but don't explicitly deactivate the hardware port
        // as that is what caused the silence in the last test.
        stop(clearQueue: false)
    }

    private func description(for error: Error?) -> String { error?.localizedDescription ?? "An unknown error occurred." }
}
