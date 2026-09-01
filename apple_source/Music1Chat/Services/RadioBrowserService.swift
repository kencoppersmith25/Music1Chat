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
    // Dedicated, stable server mirrors (removing the flaky round-robin DNS entry)
    private let baseURLs: [URL] = [
        "https://de1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info",
        "https://at1.api.radio-browser.info",
        "https://fr1.api.radio-browser.info"
    ].compactMap { URL(string: $0) }

    private let session: URLSession

    init() {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 6
        configuration.timeoutIntervalForResource = 10
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
        RideLogger.shared.log("SEARCH_START: query='\(trimmed)' limit=\(safeLimit)")

        // Parallel fetch with individual error isolation so one dead server never cancels others
        return try await withTaskGroup(of: [RadioBrowserStation]?.self) { group in
            for url in baseURLs {
                group.addTask {
                    do {
                        return try await self.search(on: url, text: trimmed, finalLimit: safeLimit)
                    } catch {
                        RideLogger.shared.log("SEARCH_MIRROR_FAILED: server='\(url.host ?? "")' error='\(error.localizedDescription)'")
                        return nil
                    }
                }
            }

            for await result in group {
                if let stations = result, !stations.isEmpty {
                    RideLogger.shared.log("SEARCH_SUCCESS: stations=\(stations.count)")
                    group.cancelAll()
                    return stations
                }
            }

            return []
        }
    }

    private func search(
        on baseURL: URL,
        text: String,
        finalLimit: Int
    ) async throws -> [RadioBrowserStation] {
        let normalized = text.lowercased()
        var collected: [RadioBrowserStation] = []

        // Fetch sub-queries independently without failing the entire server attempt
        await withTaskGroup(of: [RadioBrowserStation]?.self) { group in
            group.addTask {
                try? await self.fetch(baseURL: baseURL, field: "name", text: text, limit: finalLimit)
            }
            group.addTask {
                try? await self.fetch(baseURL: baseURL, field: "tag", text: text, limit: finalLimit)
            }

            if normalized.contains("hawai") {
                group.addTask {
                    try? await self.fetch(baseURL: baseURL, field: "state", text: "Hawaii", limit: 30)
                }
                group.addTask {
                    try? await self.fetch(baseURL: baseURL, field: "tag", text: "hawaiian", limit: 30)
                }
            }

            for await results in group {
                if let validResults = results {
                    collected.append(contentsOf: validResults)
                }
            }
        }

        // 1. Filter out invalid records
        let validStations = collected.filter { station in
            let cleanedName = station.name?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let rawURL = station.url_resolved ?? station.url ?? ""
            let cleanedURL = rawURL.trimmingCharacters(in: .whitespacesAndNewlines)
            return !cleanedName.isEmpty && !cleanedURL.isEmpty
        }

        // 2. Deduplicate by UUID
        var seenUUIDs = Set<String>()
        let deduplicated = validStations.filter { station in
            let uuid = station.stationuuid?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() ?? ""
            let hasValidUUID = !uuid.isEmpty && uuid != "00000000-0000-0000-0000-000000000000"

            if hasValidUUID {
                if seenUUIDs.contains(uuid) {
                    return false
                } else {
                    seenUUIDs.insert(uuid)
                    return true
                }
            } else {
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

        return result.compactMap { $0 }
    }

    private func fetch(
        baseURL: URL,
        field: String,
        text: String,
        limit: Int
    ) async throws -> [RadioBrowserStation] {
        let path = "/json/stations/search"
        let queryItems = [
            URLQueryItem(name: field, value: text),
            URLQueryItem(name: "\(field)Exact", value: "false"),
            URLQueryItem(name: "hidebroken", value: "true"),
            URLQueryItem(name: "order", value: "clickcount"),
            URLQueryItem(name: "reverse", value: "true"),
            URLQueryItem(name: "limit", value: String(limit))
        ]

        var url = baseURL.appendingPathComponent(path)
        if #available(iOS 16.0, *) {
            url = url.appending(queryItems: queryItems)
        } else {
            var components = URLComponents(url: url, resolvingAgainstBaseURL: false)
            components?.queryItems = queryItems
            if let finalURL = components?.url {
                url = finalURL
            }
        }

        var request = URLRequest(url: url)
        request.timeoutInterval = 6
        request.setValue("Music1Chat/1.0 iOS Radio Resolver", forHTTPHeaderField: "User-Agent")
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