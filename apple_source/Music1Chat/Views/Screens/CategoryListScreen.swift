import SwiftUI

struct CategoryListScreen: View {
    @ObservedObject var library: MusicLibraryViewModel
    @EnvironmentObject private var player: AudioPlayerService

    let compactMode: Bool
    let onDismiss: (() -> Void)?
    let onOpenSearchStations: ((String) -> Void)?
    let onOpenLibraryStations: ((UUID) -> Void)?
    let onDeleteSearchCategory: ((String) -> Void)?
    let onDeleteLibraryCategory: ((Category) -> Void)?

    @State private var pendingDeleteSearchName: String?
    @State private var pendingDeleteCategory: Category?

    init(
        library: MusicLibraryViewModel,
        compactMode: Bool = false,
        onDismiss: (() -> Void)? = nil,
        onOpenSearchStations: ((String) -> Void)? = nil,
        onOpenLibraryStations: ((UUID) -> Void)? = nil,
        onDeleteSearchCategory: ((String) -> Void)? = nil,
        onDeleteLibraryCategory: ((Category) -> Void)? = nil
    ) {
        self.library = library
        self.compactMode = compactMode
        self.onDismiss = onDismiss
        self.onOpenSearchStations = onOpenSearchStations
        self.onOpenLibraryStations = onOpenLibraryStations
        self.onDeleteSearchCategory = onDeleteSearchCategory
        self.onDeleteLibraryCategory = onDeleteLibraryCategory
    }

    var body: some View {
        VStack(spacing: 0) {
            if compactMode {
                HStack {
                    Text("Categories")
                        .font(.headline)
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 13)
                Divider()
            }

            List {
                if player.savedSearchQueues.isEmpty && library.categories.isEmpty {
                    ContentUnavailableView(
                        "No Categories Yet",
                        systemImage: "magnifyingglass",
                        description: Text("Search for stations to get started.")
                    )
                }

                ForEach(player.savedSearchQueues) { saved in
                    categoryRow(
                        title: "Search: \(saved.name)",
                        subtitle: "\(saved.stations.count) stations",
                        active: player.activeLibraryCategoryID == nil &&
                               player.activeQueueName?.caseInsensitiveCompare(saved.name) == .orderedSame,
                        navigationEnabled: player.isSearchNavigationEnabled(named: saved.name),
                        play: {
                            player.auditionSavedSearchQueue(named: saved.name)
                            onDismiss?()
                        },
                        toggleNavigation: {
                            player.toggleSearchNavigation(named: saved.name)
                        },
                        openList: {
                            onOpenSearchStations?(saved.name)
                        },
                        delete: {
                            pendingDeleteSearchName = saved.name
                        }
                    )
                }

                ForEach(library.categories) { category in
                    categoryRow(
                        title: category.name,
                        subtitle: "\(library.stations(in: category).count) stations",
                        active: player.activeLibraryCategoryID == category.id,
                        navigationEnabled: player.isCategoryNavigationEnabled(categoryID: category.id),
                        play: {
                            let stations = library.stations(in: category)
                            guard !stations.isEmpty else { return }
                            player.audition(
                                queue: stations,
                                name: category.name,
                                libraryCategoryID: category.id,
                                startAt: 0,
                                step: 1,
                                saveAsSearch: false,
                                statusMessage: "Finding category"
                            )
                            onDismiss?()
                        },
                        toggleNavigation: {
                            player.toggleCategoryNavigation(categoryID: category.id)
                        },
                        openList: {
                            onOpenLibraryStations?(category.id)
                        },
                        delete: {
                            pendingDeleteCategory = category
                        }
                    )
                }
            }
            .listStyle(.plain)
        }
        .navigationTitle(compactMode ? "" : "Categories")
        .alert(
            "Delete Search: \(pendingDeleteSearchName ?? "")?",
            isPresented: Binding(
                get: { pendingDeleteSearchName != nil },
                set: { if !$0 { pendingDeleteSearchName = nil } }
            )
        ) {
            Button("Cancel", role: .cancel) { pendingDeleteSearchName = nil }
            Button("Delete", role: .destructive) {
                if let name = pendingDeleteSearchName {
                    if let onDeleteSearchCategory {
                        onDeleteSearchCategory(name)
                    } else {
                        player.removeSavedSearchQueue(named: name)
                    }
                }
                pendingDeleteSearchName = nil
            }
        } message: {
            Text("Delete this search category? This does not delete any stations saved in static categories.")
        }
        .alert(
            "Delete \(pendingDeleteCategory?.name ?? "Category")?",
            isPresented: Binding(
                get: { pendingDeleteCategory != nil },
                set: { if !$0 { pendingDeleteCategory = nil } }
            )
        ) {
            Button("Cancel", role: .cancel) { pendingDeleteCategory = nil }
            Button("Delete", role: .destructive) {
                if let category = pendingDeleteCategory {
                    if let onDeleteLibraryCategory {
                        onDeleteLibraryCategory(category)
                    } else {
                        library.deleteCategory(category)
                    }
                }
                pendingDeleteCategory = nil
            }
        } message: {
            Text("Delete this category? The stations themselves will remain available elsewhere.")
        }
    }

    private func categoryRow(
        title: String,
        subtitle: String,
        active: Bool,
        navigationEnabled: Bool,
        play: @escaping () -> Void,
        toggleNavigation: @escaping () -> Void,
        openList: @escaping () -> Void,
        delete: @escaping () -> Void
    ) -> some View {
        HStack(spacing: 10) {
            Button(action: play) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(.headline)
                        .foregroundStyle(.primary)
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .buttonStyle(.plain)

            if active {
                Image(systemName: "speaker.wave.2.fill")
                    .foregroundStyle(.green)
            }

            Button(action: openList) {
                Image(systemName: "list.bullet")
                    .frame(width: 30, height: 30)
            }
            .buttonStyle(.plain)

            Button(role: .destructive, action: delete) {
                Image(systemName: "trash")
                    .frame(width: 30, height: 30)
            }
            .buttonStyle(.plain)

            Button(action: toggleNavigation) {
                NavigationArrowIndicator(enabled: navigationEnabled)
                    .frame(width: 44, height: 30)
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 3)
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
