import SwiftUI

struct StationListScreen: View {
    @ObservedObject var library: MusicLibraryViewModel
    @EnvironmentObject private var player: AudioPlayerService
    @Environment(\.dismiss) private var dismiss

    private let category: Category?
    private let searchQueueName: String?
    private let compactMode: Bool
    private let onStationSelected: (() -> Void)?
    private let onCategoryBecameEmpty: ((UUID) -> Void)?

    @State private var pendingDeleteStation: Station?
    @State private var draggingStationID: UUID?
    @State private var dropBeforeStationID: UUID?
    @State private var dropAtEnd = false
    @State private var rowFrames: [UUID: CGRect] = [:]

    init(
        library: MusicLibraryViewModel,
        category: Category,
        compactMode: Bool = false,
        onStationSelected: (() -> Void)? = nil,
        onCategoryBecameEmpty: ((UUID) -> Void)? = nil
    ) {
        self.library = library
        self.category = category
        self.searchQueueName = nil
        self.compactMode = compactMode
        self.onStationSelected = onStationSelected
        self.onCategoryBecameEmpty = onCategoryBecameEmpty
    }

    init(
        library: MusicLibraryViewModel,
        searchQueueName: String,
        compactMode: Bool = false,
        onStationSelected: (() -> Void)? = nil
    ) {
        self.library = library
        self.category = nil
        self.searchQueueName = searchQueueName
        self.compactMode = compactMode
        self.onStationSelected = onStationSelected
        self.onCategoryBecameEmpty = nil
    }

    var body: some View {
        Group {
            if compactMode {
                VStack(spacing: 0) {
                    HStack {
                        Text(title)
                            .font(.headline)
                        Spacer()
                        Text("\(stations.count) stations")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 13)

                    Divider()
                    stationList
                }
            } else {
                stationList
                    .navigationTitle(title)
            }
        }
        .alert(
            "Remove station?",
            isPresented: Binding(
                get: { pendingDeleteStation != nil },
                set: { if !$0 { pendingDeleteStation = nil } }
            )
        ) {
            Button("Cancel", role: .cancel) {
                pendingDeleteStation = nil
            }
            Button("Remove", role: .destructive) {
                if let station = pendingDeleteStation {
                    deleteStation(station)
                }
                pendingDeleteStation = nil
            }
        } message: {
            Text("Remove \"\(pendingDeleteStation?.name ?? "this station")\" from \(title)?")
        }
    }

    private var stationList: some View {
        Group {
            if stations.isEmpty {
                ContentUnavailableView(
                    "No Stations",
                    systemImage: "radio",
                    description: Text("This category does not currently contain any stations.")
                )
            } else if searchQueueName != nil {
                List {
                    ForEach(Array(stations.enumerated()), id: \.element.id) { index, station in
                        stationRow(station: station, index: index, allowsDelete: false)
                    }
                }
                .listStyle(.plain)
            } else {
                staticReorderList
            }
        }
    }

