// Music1Chat V9.8 TESTFLIGHT PREP — 2026-08-08
// Keeps the working AdMob TEST banner and separates the approved header artwork
// so the no-hands symbol and radio have a little more breathing room.
import SwiftUI
import AVKit
import GoogleMobileAds

struct MainScreen: View {
    @EnvironmentObject private var player: AudioPlayerService
    @StateObject private var library = MusicLibraryViewModel()
    @StateObject private var settings = Music1ChatSettings()
    @StateObject private var categorySpeaker = CategoryAnnouncementSpeaker()
    @State private var selectedCategoryIndex = 0
    @State private var selectedStationIndex = 0
    @State private var pendingDeleteSearchName: String?
    @State private var stationListSearchName: String?
    @State private var stationListCategoryID: UUID?
    @State private var showCategoryList = false
    @State private var showSaveStationDialog = false
    @State private var saveCategoryName = ""
    @State private var searchText = ""
    @State private var showSearchOverlay = false
    @State private var showSettingsOverlay = false
    @State private var lastAnnouncedCategoryName: String?
    @State private var searchErrorMessage: String?
    @State private var categoryNavigationGeneration = 0
    @FocusState private var searchFieldFocused: Bool

    private let radioBrowserService = RadioBrowserService()

    private let defaultQuickSearches = [
        "60s",
        "Classical",
        "Jazz",
        "Rock"
    ]

