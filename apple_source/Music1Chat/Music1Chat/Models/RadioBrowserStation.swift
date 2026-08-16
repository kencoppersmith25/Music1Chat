//
//  RadioBrowserStation.swift
//  Music1Chat
//
//  Created by kenneth coppersmith on 7/21/26.
//

import Foundation

internal struct RadioBrowserStation: Identifiable, Decodable, Hashable {
    let stationuuid: String
    let name: String
    let url: String
    let urlResolved: String?
    let favicon: String?
    let country: String?
    let tags: String?

    var id: String { stationuuid }

    enum CodingKeys: String, CodingKey {
        case stationuuid
        case name
        case url
        case urlResolved = "url_resolved"
        case favicon
        case country
        case tags
    }

    var asStation: Station {
        let cleanedArtwork = favicon?
            .trimmingCharacters(in: .whitespacesAndNewlines)

        return Station(
            id: UUID(uuidString: stationuuid) ?? UUID(),
            name: name.trimmingCharacters(in: .whitespacesAndNewlines),
            streamURL: (urlResolved ?? url).trimmingCharacters(in: .whitespacesAndNewlines),
            artworkURL: (cleanedArtwork?.isEmpty == false) ? cleanedArtwork : nil
        )
    }
}