    private var staticReorderList: some View {
        ScrollViewReader { scrollProxy in
            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(Array(stations.enumerated()), id: \.element.id) { index, station in
                        if dropBeforeStationID == station.id {
                            insertionLine
                        }

                        let isDragging = draggingStationID == station.id

                        stationRow(station: station, index: index, allowsDelete: true)
                            .id(station.id)
                            .padding(.leading, isDragging ? 30 : 0)
                            .padding(.vertical, 3)
                            .background(
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(isDragging ? Color.accentColor.opacity(0.24) : Color.clear)
                            )
                            .overlay {
                                GeometryReader { proxy in
                                    Color.clear.preference(
                                        key: StationRowFramePreferenceKey.self,
                                        value: [
                                            station.id: proxy.frame(in: .named("stationReorderSpace"))
                                        ]
                                    )
                                }
                            }
                            .contentShape(Rectangle())
                            .simultaneousGesture(
                                reorderGesture(
                                    stationID: station.id,
                                    scrollProxy: scrollProxy
                                )
                            )
                            .animation(.easeInOut(duration: 0.12), value: isDragging)

                        Divider()
                            .opacity(0.35)
                    }

                    if dropAtEnd {
                        insertionLine
                    }
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
            }
            .coordinateSpace(name: "stationReorderSpace")
            .onPreferenceChange(StationRowFramePreferenceKey.self) { frames in
                rowFrames = frames
            }
        }
    }

    private var insertionLine: some View {
        Rectangle()
            .fill(Color.accentColor)
            .frame(height: 3)
            .padding(.horizontal, 4)
            .shadow(color: Color.accentColor.opacity(0.4), radius: 2)
    }

    private func stationRow(
        station: Station,
        index: Int,
        allowsDelete: Bool
    ) -> some View {
        HStack(spacing: 10) {
            Button {
                playStation(at: index)
            } label: {
                VStack(alignment: .leading, spacing: 4) {
                    Text(station.name)
                        .font(.headline)
                        .foregroundStyle(.primary)
                        .lineLimit(2)

                    if player.currentStation?.id == station.id {
                        Text("Current station")
                            .font(.caption2)
                            .foregroundStyle(.green)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .buttonStyle(.plain)

            if allowsDelete {
                Button(role: .destructive) {
                    pendingDeleteStation = station
                } label: {
                    Image(systemName: "trash")
                        .frame(width: 30, height: 30)
                }
                .buttonStyle(.plain)
            }

            Button {
                player.toggleStationNavigation(
                    stationID: station.id,
                    inQueueNamed: queueName
                )
            } label: {
                NavigationArrowIndicator(
                    enabled: player.isStationNavigationEnabled(
                        stationID: station.id,
                        inQueueNamed: queueName
                    )
                )
                .frame(width: 44, height: 30)
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 3)
    }

    private func reorderGesture(
        stationID: UUID,
        scrollProxy: ScrollViewProxy
    ) -> some Gesture {
        LongPressGesture(minimumDuration: 0.32)
            .sequenced(
                before: DragGesture(
                    minimumDistance: 0,
                    coordinateSpace: .named("stationReorderSpace")
                )
            )
            .onChanged { value in
                switch value {
                case .first(true):
                    beginDragging(stationID)

                case .second(true, let drag?):
                    beginDragging(stationID)
                    updateDropLocation(
                        y: drag.location.y,
                        scrollProxy: scrollProxy
                    )

                default:
                    break
                }
            }
            .onEnded { value in
                if case .second(true, let drag?) = value {
                    updateDropLocation(
                        y: drag.location.y,
                        scrollProxy: scrollProxy
                    )
                    commitDraggedStation()
                } else {
                    clearDragState()
                }
            }
    }

    private func beginDragging(_ stationID: UUID) {
        guard draggingStationID == nil else { return }
        draggingStationID = stationID
        dropBeforeStationID = nil
        dropAtEnd = false
    }

    private func updateDropLocation(
        y: CGFloat,
        scrollProxy: ScrollViewProxy
    ) {
        guard let movingID = draggingStationID else { return }

        let remaining = stations.filter { $0.id != movingID }
        guard !remaining.isEmpty else {
            dropBeforeStationID = nil
            dropAtEnd = true
            return
        }

        for station in remaining {
            guard let frame = rowFrames[station.id] else { continue }
            if y < frame.midY {
                dropBeforeStationID = station.id
                dropAtEnd = false
                maybeAutoScroll(y: y, scrollProxy: scrollProxy)
                return
            }
        }

        dropBeforeStationID = nil
        dropAtEnd = true
        maybeAutoScroll(y: y, scrollProxy: scrollProxy)
    }

    private func maybeAutoScroll(
        y: CGFloat,
        scrollProxy: ScrollViewProxy
    ) {
        let visibleFrames = rowFrames.values
        guard let minY = visibleFrames.map(\.minY).min(),
              let maxY = visibleFrames.map(\.maxY).max() else {
            return
        }

        if y < minY + 42,
           let firstVisible = rowFrames.min(by: { $0.value.minY < $1.value.minY })?.key,
           let firstIndex = stations.firstIndex(where: { $0.id == firstVisible }),
           firstIndex > 0 {
            withAnimation(.linear(duration: 0.12)) {
                scrollProxy.scrollTo(stations[firstIndex - 1].id, anchor: .top)
            }
        } else if y > maxY - 42,
                  let lastVisible = rowFrames.max(by: { $0.value.maxY < $1.value.maxY })?.key,
                  let lastIndex = stations.firstIndex(where: { $0.id == lastVisible }),
                  lastIndex + 1 < stations.count {
            withAnimation(.linear(duration: 0.12)) {
                scrollProxy.scrollTo(stations[lastIndex + 1].id, anchor: .bottom)
            }
        }
    }

    private func commitDraggedStation() {
        guard let movingID = draggingStationID,
              let category = resolvedCategory else {
            clearDragState()
            return
        }

        let remaining = stations.filter { $0.id != movingID }
        let targetIndex: Int

        if let beforeID = dropBeforeStationID,
           let index = remaining.firstIndex(where: { $0.id == beforeID }) {
            targetIndex = index
        } else {
            targetIndex = remaining.count
        }

        library.moveStation(
            in: category,
            stationID: movingID,
            toIndex: targetIndex
        )

        clearDragState()
    }

    private func clearDragState() {
        draggingStationID = nil
        dropBeforeStationID = nil
        dropAtEnd = false
    }

    private var title: String {
        if let searchQueueName {
            return "Search: \(searchQueueName)"
        }
        return category?.name ?? "Stations"
    }

    private var queueName: String? {
        searchQueueName ?? category?.name
    }

    private var stations: [Station] {
        if let searchQueueName,
           let saved = player.savedSearchQueues.first(where: {
               $0.name.caseInsensitiveCompare(searchQueueName) == .orderedSame
           }) {
            return saved.stations
        }

        if let category = resolvedCategory {
            return library.stations(in: category)
        }

        return []
    }

    private var resolvedCategory: Category? {
        guard let category else { return nil }
        return library.categories.first(where: { $0.id == category.id })
            ?? category
    }

    private func playStation(at index: Int) {
        let currentStations = stations
        guard currentStations.indices.contains(index) else { return }

        player.audition(
            queue: currentStations,
            name: queueName,
            libraryCategoryID: category?.id,
            startAt: index,
            step: 1,
            saveAsSearch: false,
            statusMessage: "Opening station…"
        )

        if let onStationSelected {
            onStationSelected()
        } else {
            dismiss()
        }
    }

    private func deleteStation(_ station: Station) {
        guard let category = resolvedCategory else { return }

        // Remove from the data model first
        library.removeStation(station, from: category)

        // Tell the player to remove it from the active session if it's currently playing
        player.removeStationFromActiveQueue(
            stationID: station.id,
            categoryID: category.id
        )

        // Check if we just emptied the category
        let remaining = library.stations(in: category)
        if remaining.isEmpty {
            onCategoryBecameEmpty?(category.id)
        }
    }
}

private struct StationRowFramePreferenceKey: PreferenceKey {
    static var defaultValue: [UUID: CGRect] = [:]

    static func reduce(
        value: inout [UUID: CGRect],
        nextValue: () -> [UUID: CGRect]
    ) {
        value.merge(nextValue(), uniquingKeysWith: { _, new in new })
    }
}

private struct NavigationArrowIndicator: View {
    let enabled: Bool

    var body: some View {
        ZStack {
            NavigationArrowShape()
                .stroke(
                    Color.green,
                    style: StrokeStyle(lineWidth: 3.2, lineCap: .round, lineJoin: .round)
                )
                .frame(width: 36, height: 17)

            if !enabled {
                Image(systemName: "xmark")
                    .font(.system(size: 18, weight: .black))
                    .foregroundStyle(.red)
            }
        }
        .accessibilityLabel(enabled ? "Included in navigation" : "Skipped in navigation")
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

#Preview {
    NavigationStack {
        StationListScreen(
            library: MusicLibraryViewModel(),
            category: MusicLibraryViewModel.sampleCategories[0]
        )
        .environmentObject(AudioPlayerService())
    }
}
