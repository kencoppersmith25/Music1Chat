import Foundation

internal struct RadioBrowserStation: Identifiable, Decodable, Hashable {
    let stationuuid: String?
    let name: String?
    let url: String?
    let url_resolved: String?
    let favicon: String?
    let country: String?
    let tags: String?

    var id: String { stationuuid ?? UUID().uuidString }

    var asStation: Station {
        let cleanedName = name?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "Unknown Station"
        let stream = (url_resolved ?? url)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let cleanedArtwork = favicon?
            .trimmingCharacters(in: .whitespacesAndNewlines)

        return Station(
            id: UUID(uuidString: stationuuid ?? "") ?? UUID(),
            name: cleanedName,
            streamURL: stream,
            artworkURL: (cleanedArtwork?.isEmpty == false) ? cleanedArtwork : nil
        )
    }
}
