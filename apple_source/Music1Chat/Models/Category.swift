//
//  Category.swift
//  Music1Chat
//
//  Created by kenneth coppersmith on 7/21/26.
//

import Foundation

struct Category: Identifiable, Codable, Hashable {
    let id: UUID
    var name: String
    var stationIDs: [UUID]

    init(
        id: UUID = UUID(),
        name: String,
        stationIDs: [UUID] = []
    ) {
        self.id = id
        self.name = name
        self.stationIDs = stationIDs
    }
}
