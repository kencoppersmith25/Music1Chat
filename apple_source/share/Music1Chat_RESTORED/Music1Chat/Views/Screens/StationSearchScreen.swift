import SwiftUI

struct StationSearchScreen: View {
    @EnvironmentObject private var player: AudioPlayerService
    @Environment(\.dismiss) private var dismiss

    @State private var searchText: String
    @State private var isSearching = false
    @State private var isFindingPlayableStation = false
    @State private var errorMessage: String?
    @State private var didAutoSearch = false

    private let autoSearchOnAppear: Bool
    private let service = RadioBrowserService()

    init(
        initialSearchText: String = "",
        autoSearchOnAppear: Bool = false
    ) {
        _searchText = State(initialValue: initialSearchText)
        self.autoSearchOnAppear = autoSearchOnAppear
    }

    var body: some View {
        VStack(spacing: 20) {
            Spacer()

            if isSearching {
                ProgressView()
                .controlSize(.large)

                Text("Searching")
                    .font(.headline)
            } else if isFindingPlayableStation {
                ProgressView()
                    .controlSize(.large)

                Text("Finding a playable station")
                    .font(.headline)

                Text("Your current station will keep playing until a replacement is ready.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            } else {
                ContentUnavailableView(
                    "Find Radio Stations",
                    systemImage: "magnifyingglass",
                    description: Text(
                        "Search for something such as 80s, jazz, classical, or Hawaii."
                    )
                )
            }

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .navigationTitle("Search")
        .searchable(
            text: $searchText,
            prompt: "Search stations"
        )
        .onSubmit(of: .search) {
            performSearch()
        }
        .onAppear {
            configureAuditionCallbacks()

            guard autoSearchOnAppear,
                  !didAutoSearch else {
                return
            }

            didAutoSearch = true
            performSearch()
        }
        .onDisappear {
            player.onAuditionSucceeded = nil
            player.onAuditionFailed = nil
            player.cancelQueueAudition()
        }
        .alert(
            "Search",
            isPresented: errorIsPresented
        ) {
            Button("OK") {
                errorMessage = nil
                dismiss()
            }
        } message: {
            Text(errorMessage ?? "The search could not be completed.")
        }
    }

    private var errorIsPresented: Binding<Bool> {
        Binding(
            get: { errorMessage != nil },
            set: { newValue in
                if !newValue {
                    errorMessage = nil
                }
            }
        )
    }

    private func configureAuditionCallbacks() {
        player.onAuditionSucceeded = {
            isFindingPlayableStation = false
            dismiss()
        }

        player.onAuditionFailed = { message in
            isFindingPlayableStation = false
            errorMessage = message
        }
    }

    private func performSearch() {
        Task {
            let trimmed = searchText.trimmingCharacters(in: .whitespacesAndNewlines)

            guard !trimmed.isEmpty else {
                errorMessage = "Please enter something to search for."
                return
            }

            isSearching = true
            isFindingPlayableStation = false
            errorMessage = nil

            do {
                let results = try await service.searchStations(for: trimmed)
                isSearching = false

                guard !results.isEmpty else {
                    errorMessage = "No stations matched \(trimmed)."
                    return
                }

                isFindingPlayableStation = true

                player.audition(
                    queue: results.map(\.asStation),
                    name: trimmed,
                    startAt: 0
                )
            } catch {
                isSearching = false
                isFindingPlayableStation = false
                errorMessage = error.localizedDescription
            }
        }
    }
}

#Preview {
    NavigationStack {
        StationSearchScreen()
            .environmentObject(AudioPlayerService())
    }
}
