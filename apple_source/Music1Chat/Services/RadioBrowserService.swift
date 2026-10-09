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
                async let s8 = fetch(baseUrl: baseUrl, field: "name", text: "Hawaii Hawaiian Music Live", limit: 30)

                let hawaiiResults = await (s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8)
                collected.append(contentsOf: hawaiiResults)
            }

            // Synergy Israel search matching Android
            if normalized.contains("israel") {
                async let s1 = fetch(baseUrl: baseUrl, field: "tag", text: "israel", limit: 100)
                async let s2 = fetch(baseUrl: baseUrl, field: "tag", text: "israeli", limit: 100)
                async let s3 = fetch(baseUrl: baseUrl, field: "tag", text: "hebrew", limit: 100)
                async let s4 = fetch(baseUrl: baseUrl, field: "name", text: "Tel Aviv", limit: 60)
                async let s5 = fetch(baseUrl: baseUrl, field: "name", text: "Jerusalem", limit: 50)
                async let s6 = fetch(baseUrl: baseUrl, field: "name", text: "Haifa", limit: 40)
                async let s7 = fetch(baseUrl: baseUrl, field: "name", text: "Galgalatz", limit: 40)

                let israelResults = await (s1 + s2 + s3 + s4 + s5 + s6 + s7)
                collected.append(contentsOf: israelResults)
            }

            // Synergy France search matching Android
            if normalized.contains("franc") || normalized.contains("french") {
                async let s1 = fetch(baseUrl: baseUrl, field: "tag", text: "french", limit: 100)
                async let s2 = fetch(baseUrl: baseUrl, field: "tag", text: "francais", limit: 80)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "France", limit: 60)
                async let s4 = fetch(baseUrl: baseUrl, field: "name", text: "Paris", limit: 60)
                async let s5 = fetch(baseUrl: baseUrl, field: "name", text: "Lyon", limit: 40)
                async let s6 = fetch(baseUrl: baseUrl, field: "name", text: "Marseille", limit: 40)
                async let s7 = fetch(baseUrl: baseUrl, field: "name", text: "Bordeaux", limit: 30)
                async let s8 = fetch(baseUrl: baseUrl, field: "name", text: "Nantes", limit: 30)
                async let s9 = fetch(baseUrl: baseUrl, field: "name", text: "Strasbourg", limit: 30)
                async let s10 = fetch(baseUrl: baseUrl, field: "name", text: "Lille", limit: 30)
                async let s11 = fetch(baseUrl: baseUrl, field: "tag", text: "chanson", limit: 40)
                async let s12 = fetch(baseUrl: baseUrl, field: "name", text: "NRJ", limit: 40)
                async let s13 = fetch(baseUrl: baseUrl, field: "name", text: "Nostalgie", limit: 30)
                async let s14 = fetch(baseUrl: baseUrl, field: "name", text: "Skyrock", limit: 30)

                let franceResults = await (s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8 + s9 + s10 + s11 + s12 + s13 + s14)
                collected.append(contentsOf: franceResults)
            }

            // Synergy Germany search matching Android
            if normalized.contains("german") || normalized.contains("deutsch") {
                async let s1 = fetch(baseUrl: baseUrl, field: "tag", text: "german", limit: 100)
                async let s2 = fetch(baseUrl: baseUrl, field: "tag", text: "deutsch", limit: 80)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Germany", limit: 60)
                async let s4 = fetch(baseUrl: baseUrl, field: "name", text: "Berlin", limit: 60)
                async let s5 = fetch(baseUrl: baseUrl, field: "name", text: "Hamburg", limit: 40)
                async let s6 = fetch(baseUrl: baseUrl, field: "name", text: "Munich", limit: 40)
                async let s7 = fetch(baseUrl: baseUrl, field: "name", text: "München", limit: 40)
                async let s8 = fetch(baseUrl: baseUrl, field: "name", text: "Cologne", limit: 40)
                async let s9 = fetch(baseUrl: baseUrl, field: "name", text: "Köln", limit: 40)
                async let s10 = fetch(baseUrl: baseUrl, field: "name", text: "Frankfurt", limit: 40)
                async let s11 = fetch(baseUrl: baseUrl, field: "name", text: "Antenne", limit: 40)
                async let s12 = fetch(baseUrl: baseUrl, field: "name", text: "WDR", limit: 40)

                let germanyResults = await (s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8 + s9 + s10 + s11 + s12)
                collected.append(contentsOf: germanyResults)
            }

            // Synergy Africa search matching Android
            if normalized.contains("africa") || normalized.contains("nigeria") ||
               normalized.contains("kenya") || normalized.contains("ghana") ||
               normalized.contains("senegal") || normalized.contains("egypt") {

                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "Nigeria", limit: 40)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Lagos", limit: 30)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "South Africa", limit: 40)
                async let s4 = fetch(baseUrl: baseUrl, field: "name", text: "Johannesburg", limit: 30)
                async let s5 = fetch(baseUrl: baseUrl, field: "name", text: "Cape Town", limit: 25)
                async let s6 = fetch(baseUrl: baseUrl, field: "name", text: "Kenya", limit: 30)
                async let s7 = fetch(baseUrl: baseUrl, field: "name", text: "Nairobi", limit: 25)
                async let s8 = fetch(baseUrl: baseUrl, field: "name", text: "Ghana", limit: 25)
                async let s9 = fetch(baseUrl: baseUrl, field: "name", text: "Accra", limit: 20)
                async let s10 = fetch(baseUrl: baseUrl, field: "name", text: "Senegal", limit: 20)
                async let s11 = fetch(baseUrl: baseUrl, field: "name", text: "Dakar", limit: 15)
                async let s12 = fetch(baseUrl: baseUrl, field: "name", text: "Morocco", limit: 20)
                async let s13 = fetch(baseUrl: baseUrl, field: "name", text: "Casablanca", limit: 15)
                async let s14 = fetch(baseUrl: baseUrl, field: "name", text: "Egypt", limit: 20)
                async let s15 = fetch(baseUrl: baseUrl, field: "name", text: "Cairo", limit: 15)
                async let s16 = fetch(baseUrl: baseUrl, field: "name", text: "Tanzania", limit: 15)
                async let s17 = fetch(baseUrl: baseUrl, field: "name", text: "Ethiopia", limit: 15)

                let africaResults = await (s1 + s2 + s3 + s4 + s5 + s6 + s7 + s8 + s9 + s10 + s11 + s12 + s13 + s14 + s15 + s16 + s17)
                collected.append(contentsOf: africaResults)
            }

            // Synergy Puerto Rico search matching Android
            if normalized.contains("puerto rico") || normalized.contains("puertorico") || normalized.contains("puertorican") {
                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "San Juan", limit: 50)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Ponce", limit: 40)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Mayaguez", limit: 40)
                async let s4 = fetch(baseUrl: baseUrl, field: "name", text: "Puerto Rico", limit: 50)
                let results = await (s1 + s2 + s3 + s4)
                collected.append(contentsOf: results)
            }

            // Synergy Norway search matching Android
            if normalized.contains("norway") || normalized.contains("norwegian") {
                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "Oslo", limit: 50)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Bergen", limit: 40)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Trondheim", limit: 40)
                async let s4 = fetch(baseUrl: baseUrl, field: "name", text: "Norway", limit: 50)
                let results = await (s1 + s2 + s3 + s4)
                collected.append(contentsOf: results)
            }

            // Synergy Sweden search matching Android
            if normalized.contains("sweden") || normalized.contains("swedish") {
                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "Stockholm", limit: 50)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Gothenburg", limit: 40)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Malmo", limit: 40)
                async let s4 = fetch(baseUrl: baseUrl, field: "name", text: "Sweden", limit: 50)
                let results = await (s1 + s2 + s3 + s4)
                collected.append(contentsOf: results)
            }

            // Synergy India search matching Android
            if normalized.contains("india") || normalized.contains("indian") {
                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "Mumbai", limit: 50)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Delhi", limit: 40)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Bangalore", limit: 40)
                async let s4 = fetch(baseUrl: baseUrl, field: "name", text: "India", limit: 50)
                let results = await (s1 + s2 + s3 + s4)
                collected.append(contentsOf: results)
            }

            // Synergy China search matching Android
            if normalized.contains("china") || normalized.contains("chinese") {
                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "Beijing", limit: 50)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Shanghai", limit: 40)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Hong Kong", limit: 40)
                async let s4 = fetch(baseUrl: baseUrl, field: "name", text: "China", limit: 50)
                let results = await (s1 + s2 + s3 + s4)
                collected.append(contentsOf: results)
            }

            // Synergy Japan search matching Android
            if normalized.contains("japan") || normalized.contains("japanese") {
                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "Tokyo", limit: 50)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Osaka", limit: 40)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Kyoto", limit: 40)
                async let s4 = fetch(baseUrl: baseUrl, field: "name", text: "Japan", limit: 50)
                let results = await (s1 + s2 + s3 + s4)
                collected.append(contentsOf: results)
            }

            // Synergy Brazil search matching Android
            if normalized.contains("brazil") || normalized.contains("brazilian") {
                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "Rio de Janeiro", limit: 50)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Sao Paulo", limit: 40)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Salvador", limit: 40)
                async let s4 = fetch(baseUrl: baseUrl, field: "name", text: "Brazil", limit: 50)
                let results = await (s1 + s2 + s3 + s4)
                collected.append(contentsOf: results)
            }

            // Synergy Colombia search matching Android
            if normalized.contains("colombia") || normalized.contains("colombian") {
                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "Bogota", limit: 50)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Medellin", limit: 40)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Colombia", limit: 50)
                let results = await (s1 + s2 + s3)
                collected.append(contentsOf: results)
            }

            // Synergy Argentina search matching Android
            if normalized.contains("argentina") {
                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "Buenos Aires", limit: 50)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Cordoba", limit: 40)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Argentina", limit: 50)
                let results = await (s1 + s2 + s3)
                collected.append(contentsOf: results)
            }

            // Synergy United Kingdom search matching Android
            if normalized.contains("united kingdom") || normalized.contains("uk") || normalized.contains("british") {
                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "London", limit: 50)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Manchester", limit: 40)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Birmingham", limit: 40)
                async let s4 = fetch(baseUrl: baseUrl, field: "name", text: "United Kingdom", limit: 50)
                let results = await (s1 + s2 + s3 + s4)
                collected.append(contentsOf: results)
            }

            // Synergy Italy search matching Android
            if normalized.contains("italy") || normalized.contains("italian") {
                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "Rome", limit: 50)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Milan", limit: 40)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Naples", limit: 40)
                async let s4 = fetch(baseUrl: baseUrl, field: "name", text: "Italy", limit: 50)
                let results = await (s1 + s2 + s3 + s4)
                collected.append(contentsOf: results)
            }

            // Synergy Spain search matching Android
            if normalized.contains("spain") || normalized.contains("spanish") {
                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "Madrid", limit: 50)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Barcelona", limit: 40)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Spain", limit: 50)
                let results = await (s1 + s2 + s3)
                collected.append(contentsOf: results)
            }

            // Synergy South Korea search matching Android
            if normalized.contains("south korea") || normalized.contains("korean") {
                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "Seoul", limit: 50)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Busan", limit: 40)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Korea", limit: 50)
                let results = await (s1 + s2 + s3)
                collected.append(contentsOf: results)
            }

            // Synergy Philippines search matching Android
            if normalized.contains("philippines") || normalized.contains("filipino") {
                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "Manila", limit: 50)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Cebu", limit: 40)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Philippines", limit: 50)
                let results = await (s1 + s2 + s3)
                collected.append(contentsOf: results)
            }

            // Synergy Australia search matching Android
            if normalized.contains("australia") || normalized.contains("australian") {
                async let s1 = fetch(baseUrl: baseUrl, field: "name", text: "Sydney", limit: 50)
                async let s2 = fetch(baseUrl: baseUrl, field: "name", text: "Melbourne", limit: 40)
                async let s3 = fetch(baseUrl: baseUrl, field: "name", text: "Brisbane", limit: 40)
                async let s4 = fetch(baseUrl: baseUrl, field: "name", text: "Australia", limit: 50)
                let results = await (s1 + s2 + s3 + s4)
                collected.append(contentsOf: results)
            }

            let validStations = collected.filter { station in
                let cleanedName = station.name.trimmingCharacters(in: .whitespacesAndNewlines)
                let playbackUrl = (station.url_resolved?.isEmpty == false ? station.url_resolved! : station.url).trimmingCharacters(in: .whitespacesAndNewlines)
                return !cleanedName.isEmpty && !playbackUrl.isEmpty
            }

            // Deduplicate primarily by Station UUID and Exact Stream URL[cite: 1]
                let emptyStringArray: [String] = []
                var seenUuids = Set(emptyStringArray)
                var seenUrls = Set(emptyStringArray)
        
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

            // Rank by relevance matching Android LiveStationSearchEngine[cite: 1]
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

            // Drop any station with a name that has already appeared globally in the results
            var seenNames = Set(emptyStringArray)
            let distinctNameStations = ranked.filter { station in
                let cleanedName = station.name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
                if seenNames.contains(cleanedName) {
                    return false
                }
                seenNames.insert(cleanedName)
                return true
            }

            let limited = Array(distinctNameStations.prefix(finalLimit))
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

        // Country-Specific Boost matching Android
        let isCountryMatch: Bool
        switch normalizedQuery {
        case "germany", "german", "deutschland", "deutsch":
            isCountryMatch = normalizedCountry == "de"
        case "france", "french", "francais":
            isCountryMatch = normalizedCountry == "fr"
        case "israel", "israeli", "hebrew":
            isCountryMatch = normalizedCountry == "il"
        case "hawaii", "hawaiian":
            isCountryMatch = normalizedState == "hawaii"
        case "kenya", "kenyan":
            isCountryMatch = normalizedCountry == "ke"
        case "nigeria", "nigerian":
            isCountryMatch = normalizedCountry == "ng"
        case "ghana", "ghanian":
            isCountryMatch = normalizedCountry == "gh"
        case "egypt", "egyptian":
            isCountryMatch = normalizedCountry == "eg"
        case "senegal", "senegalese":
            isCountryMatch = normalizedCountry == "sn"
        case "south africa":
            isCountryMatch = normalizedCountry == "za"
        case "africa", "african":
            let africanCodes: Set<String> = ["ng", "za", "ke", "eg", "gh", "et", "sn", "ma", "tz", "ug", "dz", "ao", "cm", "ci", "sd", "cd"]
            isCountryMatch = africanCodes.contains(normalizedCountry)
        default:
            isCountryMatch = false
        }

        if isCountryMatch {
            score += 900
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
