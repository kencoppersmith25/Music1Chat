import Foundation

struct RadioBrowserStationDTO: Decodable, Sendable {
    let stationuuid: String
    let name: String
    let url: String
    let url_resolved: String?
    let favicon: String?
    let tags: String?
    let countrycode: String?
    let state: String?
    let language: String?
    let codec: String?
    let bitrate: Int?
    let votes: Int?
    let clickcount: Int?

    var asStation: Station {
        let stream = (url_resolved?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == false)
            ? url_resolved!
            : url

        return Station(
            id: UUID(),
            name: name.trimmingCharacters(in: .whitespacesAndNewlines),
            streamURL: stream.trimmingCharacters(in: .whitespacesAndNewlines),
            categoryIDs: [],
            artworkURL: favicon?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty == false ? favicon : nil
        )
    }
}

final class RadioBrowserService: Sendable {
    private let baseUrls = [
        "https://de1.api.radio-browser.info",
        "https://all.api.radio-browser.info",
        "https://de2.api.radio-browser.info",
        "https://fi1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info"
    ]

    private let userAgent = "NoHandsRadio/1.1 iOS"

    func searchStations(for query: String, limit: Int = 100) async throws -> [RadioBrowserStationDTO] {
        let searchText = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !searchText.isEmpty else { return [] }

        let safeLimit = min(max(limit, 1), 200)
        var hadDirectoryFailure = false

        for baseUrl in baseUrls {
            do {
                let results = try await executeMultiSearch(baseUrl: baseUrl, text: searchText, finalLimit: safeLimit)
                if !results.isEmpty {
                    return results
                }
            } catch {
                hadDirectoryFailure = true
            }
        }

        if hadDirectoryFailure {
            throw NSError(domain: "RadioBrowserService", code: 503, userInfo: [NSLocalizedDescriptionKey: "Station search is temporarily unavailable. Please try again."])
        }

        return []
    }

    private func executeMultiSearch(baseUrl: String, text: String, finalLimit: Int) async throws -> [RadioBrowserStationDTO] {
        let normalized = text.lowercased()

        async let nameSearch = fetch(baseUrl: baseUrl, field: "name", text: text, limit: 100)
        async let tagSearch = fetch(baseUrl: baseUrl, field: "tag", text: text, limit: 100)

        var collected = await (nameSearch + tagSearch)

        // Synergy Hawaii search matching Android
        if normalized.contains("hawai") {
            async let s1 = fetch(baseUrl: baseUrl, field: "state", text: "Hawaii", limit: 100)
            async let s2 = fetch(baseUrl: baseUrl, field: "tag", text: "hawaiian", limit: 100)
            async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Honolulu", limit: 60)
            async let s4 = fetch(baseUrl: baseUrl, field: "name", text: "Maui", limit: 50)
            async let s5 = fetch(baseUrl: baseUrl, field: "name", text: "Kauai", limit: 40)
            async let s6 = fetch(baseUrl: baseUrl, field: "name", text: "Kona", limit: 40)
            async let s7 = fetch(baseUrl: baseUrl, field: "name", text: "Aloha", limit: 40)
            async let s8 = fetch(baseUrl: baseUrl, field: "name", text: "Hawaii Music Live", limit: 30)

            let hawaiiResults = await (s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8)
            collected.append(contentsOf: hawaiiResults)
        }

        let validStations = collected.filter { station in
            let cleanedName = station.name.trimmingCharacters(in: .whitespacesAndNewlines)
            let playbackUrl = (station.url_resolved?.isEmpty == false ? station.url_resolved! : station.url).trimmingCharacters(in: .whitespacesAndNewlines)
            return !cleanedName.isEmpty && !playbackUrl.isEmpty
        }

        // Deduplicate primarily by Station UUID and Exact Stream URL
        var seenUuids = Set<String>()
        var seenUrls = Set<String>()

        let uniqueStations = validStations.filter { station in
            let uuid = station.stationuuid.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            let hasValidUuid = !uuid.isEmpty && uuid != "00000000-0000-0000-0000-000000000000"

            let rawUrl = (station.url_resolved?.isEmpty == false ? station.url_resolved! : station.url).trimmingCharacters(in: .whitespacesAndNewlines).lowercased()

            let isUuidDuplicate = hasValidUuid && seenUuids.contains(uuid)
            let isUrlDuplicate = seenUrls.contains(rawUrl)

            if isUuidDuplicate || isUrlDuplicate {
                return false
            } else {
                if hasValidUuid { seenUuids.insert(uuid) }
                seenUrls.insert(rawUrl)
                return true
            }
        }

        // Rank by relevance matching Android LiveStationSearchEngine
        let normalizedQuery = normalize(text)
        let queryWords = tokenize(text)

        let ranked = uniqueStations.compactMap { station -> (station: RadioBrowserStationDTO, score: Int)? in
            let score = calculateRelevanceScore(station: station, normalizedQuery: normalizedQuery, queryWords: queryWords)
            return score > 0 ? (station, score) : nil
        }
        .sorted {
            if $0.score != $1.score { return $0.score > $1.score }
            if ($0.station.votes ?? 0) != ($1.station.votes ?? 0) { return ($0.station.votes ?? 0) > ($1.station.votes ?? 0) }
            return ($0.station.clickcount ?? 0) > ($1.station.clickcount ?? 0)
        }
        .map(\.station)

        let limited = Array(ranked.prefix(finalLimit))
        return interleaveStations(limited)
    }

