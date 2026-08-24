//
//  Station.swift
//  Music1Chat
//
//  Created by kenneth coppersmith on 7/21/26.
//

import Foundation

struct Station: Identifiable, Codable, Hashable {
    let id: UUID
    var name: String
    var streamURL: String
    var categoryIDs: [UUID]
    var isFavorite: Bool
    var artworkURL: String?

    init(
        id: UUID = UUID(),
        name: String,
        streamURL: String,
        categoryIDs: [UUID] = [],
        isFavorite: Bool = false,
        artworkURL: String? = nil
    ) {
        self.id = id
        self.name = name
        self.streamURL = streamURL
        self.categoryIDs = categoryIDs
        self.isFavorite = isFavorite
        self.artworkURL = artworkURL
    }
}
