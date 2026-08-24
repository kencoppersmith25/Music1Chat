import Foundation

enum RadioBrowserServiceError: LocalizedError {
    case invalidSearch
    case temporarilyUnavailable
    case invalidResponse

    var errorDescription: String? {
        switch self {
        case .invalidSearch:
            return "Please enter something to search for."
        case .temporarilyUnavailable:
            return "Station search is temporarily unavailable. Please try again."
        case .invalidResponse:
            return "The station directory returned an invalid response."
        }
    }
}

struct RadioBrowserService {
    private let baseURLs: [URL] = [
        "https://de1.api.radio-browser.info",
        "https://all.api.radio-browser.info",
        "https://de2.api.radio-browser.info",
        "https://fi1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info"
    ].compactMap { (str: String) -> URL? in URL(string: str) }

    private let session: URLSession

    init() {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 6
        configuration.timeoutIntervalForResource = 8
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
        session = URLSession(configuration: configuration)
    }

    func searchStations(
        for searchText: String,
        limit: Int = 50
    ) async throws -> [RadioBrowserStation] {
        let trimmed = searchText.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmed.isEmpty else {
            throw RadioBrowserServiceError.invalidSearch
        }

        let safeLimit = min(max(limit, 1), 100)
        var hadDirectoryFailure = false

        for baseURL in baseURLs {
            if Task.isCancelled {
                throw CancellationError()
            }

            do {
                let stations = try await search(
                    on: baseURL,
                    text: trimmed,
                    finalLimit: safeLimit
                )
                return stations
            } catch is CancellationError {
                throw CancellationError()
            } catch {
                hadDirectoryFailure = true
            }
        }

        if hadDirectoryFailure {
            throw RadioBrowserServiceError.temporarilyUnavailable
        }

        return []
    }

    private func search(
        on baseURL: URL,
        text: String,
        finalLimit: Int
    ) async throws -> [RadioBrowserStation] {
        let normalized = text.lowercased()
        var collected: [RadioBrowserStation] = []
        var successfulRequestCount = 0

        try await withTaskGroup(of: [RadioBrowserStation].self) { group in
            // 1. Core Searches
            group.addTask { (try? await self.fetch(baseURL: baseURL, field: "name", text: text, limit: finalLimit)) ?? [] }
            group.addTask { (try? await self.fetch(baseURL: baseURL, field: "tag", text: text, limit: finalLimit)) ?? [] }

            // 2. Ultra Hawaiian Synergy
            if normalized.contains("hawai") {
                group.addTask { (try? await self.fetch(baseURL: baseURL, field: "state", text: "Hawaii", limit: 30)) ?? [] }
                group.addTask { (try? await self.fetch(baseURL: baseURL, field: "tag", text: "hawaiian", limit: 30)) ?? [] }
                group.addTask { (try? await self.fetch(baseURL: baseURL, field: "name", text: "Honolulu", limit: 30)) ?? [] }
                group.addTask { (try? await self.fetch(baseURL: baseURL, field: "name", text: "Maui", limit: 20)) ?? [] }
                group.addTask { (try? await self.fetch(baseURL: baseURL, field: "name", text: "Kauai", limit: 15)) ?? [] }
                group.addTask { (try? await self.fetch(baseURL: baseURL, field: "name", text: "Kona", limit: 15)) ?? [] }
                group.addTask { (try? await self.fetch(baseURL: baseURL, field: "name", text: "Aloha", limit: 15)) ?? [] }
                group.addTask { (try? await self.fetch(baseURL: baseURL, field: "name", text: "Hawaii Music Live", limit: 10)) ?? [] }
            }

            for await results in group {
                if !results.isEmpty {
                    collected.append(contentsOf: results)
                    successfulRequestCount += 1
                }
            }
        }

        guard successfulRequestCount > 0 else {
            throw RadioBrowserServiceError.temporarilyUnavailable
        }

        // 1. Filter out invalid records
        let validStations = collected.filter { (station: RadioBrowserStation) -> Bool in
            let cleanedName = station.name.trimmingCharacters(in: .whitespacesAndNewlines)
            let rawURL = station.urlResolved ?? station.url
            let cleanedURL = rawURL.trimmingCharacters(in: .whitespacesAndNewlines)
            return !cleanedName.isEmpty && !cleanedURL.isEmpty
        }

        // 2. Combine and deduplicate
        var seen = Set<String>()
        let deduplicated = validStations.filter { (station: RadioBrowserStation) -> Bool in
            let uuid = station.stationuuid.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            let rawURL = station.urlResolved ?? station.url
            let cleanedURL = rawURL.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()

            let key = uuid.isEmpty ? cleanedURL : uuid
            if seen.contains(key) {
                return false
            } else {
                seen.insert(key)
                return true
            }
        }

        // 3. Limit and Interleave
        let limitedResults = Array(deduplicated.prefix(finalLimit))
        return interleaveStations(limitedResults)
    }

    private func interleaveStations(_ stations: [RadioBrowserStation]) -> [RadioBrowserStation] {
        let count = stations.count
        if count <= 1 { return stations }

        let stride = 10
        var result = [RadioBrowserStation?](repeating: nil, count: count)

        for i in 0..<count {
            let targetIndex = ((i * stride) % count) + ((i * stride) / count)
            let safeIndex = max(0, min(targetIndex, count - 1))

            if result[safeIndex] == nil {
                result[safeIndex] = stations[i]
            } else {
                for j in 0..<count {
                    let fallbackIndex = (safeIndex + j) % count
                    if result[fallbackIndex] == nil {
                        result[fallbackIndex] = stations[i]
                        break
                    }
                }
            }
        }

        return result.compactMap { (s: RadioBrowserStation?) -> RadioBrowserStation? in s }
    }

    private func fetch(
        baseURL: URL,
        field: String,
        text: String,
        limit: Int
    ) async throws -> [RadioBrowserStation] {

        var components = URLComponents(
            url: baseURL.appendingPathComponent("json/stations/search"),
            resolvingAgainstBaseURL: false
        )

        components?.queryItems = [
            URLQueryItem(name: field, value: text),
            URLQueryItem(name: "\(field)Exact", value: "false"),
            URLQueryItem(name: "hidebroken", value: "true"),
            URLQueryItem(name: "order", value: "clickcount"),
            URLQueryItem(name: "reverse", value: "true"),
            URLQueryItem(name: "limit", value: "\(limit)")
        ]

        guard let url = components?.url else {
            throw RadioBrowserServiceError.invalidSearch
        }

        var request = URLRequest(url: url)
        request.timeoutInterval = 6
        request.setValue("NoHandsRadio/1.1 iOS", forHTTPHeaderField: "User-Agent")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let (data, response) = try await session.data(for: request)

        guard let http = response as? HTTPURLResponse else {
            throw RadioBrowserServiceError.invalidResponse
        }

        guard 200...299 ~= http.statusCode else {
            throw RadioBrowserServiceError.temporarilyUnavailable
        }

        return try JSONDecoder().decode([RadioBrowserStation].self, from: data)
    }
}