    private let genreChoices = [
        "60s", "70s", "80s", "90s", "Alternative", "Ambient",
        "Americana", "Big Band", "Bluegrass", "Blues", "Celtic",
        "Chill Out", "Christian", "Classical", "College", "Comedy",
        "Country", "Dance", "Disco", "Electronic", "Folk", "Funk",
        "Gospel", "Hawaiian", "Hip Hop", "House", "Indie", "Jazz",
        "K-pop", "Latin", "Lounge", "Metal", "Oldies", "Opera",
        "Piano", "Pop", "Public", "Punk", "Reggae", "Relaxation",
        "Rock", "Smooth Jazz", "Soul", "Soundtrack", "St. Louis",
        "Talk", "Techno", "Trance", "World"
    ]
    var body: some View {
        NavigationStack {
            ZStack {
                Color.black
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 12) {
                        topBar
                        searchArea
                        categoryCard
                        nowPlayingCard
                        playbackControls

                        Spacer().frame(height: 30)
                    }
                    .padding(.horizontal, 14)
                    .padding(.top, 4)
                }

                if showSearchOverlay {
                    searchOverlay
                }

                if showCategoryList {
                    categoryListOverlay
                }

                if stationListSearchName != nil || stationListCategoryID != nil {
                    stationListOverlay
                }

                if showSaveStationDialog {
                    saveStationOverlay
                }

                if showSettingsOverlay {
                    settingsOverlay
                }
            }
            .toolbar(.hidden, for: .navigationBar)
            .preferredColorScheme(.dark)
            .safeAreaInset(edge: .bottom, spacing: 0) {
                AdMobBannerView()
                    .frame(width: 320, height: 50)
                    .frame(maxWidth: .infinity)
                    .background(Color.black)
            }
            .onAppear {
                lastAnnouncedCategoryName = player.activeQueueName

                player.onNextTrackCommand = {
                    nextStation()
                }

                player.onPreviousTrackCommand = {
                    handlePreviousTrackCommand()
                }

                player.onAuditionFailed = { message in
                    // Status text handles this
                }

                Music1ChatIntentBridge.shared.connectCategoryActions(
                    next: { nextCategory() },
                    previous: { previousCategory() }
                )

                scheduleNavigationPrefetch()

                // DELAYED validation to speed up screen rendering
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                    Task {
                        for saved in player.savedSearchQueues {
                            if saved.name.caseInsensitiveCompare(player.activeQueueName ?? "") != .orderedSame {
                                try? await Task.sleep(nanoseconds: 500_000_000)
                                validateSearchBackground(named: saved.name)
                            }
                        }
                    }
                }
            }
        

            
            .onDisappear {
                player.onNextTrackCommand = nil
                player.onPreviousTrackCommand = nil
                Music1ChatIntentBridge.shared.disconnectCategoryActions()
            }
            
            .onChange(of: player.activeQueueName) { _, newName in
                announceCategoryIfNeeded(newName)
                scheduleNavigationPrefetch()
            }
            .onChange(of: player.currentQueueIndex) { _, _ in
                scheduleNavigationPrefetch()
            }
            .onChange(of: player.navigationRevision) { _, _ in
                scheduleNavigationPrefetch()
            }
            .alert(
                "Delete Search: \(pendingDeleteSearchName ?? "")?",
                isPresented: Binding(
                    get: { pendingDeleteSearchName != nil },
                    set: { if !$0 { pendingDeleteSearchName = nil } }
                )
            ) {
                Button("Cancel", role: .cancel) {
                    pendingDeleteSearchName = nil
                }
                Button("Delete", role: .destructive) {
                    if let name = pendingDeleteSearchName {
                        deleteSearchCategory(named: name)
                    }
                    pendingDeleteSearchName = nil
                }
            } message: {
                Text("This removes the saved search category. The station currently playing will keep playing while No Hands Radio finds the next enabled category.")
            }
            .alert(
                "Search",
                isPresented: Binding(
                    get: { searchErrorMessage != nil },
                    set: { if !$0 { searchErrorMessage = nil } }
                )
            ) {
                Button("OK") { searchErrorMessage = nil }
            } message: {
                Text(searchErrorMessage ?? "Station search is temporarily unavailable.")
            }
        }
    }

    // MARK: - Top Bar

    private var headerIcon: some View {
        Group {
            // No fixed frame here; parent (topBar) will control the size
            if let image = UIImage(named: "AppLogo") {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
            } else if let icon = UIImage(named: "AppIcon") {
                Image(uiImage: icon)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
            } else {
                Image(systemName: "radio")
                    .font(.system(size: 38))
                    .foregroundStyle(.purple)
            }
        }
    }

    private var topBar: some View {
        HStack(spacing: 0) {
            // RE-SIZED Header: Large but stays on screen
            headerIcon
                .frame(width: 170, height: 85)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .accessibilityLabel("No Hands Radio")

            Spacer()

            HStack(spacing: 8) {
                Button {
                    showSettingsOverlay = true
                } label: {
                    Image(systemName: "gearshape.fill")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 44, height: 44)
                }
                .buttonStyle(.plain)

                AirPlayRouteButton()
                    .frame(width: 44, height: 44)

                Button {
                    player.togglePlayback()
                } label: {
                    Image(systemName: "power")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(
                            player.isPlaying || player.isConnecting ? .red : .green
                        )
                        .frame(width: 48, height: 48)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.top, 6)
    }
            

    private func topIcon(
        systemName: String
    ) -> some View {
        Image(systemName: systemName)
            .font(.system(size: 18, weight: .semibold))
            .foregroundStyle(.white)
            .frame(width: 32, height: 32)
    }

    // MARK: - Search

    private var searchArea: some View {
        VStack(spacing: 10) {
            HStack(spacing: 8) {
                Button {
                    showSearchOverlay = true
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: "magnifyingglass")
                            .foregroundStyle(.secondary)
                        Text(searchText.isEmpty ? "Search genres" : searchText)
                            .foregroundStyle(searchText.isEmpty ? Color.secondary : Color.white)
                            .lineLimit(1)
                        Spacer()
                    }
                    .padding(.horizontal, 14)
                    .frame(height: 48)
                    .background(
                        RoundedRectangle(cornerRadius: 14)
                            .fill(Color.white.opacity(0.10))
                    )
                }
                .buttonStyle(.plain)

                Button {
                    showSearchOverlay = true
                } label: {
                    Image(systemName: "chevron.down")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 44, height: 48)
                        .background(
                            RoundedRectangle(cornerRadius: 14)
                                .fill(Color.white.opacity(0.10))
                        )
                }
                .buttonStyle(.plain)
            }

            HStack(spacing: 7) {
                ForEach(dynamicQuickSearches, id: \.self) { search in
                    Button {
                        searchText = search
                        submitInlineSearch(search)
                    } label: {
                        Text(search)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 10)
                            .frame(height: 32)
                            .background(
                                Capsule()
                                    .fill(Color.white.opacity(0.10))
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
            .frame(maxWidth: .infinity)
        }
    }

    private var dynamicQuickSearches: [String] {
        let recent = player.savedSearchQueues
            .map(\.name)
            .reversed()
            .reduce(into: [String]()) { result, name in
                if !result.contains(where: { $0.caseInsensitiveCompare(name) == .orderedSame }) {
                    result.append(name)
                }
            }

        var combined = Array(recent.prefix(4))
        for item in defaultQuickSearches where combined.count < 4 {
            if !combined.contains(where: { $0.caseInsensitiveCompare(item) == .orderedSame }) {
                combined.append(item)
            }
        }
        return combined
    }

    // MARK: - Category Card

    private var categoryCard: some View {
        HStack(spacing: 10) {
            Button {
                showCategoryList = true
            } label: {
                VStack(alignment: .leading, spacing: 2) { // Compact spacing
                    Text("CATEGORY")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(.secondary)

                    ScrollingText(
                        text: displayedCategoryName,
                        font: .system(size: 19, weight: .bold),
                        color: .white,
                        speed: 0.8
                    )
                    .frame(height: 24)

                    Text(categoryPositionText)
                        .font(.system(size: 11))
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .buttonStyle(.plain)

            if let activeSearch = activeSavedSearch {
                Button {
                    stationListSearchName = activeSearch.name
                    stationListCategoryID = nil
                } label: {
                    Image(systemName: "list.bullet")
                        .font(.system(size: 19, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 34, height: 34)
                }
                .buttonStyle(.plain)

                Button(role: .destructive) {
                    pendingDeleteSearchName = activeSearch.name
                } label: {
                    Image(systemName: "trash")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.red)
                        .frame(width: 34, height: 34)
                }
                .buttonStyle(.plain)
            } else if let activeCategory = activeLibraryCategory {
                Button {
                    stationListCategoryID = activeCategory.id
                    stationListSearchName = nil
                } label: {
                    Image(systemName: "list.bullet")
                        .font(.system(size: 19, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 34, height: 34)
                }
                .buttonStyle(.plain)
            }

            Button {
                toggleCurrentCategoryNavigation()
            } label: {
                NavigationArrowIndicator(enabled: currentCategoryNavigationEnabled)
                    .frame(width: 44, height: 34)
            }
            .buttonStyle(.plain)
        }
        .padding(16)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(Color.white.opacity(0.10))
        )
    }

    // MARK: - Now Playing

    private var nowPlayingCard: some View {
        VStack(spacing: 5) { // Compact inner spacing

            HStack(
                alignment: .top,
                spacing: 12
            ) {
                stationArtwork
                    .frame(width: 64, height: 64) // Smaller artwork for SE

                VStack(
                    alignment: .leading,
                    spacing: 2
                ) {
                    ScrollingText(
                        text: player.nowPlayingTitle
                            ?? (player.isPlaying
                                ? "Live Radio"
                                : ((library.categories.isEmpty && player.savedSearchQueues.isEmpty)
                                    ? "Tap a genre below to get started"
                                    : "Ready to Play")),
                        font: .system(size: 19, weight: .bold),
                        color: .white,
                        speed: 0.8
                    )
                    .frame(height: 24)

                    if let artist = player.nowPlayingArtist,
                       !artist.isEmpty {
                        Text(artist)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    }

                    HStack(spacing: 8) {
                        if !player.isPlaying {
                            Text(playbackStatusText)
                                .font(.system(size: 11, weight: .medium))
                                .foregroundStyle(.secondary)
                        }

                        VUMeter(isPlaying: player.isPlaying)
                            .frame(width: 38, height: 11)
                    }
                }

                Spacer()
            }

            HStack {
                VStack(
                    alignment: .leading,
                    spacing: 3
                ) {
                    Text("STATION")
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    ScrollingText(
                        text: displayedStation?.name ?? "None",
                        font: .system(size: 15, weight: .semibold),
                        color: .white
                    )
                    .frame(height: 20)
                    .frame(maxWidth: 230)
                }

                Spacer()

                Text(stationPositionText)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(.secondary)

                Button {
                    prepareStationFileAction()
                } label: {
                    Image(systemName: "folder")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 34, height: 34)
                }
                .buttonStyle(.plain)
                .disabled(displayedStation == nil)

                Button {
                    toggleCurrentStationNavigation()
                } label: {
                    NavigationArrowIndicator(enabled: currentStationNavigationEnabled)
                        .frame(width: 44, height: 34)
                }
                .buttonStyle(.plain)
                .disabled(displayedStation == nil)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 22)
                .fill(Color.white.opacity(0.10))
        )
    }

    private var stationArtwork: some View {
        Group {
            if let artwork = displayedStation?.artworkURL,
               let url = URL(string: artwork) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFit()
                            .padding(4)
                    default:
                        artworkFallback
                    }
                }
            } else {
                artworkFallback
            }
        }
        .frame(width: 76, height: 76)
        .background(
            RoundedRectangle(cornerRadius: 15)
                .fill(Color.white.opacity(0.08))
        )
        .clipShape(RoundedRectangle(cornerRadius: 15))
    }

    private var artworkFallback: some View {
        Image(systemName: "dot.radiowaves.left.and.right")
            .font(.system(size: 29, weight: .medium))
            .foregroundStyle(Color.white.opacity(0.65))
    }

    // MARK: - Controls

    private var playbackControls: some View {
        VStack(spacing: 12) {

            HStack {
                controlButton(
                    systemName: "backward.fill",
                    title: "",
                    action: previousCategory
                )

                Spacer()

                controlButton(
                    systemName: "arrowtriangle.left.fill",
                    title: "",
                    action: previousStation
                )

                Spacer()

                playButton

                Spacer()

                controlButton(
                    systemName: "arrowtriangle.right.fill",
                    title: "",
                    action: nextStation
                )

                Spacer()

                controlButton(
                    systemName: "forward.fill",
                    title: "",
                    action: nextCategory
                )
            }

            if let auditionStatus = player.auditionStatusMessage {
                HStack(spacing: 8) {
                    ProgressView()
                        .controlSize(.small)
                    Text(auditionStatus)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            } else if let error = player.errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundStyle(.red)
                    .multilineTextAlignment(.center)
            } else if player.isConnecting {
                HStack(spacing: 8) {
                    ProgressView()
                        .controlSize(.small)
                    Text(playbackStatusText)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.horizontal, 2)
    }

    private var playButton: some View {
        Button {
            togglePlayback()
        } label: {
            ZStack {
                Circle()
                    .fill(player.isPlaying ? Color.red : Color.green)
                    .frame(width: 62, height: 62)

                Image(
                    systemName:
                        player.isPlaying
                        ? "stop.fill"
                        : "play.fill"
                )
                .font(.system(
                    size: 25,
                    weight: .bold
                ))
                .foregroundStyle(.white)
                .offset(
                    x: player.isPlaying ? 0 : 2
                )
            }
        }
        .buttonStyle(.plain)
    }

    private func controlButton(
        systemName: String,
        title: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            VStack(spacing: 5) {
                Image(systemName: systemName)
                    .font(.system(
                        size: 21,
                        weight: .semibold
                    ))

                if !title.isEmpty {
                    Text(title)
                        .font(.system(size: 9))
                        .lineLimit(1)
                }
            }
            .foregroundStyle(.white)
            .frame(minWidth: 42)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Selected Data

    private var selectedCategory: Category? {
        guard library.categories.indices.contains(
            selectedCategoryIndex
        ) else {
            return nil
        }

        return library.categories[
            selectedCategoryIndex
        ]
    }

    private var categoryStations: [Station] {
        guard let selectedCategory else {
            return []
        }

        return library.stations(
            in: selectedCategory
        )
    }

    private var displayedStation: Station? {
        if let currentStation =
            player.currentStation {
            return currentStation
        }

        guard categoryStations.indices.contains(
            selectedStationIndex
        ) else {
            return nil
        }

        return categoryStations[
            selectedStationIndex
        ]
    }

    private var activeSavedSearch: AudioPlayerService.SavedSearchQueue? {
        guard player.activeLibraryCategoryID == nil,
              let activeName = player.activeQueueName else {
            return nil
        }

        return player.savedSearchQueues.first {
            $0.name.caseInsensitiveCompare(activeName) == .orderedSame
        }
    }

    private var activeLibraryCategory: Category? {
        if let categoryID = player.activeLibraryCategoryID {
            return library.categories.first { $0.id == categoryID }
        }

        return selectedCategory
    }

    private var displayedCategoryName: String {
        if let activeSavedSearch {
            return "Search: \(activeSavedSearch.name)"
        }
        if let activeLibraryCategory {
            return activeLibraryCategory.name
        }
        if library.categories.isEmpty && player.savedSearchQueues.isEmpty {
            return "No categories yet. Search to find stations."
        }
        return "No category selected"
    }

    private var currentCategoryNavigationEnabled: Bool {
        _ = player.navigationRevision
        if let activeSavedSearch {
            return player.isSearchNavigationEnabled(named: activeSavedSearch.name)
        }
        if let activeLibraryCategory {
            return player.isCategoryNavigationEnabled(categoryID: activeLibraryCategory.id)
        }
        return false
    }

    private func toggleCurrentCategoryNavigation() {
        if let activeSavedSearch {
            player.toggleSearchNavigation(named: activeSavedSearch.name)
        } else if let activeLibraryCategory {
            player.toggleCategoryNavigation(categoryID: activeLibraryCategory.id)
        }
    }

    private var categoryPositionText: String {
        let allKeys = library.categories.map { "category:\($0.id.uuidString)" } +
            player.savedSearchQueues.map { "search:\($0.name.lowercased())" }

        guard !allKeys.isEmpty else { return "0 of 0" }

        let currentKey: String?
        if let activeSavedSearch {
            currentKey = "search:\(activeSavedSearch.name.lowercased())"
        } else if let activeLibraryCategory {
            currentKey = "category:\(activeLibraryCategory.id.uuidString)"
        } else {
            currentKey = nil
        }

        let index = currentKey.flatMap { allKeys.firstIndex(of: $0) } ?? 0
        return "\(index + 1) of \(allKeys.count)"
    }

    private var stationPositionText: String {
        if let queueIndex = player.currentQueueIndex,
           !player.activeQueue.isEmpty {
            return "\(queueIndex + 1) of \(player.activeQueue.count)"
        }

        guard !categoryStations.isEmpty else {
            return "0 of 0"
        }

        return "\(selectedStationIndex + 1) of \(categoryStations.count)"
    }

    // MARK: - Playback

    private var playbackStatusText: String {
        switch player.playbackState {
        case .stopped:
            return "Ready"

        case .resolving:
            return "Checking stream…"

        case .connecting:
            return "Connecting…"

        case .playing:
            return "Playing"

        case .paused:
            return "Paused"

        case .failed:
            return "Playback unavailable"
        }
    }

    private func togglePlayback() {
        if player.isPlaying || player.isConnecting {
            player.stop()
            return
        }

        if !player.activeQueue.isEmpty {
            player.restartCurrentQueueItem()
            return
        }

        guard let station = selectedStationForPlayback else {
            return
        }

        player.play(
            queue: categoryStations,
            name: selectedCategory?.name,
            startAt: selectedStationIndex,
            autoAdvanceOnFailure: true
        )
    }

    private var selectedStationForPlayback: Station? {
        guard categoryStations.indices.contains(
            selectedStationIndex
        ) else {
            return nil
        }

        return categoryStations[
            selectedStationIndex
        ]
    }

    // MARK: - Navigation

    private func previousStation() {
        if player.activeQueue.isEmpty {
            guard !categoryStations.isEmpty else { return }
            player.play(
                queue: categoryStations,
                name: selectedCategory?.name,
                startAt: selectedStationIndex,
                autoAdvanceOnFailure: true
            )
        } else {
            player.auditionPreviousStation()
        }
    }

    private func nextStation() {
        if player.activeQueue.isEmpty {
            guard !categoryStations.isEmpty else { return }
            player.play(
                queue: categoryStations,
                name: selectedCategory?.name,
                startAt: selectedStationIndex,
                autoAdvanceOnFailure: true
            )
        } else {
            player.auditionNextStation()
        }
    }

    private func scheduleNavigationPrefetch() {
        Task { @MainActor in
            // Let any just-completed category/station handoff publish its final
            // queue state before computing the next likely destinations.
            try? await Task.sleep(nanoseconds: 180_000_000)
            prefetchLikelyNavigationTargets()
        }
    }

    private func prefetchLikelyNavigationTargets() {
        // The forward station command is the most common Bluetooth/Siri action.
        player.prefetchNextStation()

        // Also pre-resolve the first playable candidate in the next enabled
        // category. This keeps the button press from paying directory/playlist
        // resolution latency before AVPlayer can even begin connecting.
        guard let target = nextEligibleTarget() else { return }

        switch target {
        case .library(let category):
            let stations = library.stations(in: category)
            guard !stations.isEmpty else { return }
            player.prefetch(
                queue: stations,
                name: category.name,
                startAt: 0,
                step: 1
            )

        case .search(let saved):
            guard !saved.stations.isEmpty else { return }
            player.prefetch(
                queue: saved.stations,
                name: saved.name,
                startAt: 0,
                step: 1
            )
        }
    }

    private func previousCategory() {
        moveCategory(by: -1)
    }

    private func nextCategory() {
        moveCategory(by: 1)
    }

    private func moveCategory(
        by offset: Int
    ) {
        let direction = offset < 0 ? -1 : 1
        let targets = allCategoryTargets().filter { isEligibleCategoryTarget($0) }
        guard !targets.isEmpty else { return }

        // A new category command owns navigation from this point forward.
        // Cancel any older audition so a late callback cannot overwrite the
        // user's newer Previous/Next Category request.
        categoryNavigationGeneration += 1
        let generation = categoryNavigationGeneration
        player.cancelQueueAudition()

        let currentIndex = targets.firstIndex { target in
            switch target {
            case .library(let category):
                return activeLibraryCategory?.id == category.id
            case .search(let saved):
                return activeSavedSearch?.name.caseInsensitiveCompare(saved.name) == .orderedSame
            }
        }

        let firstIndex: Int
        if let currentIndex {
            firstIndex = ((currentIndex + direction) % targets.count + targets.count) % targets.count
        } else {
            firstIndex = direction > 0 ? 0 : targets.count - 1
        }

        auditionCategoryTarget(
            targets,
            index: firstIndex,
            direction: direction,
            attemptsRemaining: targets.count,
            generation: generation
        )
    }

    private func auditionCategoryTarget(
        _ targets: [CategoryTarget],
        index: Int,
        direction: Int,
        attemptsRemaining: Int,
        generation: Int
    ) {
        guard generation == categoryNavigationGeneration,
              !targets.isEmpty,
              attemptsRemaining > 0 else { return }

        let safeIndex = ((index % targets.count) + targets.count) % targets.count
        let target = targets[safeIndex]
        let status = direction > 0 ? "Finding next category…" : "Finding previous category…"

        let failure: () -> Void = {
            guard generation == categoryNavigationGeneration else { return }
            let next = ((safeIndex + direction) % targets.count + targets.count) % targets.count
            auditionCategoryTarget(
                targets,
                index: next,
                direction: direction,
                attemptsRemaining: attemptsRemaining - 1,
                generation: generation
            )
        }

        switch target {
        case .library(let category):
            let stations = library.stations(in: category)
            guard !stations.isEmpty else {
                failure()
                return
            }

            player.audition(
                queue: stations,
                name: category.name,
                libraryCategoryID: category.id,
                startAt: direction > 0 ? 0 : stations.count - 1,
                step: direction,
                saveAsSearch: false,
                statusMessage: status,
                onFailure: failure
            )
        case .search(let saved):
            guard !saved.stations.isEmpty else {
                failure()
                return
            }

            player.audition(
                queue: saved.stations,
                name: saved.name,
                startAt: direction > 0 ? 0 : saved.stations.count - 1,
                step: direction,
                saveAsSearch: false,
                statusMessage: status,
                onFailure: failure
            )
        }
    }
    private var currentStationNavigationEnabled: Bool {
        guard let station = displayedStation else { return false }
        return player.isStationNavigationEnabled(
            stationID: station.id,
            inQueueNamed: player.activeQueueName
        )
    }

    private func toggleCurrentStationNavigation() {
        guard let station = displayedStation else { return }
        player.toggleStationNavigation(
            stationID: station.id,
            inQueueNamed: player.activeQueueName
        )
    }

    private func prepareStationFileAction() {
        guard displayedStation != nil else { return }
        saveCategoryName = activeSavedSearch?.name ?? ""
        showSaveStationDialog = true
    }

    // MARK: - Search Category Deletion

    private enum CategoryTarget {
        case library(Category)
        case search(AudioPlayerService.SavedSearchQueue)
    }

    private func allCategoryTargets() -> [CategoryTarget] {
        library.categories.map(CategoryTarget.library)
            + player.savedSearchQueues.map(CategoryTarget.search)
    }

    private func isEligibleCategoryTarget(
        _ target: CategoryTarget,
        excludingLibraryCategoryID: UUID? = nil,
        excludingSearchName: String? = nil
    ) -> Bool {
        switch target {
        case .library(let category):
            return category.id != excludingLibraryCategoryID
                && !library.stations(in: category).isEmpty
                && player.isCategoryNavigationEnabled(categoryID: category.id)

        case .search(let saved):
            if let excludingSearchName,
               saved.name.caseInsensitiveCompare(excludingSearchName) == .orderedSame {
                return false
            }
            return !saved.stations.isEmpty
                && player.isSearchNavigationEnabled(named: saved.name)
        }
    }

    private func nextEligibleTarget(
        excludingLibraryCategoryID: UUID? = nil,
        excludingSearchName: String? = nil
    ) -> CategoryTarget? {
        let all = allCategoryTargets()
        guard !all.isEmpty else { return nil }

        let currentIndex = all.firstIndex { target in
            switch target {
            case .library(let category):
                if let excludingLibraryCategoryID {
                    return category.id == excludingLibraryCategoryID
                }
                return activeLibraryCategory?.id == category.id

            case .search(let saved):
                if let excludingSearchName {
                    return saved.name.caseInsensitiveCompare(excludingSearchName) == .orderedSame
                }
                return activeSavedSearch?.name.caseInsensitiveCompare(saved.name) == .orderedSame
            }
        } ?? -1

        for step in 1...all.count {
            let index = (currentIndex + step + all.count) % all.count
            let target = all[index]
            if isEligibleCategoryTarget(
                target,
                excludingLibraryCategoryID: excludingLibraryCategoryID,
                excludingSearchName: excludingSearchName
            ) {
                return target
            }
        }
        return nil
    }

    private func auditionCategoryTarget(_ target: CategoryTarget) {
        switch target {
        case .library(let category):
            player.audition(
                queue: library.stations(in: category),
                name: category.name,
                libraryCategoryID: category.id,
                startAt: 0,
                step: 1,
                saveAsSearch: false,
                statusMessage: "Finding next category…"
            )

        case .search(let saved):
            player.audition(
                queue: saved.stations,
                name: saved.name,
                startAt: 0,
                step: 1,
                saveAsSearch: false,
                statusMessage: "Finding next category…"
            )
        }
    }
    private func deleteSearchCategory(named name: String) {
        let isActive = activeSavedSearch?.name.caseInsensitiveCompare(name) == .orderedSame
        let replacement = isActive
            ? nextEligibleTarget(excludingSearchName: name)
            : nil

        if let replacement {
            auditionCategoryTarget(replacement)
        }

        player.removeSavedSearchQueue(named: name)

        if isActive && replacement == nil {
            player.stop(clearQueue: true)
        }
    }

    private func deleteLibraryCategory(_ category: Category) {
        let isActive = activeLibraryCategory?.id == category.id
        let replacement = isActive
            ? nextEligibleTarget(excludingLibraryCategoryID: category.id)
            : nil

        if let replacement {
            auditionCategoryTarget(replacement)
        }

        library.deleteCategory(category)
        selectedCategoryIndex = min(
            selectedCategoryIndex,
            max(library.categories.count - 1, 0)
        )
        selectedStationIndex = 0

        if isActive && replacement == nil {
            player.stop(clearQueue: true)
        }
    }

    private func handleCategoryBecameEmpty(categoryID: UUID) {
        // If the category is empty, we delete it entirely to match Android logic
        // We do this on the main queue with a tiny delay to allow the station list UI to dismiss first
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            if let category = library.categories.first(where: { $0.id == categoryID }) {
                deleteLibraryCategory(category)
            }
        }
    }

    // MARK: - Search Overlay

    private var searchOverlay: some View {
        ZStack {
            Color.black.opacity(0.58)
                .ignoresSafeArea()
                .onTapGesture {
                    showSearchOverlay = false
                    searchFieldFocused = false
                }

            VStack(spacing: 12) {
                HStack(spacing: 8) {
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(.secondary)

                    TextField("Search genres", text: $searchText)
                        .textInputAutocapitalization(.words)
                        .autocorrectionDisabled()
                        .submitLabel(.search)
                        .focused($searchFieldFocused)
                        .onSubmit { submitInlineSearch(searchText) }

                    if !searchText.isEmpty {
                        Button { searchText = "" } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(.secondary)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 12)
                .frame(height: 46)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.black.opacity(0.28))
                )

                ScrollView {
                    LazyVStack(spacing: 4) {
                        ForEach(searchSuggestions, id: \.self) { suggestion in
                            Button {
                                searchText = suggestion
                                submitInlineSearch(suggestion)
                            } label: {
                                HStack {
                                    Text(suggestion)
                                        .foregroundStyle(.primary)
                                    Spacer()
                                }
                                .padding(.horizontal, 12)
                                .frame(height: 38)
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .frame(maxHeight: 300)
            }
            .padding(16)
            .frame(maxWidth: 350)
            .background(
                RoundedRectangle(cornerRadius: 22)
                    .fill(Color(.secondarySystemBackground))
            )
            .shadow(radius: 24)
            .padding(.horizontal, 22)
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                    searchFieldFocused = true
                }
            }
        }
        .zIndex(40)
    }

    private var searchSuggestions: [String] {
        let trimmed = searchText.trimmingCharacters(
            in: .whitespacesAndNewlines
        )

        if trimmed.isEmpty {
            return genreChoices
        }

        let prefixMatches = genreChoices.filter { genre in
            genre.range(
                of: trimmed,
                options: [
                    .anchored,
                    .caseInsensitive,
                    .diacriticInsensitive
                ]
            ) != nil
        }

        let containsMatches = genreChoices.filter { genre in
            !prefixMatches.contains(genre) &&
            genre.range(
                of: trimmed,
                options: [
                    .caseInsensitive,
                    .diacriticInsensitive
                ]
            ) != nil
        }

        let combined = prefixMatches + containsMatches
        return combined.count > 20
            ? Array(combined[0..<20])
            : combined
    }

    private func submitInlineSearch(_ rawText: String) {
        let query = rawText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return }

        searchText = query
        showSearchOverlay = false
        searchFieldFocused = false
        searchErrorMessage = nil
        player.setAuditionStatus(message: "Searching for “\(query)”…")

        Task {
            do {
                let results = try await radioBrowserService.searchStations(
                    for: query,
                    limit: settings.maximumSearchResults
                )
                guard !results.isEmpty else {
                    player.setAuditionStatus(message: nil)
                    searchErrorMessage = "No stations matched \(query)."
                    return
                }

                searchText = ""
                player.audition(
                    queue: results.map(\.asStation),
                    name: query,
                    startAt: 0,
                    step: 1,
                    saveAsSearch: true,
                    statusMessage: "Finding a playable station…"
                )
            } catch {
                searchErrorMessage = (error as? LocalizedError)?.errorDescription
                    ?? "Station search is temporarily unavailable. Please try again."
            }
        }
    }

    private func validateSearchBackground(named query: String) {
        Task {
            do {
                let results = try await radioBrowserService.searchStations(
                    for: query,
                    limit: settings.maximumSearchResults
                )

                if !results.isEmpty {
                    // This call will trigger the Safety Shield in saveOrUpdateSearchQueue
                    // if the results are suspiciously small.
                    let stationList: [Station] = results.map { $0.asStation }
                    player.updateSavedSearchQueue(named: query, stations: stationList)
                }
            } catch {
                // Silently ignore background refresh errors
            }
        }
    }

    // MARK: - Category List Overlay

    private var categoryListOverlay: some View {
        ZStack {
            Color.black.opacity(0.58)
                .ignoresSafeArea()
                .onTapGesture { showCategoryList = false }

            CategoryListScreen(
                library: library,
                compactMode: true,
                onDismiss: { showCategoryList = false },
                onOpenSearchStations: { name in
                    showCategoryList = false
                    stationListSearchName = name
                    stationListCategoryID = nil
                },
                onOpenLibraryStations: { id in
                    showCategoryList = false
                    stationListCategoryID = id
                    stationListSearchName = nil
                },
                onDeleteSearchCategory: { name in
                    deleteSearchCategory(named: name)
                },
                onDeleteLibraryCategory: { category in
                    deleteLibraryCategory(category)
                }
            )
            .frame(maxWidth: 370, maxHeight: 520)
            .background(
                RoundedRectangle(cornerRadius: 22)
                    .fill(Color(.secondarySystemBackground))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 22)
                    .stroke(Color.white.opacity(0.92), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 22))
            .shadow(radius: 24)
            .padding(.horizontal, 20)
        }
        .transition(.opacity)
        .zIndex(19)
    }

    // MARK: - Save Station Overlay

    private var saveStationOverlay: some View {
        ZStack {
            Color.black.opacity(0.58)
                .ignoresSafeArea()
                .onTapGesture { showSaveStationDialog = false }

            VStack(alignment: .leading, spacing: 12) {
                Text(activeSavedSearch != nil ? "Save to Category" : "Station")
                    .font(.headline)

                if activeSavedSearch == nil,
                   let station = displayedStation,
                   let category = activeLibraryCategory {
                    Button(role: .destructive) {
                        library.removeStation(station, from: category)
                        showSaveStationDialog = false
                    } label: {
                        Label("Delete from \(category.name)", systemImage: "trash")
                    }
                }

                TextField("Category name", text: $saveCategoryName)
                    .textFieldStyle(.roundedBorder)

                Button {
                    saveDisplayedStation(to: saveCategoryName)
                } label: {
                    Text(saveCategoryName.isEmpty ? "Save" : "Save to \(saveCategoryName)")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(saveCategoryName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                ScrollView {
                    VStack(spacing: 7) {
                        ForEach(saveCategoryChoices, id: \.self) { name in
                            Button {
                                saveCategoryName = name
                                saveDisplayedStation(to: name)
                            } label: {
                                HStack {
                                    Text(name)
                                    Spacer()
                                }
                                .padding(.horizontal, 12)
                                .frame(height: 36)
                                .background(
                                    RoundedRectangle(cornerRadius: 9)
                                        .fill(Color.white.opacity(0.08))
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .frame(maxHeight: 280)

                Button("Cancel") {
                    showSaveStationDialog = false
                }
                .frame(maxWidth: .infinity)
            }
            .padding(18)
            .frame(maxWidth: 350)
            .background(
                RoundedRectangle(cornerRadius: 22)
                    .fill(Color(.secondarySystemBackground))
            )
            .padding(.horizontal, 22)
        }
        .zIndex(30)
    }

    private var saveCategoryChoices: [String] {
        var names: [String] = []

        // A search category name is a useful default for creating a new static
        // category with the same listening theme.
        if let searchName = activeSavedSearch?.name {
            names.append(searchName)
        }

        let currentStaticCategoryID = player.activeLibraryCategoryID
        names.append(
            contentsOf: library.categories
                .filter { $0.id != currentStaticCategoryID }
                .map(\.name)
                .sorted {
                    $0.localizedCaseInsensitiveCompare($1) == .orderedAscending
                }
        )
        names.append(contentsOf: genreChoices)

        let currentStaticName = activeLibraryCategory?.name

        return names.reduce(into: [String]()) { result, name in
            if let currentStaticName,
               name.caseInsensitiveCompare(currentStaticName) == .orderedSame {
                return
            }

            if !result.contains(where: {
                $0.caseInsensitiveCompare(name) == .orderedSame
            }) {
                result.append(name)
            }
        }
    }

    private func saveDisplayedStation(to categoryName: String) {
        guard let station = displayedStation else { return }
        library.saveStation(station, toCategoryNamed: categoryName)
        showSaveStationDialog = false
    }

    // MARK: - Settings Overlay

    private var settingsOverlay: some View {
        ZStack {
            Color.black.opacity(0.58)
                .ignoresSafeArea()
                .onTapGesture { showSettingsOverlay = false }

            SettingsPanel(
                settings: settings,
                speaker: categorySpeaker,
                onDismiss: { showSettingsOverlay = false }
            )
            .padding(.horizontal, 18)
        }
        .transition(.opacity)
        .zIndex(60)
    }

    private func announceCategoryIfNeeded(_ newName: String?) {
        guard let newName,
              !newName.isEmpty else {
            lastAnnouncedCategoryName = newName
            return
        }

        defer { lastAnnouncedCategoryName = newName }

        guard settings.categoryAnnouncementsEnabled else {
            return
        }

        if let previous = lastAnnouncedCategoryName,
           previous.caseInsensitiveCompare(newName) == .orderedSame {
            return
        }

        categorySpeaker.announceCategory(
            newName,
            voiceIdentifier: settings.categoryVoiceIdentifier
        )
    }

    private func handlePreviousTrackCommand() {
        switch settings.previousTrackBehavior {
        case .nextCategory:
            nextCategory()
        case .previousStation:
            previousStation()
        }
    }

    // MARK: - Station List Overlay

    private var stationListOverlay: some View {
        ZStack {
            Color.black.opacity(0.58)
                .ignoresSafeArea()
                .onTapGesture { dismissStationList() }

            Group {
                if let searchName = stationListSearchName {
                    StationListScreen(
                        library: library,
                        searchQueueName: searchName,
                        compactMode: true,
                        onStationSelected: dismissStationList
                    )
                } else if let categoryID = stationListCategoryID,
                          let category = library.categories.first(where: { $0.id == categoryID }) {
                    StationListScreen(
                        library: library,
                        category: category,
                        compactMode: true,
                        onStationSelected: dismissStationList,
                        onCategoryBecameEmpty: { categoryID in
                            handleCategoryBecameEmpty(categoryID: categoryID)
                        }
                    )
                }
            }
            .frame(maxWidth: 370, maxHeight: 520)
            .background(
                RoundedRectangle(cornerRadius: 22)
                    .fill(Color(.secondarySystemBackground))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 22)
                    .stroke(Color.white.opacity(0.92), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 22))
            .shadow(radius: 24)
            .padding(.horizontal, 20)
        }
        .transition(.opacity)
        .zIndex(20)
    }

    private func dismissStationList() {
        stationListSearchName = nil
        stationListCategoryID = nil
    }

}

// MARK: - AdMob Banner View

private struct AdMobBannerView: UIViewRepresentable {
    /*
     * PRODUCTION READY:
     * When you have your real iOS IDs from AdMob, paste them here:
     * App ID (for Info.plist): ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
     * Banner ID: ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
     */

    private let productionAdUnitID = "ca-app-pub-3940256099942544/2934735716" // Replace with real ID
    private let testAdUnitID = "ca-app-pub-3940256099942544/2934735716"

    func makeUIView(context: Context) -> BannerView {
        MobileAds.shared.start()

        let banner = BannerView(adSize: AdSizeBanner)

        // SWITCH TO productionAdUnitID before submitting to Apple
        banner.adUnitID = testAdUnitID

        banner.load(Request())
        return banner
    }

    func updateUIView(_ uiView: BannerView, context: Context) {}
}

private struct AirPlayRouteButton: UIViewRepresentable {
    func makeUIView(context: Context) -> AVRoutePickerView {
        let picker = AVRoutePickerView(frame: .zero)
        picker.prioritizesVideoDevices = false
        picker.activeTintColor = .white
        picker.tintColor = .white
        return picker
    }

    func updateUIView(_ uiView: AVRoutePickerView, context: Context) {}
}

private struct NavigationArrowIndicator: View {
    let enabled: Bool

    var body: some View {
        ZStack {
            NavigationArrowShape()
                .stroke(
                    Color.green,
                    style: StrokeStyle(
                        lineWidth: 3.2,
                        lineCap: .round,
                        lineJoin: .round
                    )
                )
                .frame(width: 36, height: 17)

            if !enabled {
                Image(systemName: "xmark")
                    .font(.system(size: 18, weight: .black))
                    .foregroundStyle(.red)
            }
        }
        .accessibilityLabel(
            enabled
                ? "Included in navigation"
                : "Skipped in navigation"
        )
    }
}

private struct NavigationArrowShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let midY = rect.midY
        let tipX = rect.maxX - 1
        let headBaseX = tipX - 9

        path.move(to: CGPoint(x: rect.minX + 1, y: midY))
        path.addLine(to: CGPoint(x: tipX, y: midY))

        path.move(to: CGPoint(x: headBaseX, y: midY - 6))
        path.addLine(to: CGPoint(x: tipX, y: midY))
        path.addLine(to: CGPoint(x: headBaseX, y: midY + 6))
        return path
    }
}

private struct ScrollingText: View {
    let text: String
    let font: Font
    let color: Color
    var speed: Double = 1.0

    var body: some View {
        ViewThatFits(in: .horizontal) {
            Text(text)
                .font(font)
                .foregroundStyle(color)
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)

            MarqueeText(text: text, font: font, color: color, speed: speed)
        }
    }
}

private struct MarqueeText: View {
    let text: String
    let font: Font
    let color: Color
    let speed: Double

    @State private var offset: CGFloat = 0

    var body: some View {
        GeometryReader { geometry in
            Text(text)
                .font(font)
                .foregroundStyle(color)
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
                .offset(x: offset)
                .onAppear { start(in: geometry.size.width) }
                .onChange(of: text) { _, _ in
                    offset = 0
                    start(in: geometry.size.width)
                }
        }
        .clipped()
    }

    private func start(in width: CGFloat) {
        let textWidth = CGFloat(text.count) * 11.0
        offset = width
        // FIXED SPEED: 30 points per second (constant)
        let duration = Double(max(textWidth + width, 1)) / 30.0 / speed

        withAnimation(
            .linear(duration: duration)
                .repeatForever(autoreverses: false)
        ) {
            offset = -textWidth
        }
    }
}

private struct VUMeter: View {
    let isPlaying: Bool

    var body: some View {
        TimelineView(.animation(minimumInterval: 0.08)) { timeline in
            let time = timeline.date.timeIntervalSinceReferenceDate

            HStack(alignment: .bottom, spacing: 3) {
                ForEach(0..<7, id: \.self) { index in
                    Capsule()
                        .fill(isPlaying ? Color.green : Color.white.opacity(0.30))
                        .frame(
                            width: 3,
                            height: barHeight(index: index, time: time)
                        )
                }
            }
            .frame(maxWidth: .infinity, alignment: .center)
        }
    }

    private func barHeight(index: Int, time: TimeInterval) -> CGFloat {
        guard isPlaying else {
            return CGFloat(3 + (index % 2))
        }

        let speeds: [Double] = [3.1, 5.2, 2.7, 6.0, 3.8, 4.6, 2.3]
        let phases: [Double] = [0.2, 1.7, 0.9, 2.8, 1.1, 3.4, 2.0]
        let amplitudes: [Double] = [6.0, 9.5, 4.5, 8.0, 5.5, 10.0, 6.5]
        let i = index % speeds.count
        let wave = abs(sin(time * speeds[i] + phases[i]))
        let wobble = abs(sin(time * (speeds[i] * 0.43) + Double(index) * 1.13))
        return 2.5 + CGFloat(wave * amplitudes[i] + wobble * 2.2)
    }
}

#Preview {
    MainScreen()
        .environmentObject(AudioPlayerService())
}
