//
//  StationRowView.swift
//  Music1Chat
//
//  Created by kenneth coppersmith on 7/21/26.
//

import SwiftUI

struct StationRowView: View {
    let station: Station

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "radio")
                .font(.title2)
                .frame(width: 36, height: 36)

            VStack(alignment: .leading, spacing: 4) {
                Text(station.name)
                    .font(.headline)

                Text(station.streamURL)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer()

            if station.isFavorite {
                Image(systemName: "star.fill")
                    .foregroundStyle(.yellow)
            }
        }
        .padding(.vertical, 4)
    }
}

#Preview {
    StationRowView(
        station: Station(
            name: "Sample Radio",
            streamURL: "https://example.com/stream",
            isFavorite: true
        )
    )
}
