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
    ].compactMap(URL.init(string:))

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

                // A successful search is authoritative, even when it
                // legitimately produces zero stations.
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

        // Search name and tag independently.
        // Do not apply the final station limit to either search here.
        async let nameResults = fetch(
            baseURL: baseURL,
            field: "name",
            text: text
        )

        async let tagResults = fetch(
            baseURL: baseURL,
            field: "tag",
            text: text
        )

        var collected: [RadioBrowserStation] = []
        var successfulRequestCount = 0

        do {
            collected.append(contentsOf: try await nameResults)
            successfulRequestCount += 1
        } catch is CancellationError {
            throw CancellationError()
        } catch {
            // Allow the tag search to provide results if name search fails.
        }

        do {
            collected.append(contentsOf: try await tagResults)
            successfulRequestCount += 1
        } catch is CancellationError {
            throw CancellationError()
        } catch {
            // Allow the name search to provide results if tag search fails.
        }

        guard successfulRequestCount > 0 else {
            throw RadioBrowserServiceError.temporarilyUnavailable
        }

        // Remove invalid records, but do not limit the result set.
        let validStations = collected.filter { station in
            let name = station.name.trimmingCharacters(
                in: .whitespacesAndNewlines
            )

            let resolvedURL = station.urlResolved.trimmingCharacters(
                in: .whitespacesAndNewlines
            )

            return !name.isEmpty && !resolvedURL.isEmpty
        }

        // Combine name + tag results first, then deduplicate.
        var seen = Set<String>()

        let deduplicated = validStations.filter { station in
            let uuid = station.stationuuid
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .lowercased()

            let resolvedURL = station.urlResolved
                .trimmingCharacters(in: .whitespacesAndNewlines)
                .lowercased()

            let key = uuid.isEmpty ? resolvedURL : uuid

            return seen.insert(key).inserted
        }

        // IMPORTANT:
        // The final limit is applied only here.
        //
        // Everything before this point is:
        //   name search
        //   + tag search
        //   + validation
        //   + deduplication
        //
        // Nothing is artificially capped along the way.
        return Array(
            deduplicated.prefix(finalLimit)
        )
    }
    

    private func fetch(
        baseURL: URL,
        field: String,
        text: String
    ) async throws -> [RadioBrowserStation] {

        var components = URLComponents(
            url: baseURL.appendingPathComponent(
                "json/stations/search"
            ),
            resolvingAgainstBaseURL: false
        )

        components?.queryItems = [
            URLQueryItem(name: field, value: text),
            URLQueryItem(
                name: "\(field)Exact",
                value: "false"
            ),
            URLQueryItem(
                name: "hidebroken",
                value: "true"
            ),
            URLQueryItem(
                name: "order",
                value: "clickcount"
            ),
            URLQueryItem(
                name: "reverse",
                value: "true"
            )
        ]

        guard let url = components?.url else {
            throw RadioBrowserServiceError.invalidSearch
        }

        var request = URLRequest(url: url)
        request.timeoutInterval = 6
        request.setValue(
            "Music1Chat/1.0 iOS",
            forHTTPHeaderField: "User-Agent"
        )
        request.setValue(
            "application/json",
            forHTTPHeaderField: "Accept"
        )

        let (data, response) = try await session.data(
            for: request
        )

        guard let http = response as? HTTPURLResponse else {
            throw RadioBrowserServiceError.invalidResponse
        }

        guard 200...299 ~= http.statusCode else {
            throw RadioBrowserServiceError.temporarilyUnavailable
        }

        return try JSONDecoder().decode(
            [RadioBrowserStation].self,
            from: data
        )
    }
}
