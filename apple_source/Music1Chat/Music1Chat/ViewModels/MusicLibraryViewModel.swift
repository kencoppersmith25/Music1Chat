import Foundation
import Combine

@MainActor
final class MusicLibraryViewModel: ObservableObject {
    @Published var stations: [Station] {
        didSet { persistIfReady() }
    }
    @Published var categories: [Category] {
        didSet { persistIfReady() }
    }

    private struct LibrarySnapshot: Codable {
        let stations: [Station]
        let categories: [Category]
    }

    private let persistenceKey = "NoHandsRadio.Library.v1"
    private var persistenceReady = false

    init(
        stations: [Station] = [],
        categories: [Category] = []
    ) {
        let shouldRestorePersistedLibrary =
            stations.isEmpty && categories.isEmpty

        if shouldRestorePersistedLibrary,
           let data = UserDefaults.standard.data(forKey: persistenceKey),
           let snapshot = try? JSONDecoder().decode(LibrarySnapshot.self, from: data) {
            self.stations = snapshot.stations
            self.categories = snapshot.categories
        } else {
            self.stations = stations
            self.categories = categories
        }

        removeLegacySampleFavoritesIfPresent()

        // Ensure "Reliable Stations" exists on first run
        if categories.isEmpty && stations.isEmpty {
            createReliableStarterPack()
        }

        persistenceReady = true
        persistIfReady()
    }

    private func createReliableStarterPack() {
        let starterStations = [
            Station(name: "Classical King FM", streamURL: "https://kingfm.streamguys1.com/king-fm-mp3", artworkURL: "https://www.king.org/wp-content/uploads/2019/12/KINGFM_Logos_Square_White_RGB-1.png"),
            Station(name: "Jazz24", streamURL: "https://kexp-jazz24.streamguys1.com/jazz24.mp3", artworkURL: "https://vignette.wikia.nocookie.net/logopedia/images/4/4e/Jazz24_logo.png"),
            Station(name: "BBC World Service", streamURL: "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service", artworkURL: "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e1/BBC_World_Service_logo.svg/1200px-BBC_World_Service_logo.svg.png"),
            Station(name: "SomaFM Groove Salad", streamURL: "https://ice1.somafm.com/groovesalad-256-mp3", artworkURL: "https://somafm.com/img/gs256.png"),
            Station(name: "Radio Swiss Classic", streamURL: "https://stream.srg-ssr.ch/m/rsc_de/mp3_128", artworkURL: "https://www.radioswissclassic.ch/images/rsc_logo_square.png")
        ]

        self.stations = starterStations
        let reliableCategory = Category(name: "Reliable Stations", stationIDs: starterStations.map { $0.id })
        self.categories = [reliableCategory]

        // Ensure stations are linked to the category
        for i in self.stations.indices {
            self.stations[i].categoryIDs = [reliableCategory.id]
        }
    }

    func stations(in category: Category) -> [Station] {
        category.stationIDs.compactMap { stationID in
            stations.first { $0.id == stationID }
        }
    }

    func category(for station: Station) -> [Category] {
        categories.filter { station.categoryIDs.contains($0.id) }
    }

    func removeStation(_ station: Station, from category: Category) {
        guard let categoryIndex = categories.firstIndex(where: { $0.id == category.id }) else { return }
        categories[categoryIndex].stationIDs.removeAll { $0 == station.id }

        if let stationIndex = stations.firstIndex(where: { $0.id == station.id }) {
            stations[stationIndex].categoryIDs.removeAll { $0 == category.id }
        }
    }

    func deleteCategory(_ category: Category) {
        categories.removeAll { $0.id == category.id }
        for index in stations.indices {
            stations[index].categoryIDs.removeAll { $0 == category.id }
        }
    }

    func addStation(_ station: Station, to category: Category) {
        let stationIndex: Int
        if let existing = stations.firstIndex(where: { $0.id == station.id }) {
            stationIndex = existing
            stations[existing].name = station.name
            stations[existing].streamURL = station.streamURL
        } else {
            stations.append(station)
            stationIndex = stations.count - 1
        }

        guard let categoryIndex = categories.firstIndex(where: { $0.id == category.id }) else { return }

        if !categories[categoryIndex].stationIDs.contains(station.id) {
            categories[categoryIndex].stationIDs.append(station.id)
        }
        if !stations[stationIndex].categoryIDs.contains(category.id) {
            stations[stationIndex].categoryIDs.append(category.id)
        }
    }


    @discardableResult
    func createCategory(named rawName: String) -> Category? {
        let name = rawName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { return nil }

        if let existing = categories.first(where: {
            $0.name.caseInsensitiveCompare(name) == .orderedSame
        }) {
            return existing
        }

        let category = Category(
            id: UUID(),
            name: name,
            stationIDs: []
        )
        categories.append(category)
        return category
    }

    func saveStation(_ station: Station, toCategoryNamed rawName: String) {
        guard let category = createCategory(named: rawName) else { return }
        addStation(station, to: category)
    }

    func moveStation(
        in category: Category,
        stationID: UUID,
        toIndex requestedIndex: Int
    ) {
        guard let categoryIndex = categories.firstIndex(where: { $0.id == category.id }),
              let sourceIndex = categories[categoryIndex].stationIDs.firstIndex(of: stationID) else {
            return
        }

        var ids = categories[categoryIndex].stationIDs
        let movingID = ids.remove(at: sourceIndex)
        let insertionIndex = min(max(requestedIndex, 0), ids.count)
        ids.insert(movingID, at: insertionIndex)
        categories[categoryIndex].stationIDs = ids
    }

    private func removeLegacySampleFavoritesIfPresent() {
        // Early development builds seeded a fake Favorites category containing
        // one hard-coded KEXP station. Remove only that exact legacy seed.
        guard let categoryIndex = categories.firstIndex(where: { category in
            category.name.caseInsensitiveCompare("Favorites") == .orderedSame &&
            category.stationIDs.count == 1
        }) else {
            return
        }

        let legacyStationID = categories[categoryIndex].stationIDs[0]

        guard let stationIndex = stations.firstIndex(where: { station in
            station.id == legacyStationID &&
            station.name.caseInsensitiveCompare("KEXP") == .orderedSame &&
            station.streamURL.lowercased().contains("kexp.streamguys1.com/kexp160.aac")
        }) else {
            return
        }

        categories.remove(at: categoryIndex)

        let isStillUsedElsewhere = categories.contains { category in
            category.stationIDs.contains(legacyStationID)
        }

        if !isStillUsedElsewhere {
            stations.remove(at: stationIndex)
        }
    }

    private func persistIfReady() {
        guard persistenceReady else { return }
        let snapshot = LibrarySnapshot(stations: stations, categories: categories)
        guard let data = try? JSONEncoder().encode(snapshot) else { return }
        UserDefaults.standard.set(data, forKey: persistenceKey)
    }

    static let sampleCategoryID = UUID()
    static let sampleStationID = UUID()

    static let sampleStations: [Station] = [
        Station(
            id: sampleStationID,
            name: "KEXP",
            streamURL: "https://kexp.streamguys1.com/kexp160.aac",
            categoryIDs: [sampleCategoryID]
        )
    ]

    static let sampleCategories: [Category] = [
        Category(
            id: sampleCategoryID,
            name: "Favorites",
            stationIDs: [sampleStationID]
        )
    ]
}