    // Android Stride-7 Interleaver
    private func interleaveStations(_ stations: [RadioBrowserStationDTO]) -> [RadioBrowserStationDTO] {
        let count = stations.count
        guard count > 1 else { return stations }

        let stride = 7
        var result: [RadioBrowserStationDTO?] = Array(repeating: nil, count: count)

        for i in 0..<count {
            let targetIndex = ((i * stride) % count) + ((i * stride) / count)
            let safeIndex = min(max(targetIndex, 0), count - 1)

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

    private func fetch(baseUrl: String, field: String, text: String, limit: Int) async -> [RadioBrowserStationDTO] {
        guard let encodedText = text.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
              let url = URL(string: "\(baseUrl)/json/stations/search?\(field)=\(encodedText)&\(field)Exact=false&order=votes&reverse=true&limit=\(limit)") else {
            return []
        }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.timeoutInterval = 8.0
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
                return []
            }
            return try JSONDecoder().decode([RadioBrowserStationDTO].self, from: data)
        } catch {
            return []
        }
    }

    private func calculateRelevanceScore(station: RadioBrowserStationDTO, normalizedQuery: String, queryWords: [String]) -> Int {
        let normalizedName = normalize(station.name)
        let normalizedTags = normalize(station.tags ?? "")
        let normalizedState = normalize(station.state ?? "")
        let normalizedCountry = normalize(station.countrycode ?? "")
        let normalizedLanguage = normalize(station.language ?? "")

        guard !normalizedName.isEmpty else { return 0 }

        var score = 100

        if normalizedName == normalizedQuery {
            score += 1000
        } else if normalizedName.hasPrefix(normalizedQuery) {
            score += 800
        } else if normalizedName.contains(normalizedQuery) {
            score += 650
        } else if normalizedTags == normalizedQuery {
            score += 550
        } else if normalizedTags.contains(normalizedQuery) {
            score += 500
        } else if normalizedState.contains(normalizedQuery) {
            score += 700
        }

        if !queryWords.isEmpty {
            let nameWordMatches = queryWords.filter { normalizedName.contains($0) }.count
            let tagWordMatches = queryWords.filter { normalizedTags.contains($0) }.count
            let locationWordMatches = queryWords.filter { normalizedState.contains($0) || normalizedCountry.contains($0) }.count
            let languageWordMatches = queryWords.filter { normalizedLanguage.contains($0) }.count

            score += nameWordMatches * 140
            score += tagWordMatches * 90
            score += locationWordMatches * 70
            score += languageWordMatches * 60
        }

        if let resolved = station.url_resolved, !resolved.isEmpty {
            score += 25
        }

        score += min(station.votes ?? 0, 500) / 20
        score += min(station.clickcount ?? 0, 2000) / 100

        return score
    }

    private func normalize(_ value: String) -> String {
        value
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
            .replacingOccurrences(of: "[^a-z0-9]+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
    }

    private func tokenize(_ value: String) -> [String] {
        let genericWords: Set<String> = ["radio", "station", "music", "live", "online", "stream", "fm", "am", "the"]
        return normalize(value)
            .split(separator: " ")
            .map(String.init)
            .filter { $0.count >= 2 && !genericWords.contains($0) }
    }
}