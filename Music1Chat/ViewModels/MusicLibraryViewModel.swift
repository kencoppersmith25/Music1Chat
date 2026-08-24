import Foundation
import Combine

@MainActor
final class MusicLibraryViewModel: ObservableObject {
    @Published private(set) var categories: [Category] = []
    @Published private(set) var stations: [Station] = []
    @Published private(set) var persistenceReady = false

    private let persistenceKey = "NoHandsRadio.Library.v1"
    private var isRestoring = false

    init() {
        restoreLibrary()

        // FORCE UPGRADE "Reliable Stations" to Music-only version
        let reliableName = "Reliable Stations"
        if let existingIdx = categories.firstIndex(where: { $0.name == reliableName }) {
            let oldStations = stations.filter { station in
                station.categoryIDs.contains(categories[existingIdx].id)
            }
            // If the old BBC talk station is found, we swap the whole set
            if oldStations.contains(where: { $0.name.contains("BBC") }) {
                deleteCategory(categories[existingIdx])
                createReliableStarterPack()
            }
        } else if categories.isEmpty {
            createReliableStarterPack()
        }

        persistenceReady = true
        persistIfReady()
    }

    private func createReliableStarterPack() {
        let musicStations = [
            Station(name: "Jazz24", streamURL: "https://kexp-jazz24.streamguys1.com/jazz24.mp3", artworkURL: "https://www.jazz24.org/wp-content/themes/jazz24/images/jazz24-logo-og.png"),
            Station(name: "SomaFM Groove Salad", streamURL: "https://ice1.somafm.com/groovesalad-256-mp3", artworkURL: "https://somafm.com/img/gs256.png"),
            Station(name: "Planet Rock", streamURL: "https://icecast.bauermedia.co.uk/planetrock.mp3", artworkURL: "https://upload.wikimedia.org/wikipedia/en/2/23/Planet_Rock_Logo.png"),
            Station(name: "HITS 80s", streamURL: "https://streaming.radio.co/s514757c93/listen", artworkURL: "https://az827626.vo.msecnd.net/cdn/stationlogos/hits80s.png"),
            Station(name: "Swiss Classic", streamURL: "https://stream.srg-ssr.ch/m/rsc_de/mp3_128", artworkURL: "https://www.radioswissclassic.ch/images/rsc_logo_square.png")
        ]

        self.stations.append(contentsOf: musicStations)
        let reliableCategory = Category(name: "Reliable Stations", stationIDs: musicStations.map { $0.id })
        self.categories.append(reliableCategory)

        for i in self.stations.indices {
            if musicStations.contains(where: { $0.id == self.stations[i].id }) {
                self.stations[i].categoryIDs = [reliableCategory.id]
            }
        }
    }

    func stations(in category: Category) -> [Station] {
        stations.filter { category.stationIDs.contains($0.id) }
    }

    func moveStation(in category: Category, stationID: UUID, toIndex: Int) {
        guard let cIdx = categories.firstIndex(where: { $0.id == category.id }) else { return }

        var ids = categories[cIdx].stationIDs
        guard let fromIndex = ids.firstIndex(of: stationID) else { return }

        ids.remove(at: fromIndex)
        let safeToIndex = min(max(toIndex, 0), ids.count)
        ids.insert(stationID, at: safeToIndex)

        categories[cIdx].stationIDs = ids
        persistIfReady()
    }

    func saveStation(_ station: Station, toCategoryNamed categoryName: String) {
        let trimmedName = categoryName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else { return }

        var stationToSave = station

        if let existingIdx = categories.firstIndex(where: { $0.name.caseInsensitiveCompare(trimmedName) == .orderedSame }) {
            if !categories[existingIdx].stationIDs.contains(station.id) {
                categories[existingIdx].stationIDs.append(station.id)
                if let sIdx = stations.firstIndex(where: { $0.id == station.id }) {
                    stations[sIdx].categoryIDs.append(categories[existingIdx].id)
                } else {
                    stationToSave.categoryIDs.append(categories[existingIdx].id)
                    stations.append(stationToSave)
                }
            }
        } else {
            let newCategory = Category(name: trimmedName, stationIDs: [station.id])
            categories.append(newCategory)
            if let sIdx = stations.firstIndex(where: { $0.id == station.id }) {
                stations[sIdx].categoryIDs.append(newCategory.id)
            } else {
                stationToSave.categoryIDs.append(newCategory.id)
                stations.append(stationToSave)
            }
        }

        persistIfReady()
    }

    func removeStation(_ station: Station, from category: Category) {
        guard let cIdx = categories.firstIndex(where: { $0.id == category.id }) else { return }

        categories[cIdx].stationIDs.removeAll { $0 == station.id }

        if let sIdx = stations.firstIndex(where: { $0.id == station.id }) {
            stations[sIdx].categoryIDs.removeAll { $0 == category.id }
            if stations[sIdx].categoryIDs.isEmpty {
                stations.remove(at: sIdx)
            }
        }

        if categories[cIdx].stationIDs.isEmpty {
            categories.remove(at: cIdx)
        }

        persistIfReady()
    }

    func deleteCategory(_ category: Category) {
        categories.removeAll { $0.id == category.id }
        for i in stations.indices.reversed() {
            stations[i].categoryIDs.removeAll { $0 == category.id }
            if stations[i].categoryIDs.isEmpty {
                stations.remove(at: i)
            }
        }
        persistIfReady()
    }

    private func persistIfReady() {
        guard persistenceReady && !isRestoring else { return }
        let state = PersistedLibrary(categories: categories, stations: stations)
        if let data = try? JSONEncoder().encode(state) {
            UserDefaults.standard.set(data, forKey: persistenceKey)
        }
    }

    private func restoreLibrary() {
        isRestoring = true
        defer { isRestoring = false }

        if let data = UserDefaults.standard.data(forKey: persistenceKey),
           let decoded = try? JSONDecoder().decode(PersistedLibrary.self, from: data) {
            self.categories = decoded.categories
            self.stations = decoded.stations
        }
    }
}

private struct PersistedLibrary: Codable {
    let categories: [Category]
    let stations: [Station]
}
