//
//  CategoryRowView.swift
//  Music1Chat
//
//  Created by kenneth coppersmith on 7/21/26.
//

import SwiftUI

struct CategoryRowView: View {
    let category: Category
    let stationCount: Int

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "square.grid.2x2")
                .font(.title2)
                .frame(width: 36, height: 36)

            VStack(alignment: .leading, spacing: 4) {
                Text(category.name)
                    .font(.headline)

                Text(stationCountText)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()
        }
        .padding(.vertical, 4)
    }

    private var stationCountText: String {
        stationCount == 1 ? "1 station" : "\(stationCount) stations"
    }
}

#Preview {
    CategoryRowView(
        category: Category(name: "Favorites"),
        stationCount: 1
    )
}
