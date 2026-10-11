package com.coppersmith.music1chat.search

import com.coppersmith.music1chat.models.SourceType
import com.coppersmith.music1chat.models.Station

// LIVE SEARCH RELEVANCE FIX V1
// - Keeps the user's search term as the displayed genre.
// - Rejects weak/unrelated Radio Browser matches.
// - Prioritizes exact names, whole-word name matches, and whole-word tags.
// - Keeps temporary station IDs negative to avoid repository ID collisions.

class LiveStationSearchEngine(
    private val radioBrowserClient: RadioBrowserClient =
        RadioBrowserClient()
) {

    suspend fun search(
        query: String,
        limit: Int = 1000
    ): SearchResult {
        val searchText = query.trim()

        if (searchText.isBlank()) {
            return SearchResult(
                query = "",
                stations = emptyList()
            )
        }

        val normalizedQuery =
            normalize(searchText)

        val queryWords =
            tokenize(searchText)

        val radioBrowserStations =
            radioBrowserClient.search(
                query = searchText,
                limit = (limit * 3).coerceAtMost(100)
            )

        // SYNERGY SEARCH: Expand broad geographic regions into key hubs/tags.
        val synergyStations = when {
            normalizedQuery.contains("hawai") || normalizedQuery.contains("hawaiian") -> {
                radioBrowserClient.search(query = "Honolulu") +
                        radioBrowserClient.search(query = "Maui") +
                        radioBrowserClient.search(query = "Kauai") +
                        radioBrowserClient.search(query = "Kona") +
                        radioBrowserClient.search(query = "Aloha") +
                        radioBrowserClient.search(query = "Hilo") +
                        radioBrowserClient.search(query = "Hawaii Music Live")
            }
            normalizedQuery.contains("classical") || normalizedQuery.contains("symphony") || normalizedQuery.contains("orchestra") || normalizedQuery.contains("opera") -> {
                radioBrowserClient.search(query = "Classical") +
                        radioBrowserClient.search(query = "Symphony") +
                        radioBrowserClient.search(query = "Orchestra") +
                        radioBrowserClient.search(query = "Opera") +
                        radioBrowserClient.search(query = "Chamber") +
                        radioBrowserClient.search(query = "Baroque") +
                        radioBrowserClient.search(query = "Philharmonic") +
                        radioBrowserClient.search(query = "Choir")
            }
            normalizedQuery.contains("jazz") || normalizedQuery.contains("blues") -> {
                radioBrowserClient.search(query = "Jazz") +
                        radioBrowserClient.search(query = "Blues") +
                        radioBrowserClient.search(query = "Smooth Jazz") +
                        radioBrowserClient.search(query = "New Orleans") +
                        radioBrowserClient.search(query = "Chicago Jazz") +
                        radioBrowserClient.search(query = "Bebop")
            }
            normalizedQuery.contains("electronic") || normalizedQuery.contains("edm") || normalizedQuery.contains("techno") || normalizedQuery.contains("house") -> {
                radioBrowserClient.search(query = "Electronic") +
                        radioBrowserClient.search(query = "Techno") +
                        radioBrowserClient.search(query = "House") +
                        radioBrowserClient.search(query = "Trance") +
                        radioBrowserClient.search(query = "Ambient") +
                        radioBrowserClient.search(query = "Ibiza") +
                        radioBrowserClient.search(query = "Berlin Electronic")
            }
            normalizedQuery.contains("israel") || normalizedQuery.contains("israeli") -> {
                radioBrowserClient.search(query = "Tel Aviv") +
                        radioBrowserClient.search(query = "Jerusalem") +
                        radioBrowserClient.search(query = "Haifa") +
                        radioBrowserClient.search(query = "Eilat") +
                        radioBrowserClient.search(query = "Beer Sheva") +
                        radioBrowserClient.search(query = "Netanya") +
                        radioBrowserClient.search(query = "Israel")
            }
            normalizedQuery.contains("africa") || normalizedQuery.contains("african") -> {
                radioBrowserClient.search(query = "Lagos") +
                        radioBrowserClient.search(query = "Johannesburg") +
                        radioBrowserClient.search(query = "Cairo") +
                        radioBrowserClient.search(query = "Nairobi") +
                        radioBrowserClient.search(query = "Casablanca") +
                        radioBrowserClient.search(query = "Accra") +
                        radioBrowserClient.search(query = "Cape Town") +
                        radioBrowserClient.search(query = "Dakar") +
                        radioBrowserClient.search(query = "Tunis") +
                        radioBrowserClient.search(query = "Addis Ababa") +
                        radioBrowserClient.search(query = "African")
            }
            normalizedQuery.contains("france") || normalizedQuery.contains("french") -> {
                radioBrowserClient.search(query = "Paris") +
                        radioBrowserClient.search(query = "Lyon") +
                        radioBrowserClient.search(query = "Marseille") +
                        radioBrowserClient.search(query = "Nice") +
                        radioBrowserClient.search(query = "Bordeaux") +
                        radioBrowserClient.search(query = "Toulouse") +
                        radioBrowserClient.search(query = "Nantes") +
                        radioBrowserClient.search(query = "Strasbourg") +
                        radioBrowserClient.search(query = "Lille") +
                        radioBrowserClient.search(query = "France")
            }
            normalizedQuery.contains("germany") || normalizedQuery.contains("german") -> {
                radioBrowserClient.search(query = "Berlin") +
                        radioBrowserClient.search(query = "Munich") +
                        radioBrowserClient.search(query = "Frankfurt") +
                        radioBrowserClient.search(query = "Hamburg") +
                        radioBrowserClient.search(query = "Cologne") +
                        radioBrowserClient.search(query = "Stuttgart") +
                        radioBrowserClient.search(query = "Dusseldorf") +
                        radioBrowserClient.search(query = "Leipzig") +
                        radioBrowserClient.search(query = "Dortmund") +
                        radioBrowserClient.search(query = "Germany")
            }
            normalizedQuery.contains("puerto rico") || normalizedQuery.contains("puertorico") || normalizedQuery.contains("puertorican") -> {
                radioBrowserClient.search(query = "San Juan") +
                        radioBrowserClient.search(query = "Ponce") +
                        radioBrowserClient.search(query = "Mayaguez") +
                        radioBrowserClient.search(query = "Caguas") +
                        radioBrowserClient.search(query = "Arecibo") +
                        radioBrowserClient.search(query = "Puerto Rico")
            }
            normalizedQuery.contains("norway") || normalizedQuery.contains("norwegian") -> {
                radioBrowserClient.search(query = "Oslo") +
                        radioBrowserClient.search(query = "Bergen") +
                        radioBrowserClient.search(query = "Trondheim") +
                        radioBrowserClient.search(query = "Stavanger") +
                        radioBrowserClient.search(query = "Tromso") +
                        radioBrowserClient.search(query = "Norway")
            }
            normalizedQuery.contains("sweden") || normalizedQuery.contains("swedish") -> {
                radioBrowserClient.search(query = "Stockholm") +
                        radioBrowserClient.search(query = "Gothenburg") +
                        radioBrowserClient.search(query = "Malmo") +
                        radioBrowserClient.search(query = "Uppsala") +
                        radioBrowserClient.search(query = "Linkoping") +
                        radioBrowserClient.search(query = "Sweden")
            }
            normalizedQuery.contains("india") || normalizedQuery.contains("indian") -> {
                radioBrowserClient.search(query = "Mumbai") +
                        radioBrowserClient.search(query = "Delhi") +
                        radioBrowserClient.search(query = "Bangalore") +
                        radioBrowserClient.search(query = "Chennai") +
                        radioBrowserClient.search(query = "Kolkata") +
                        radioBrowserClient.search(query = "Hyderabad") +
                        radioBrowserClient.search(query = "Pune") +
                        radioBrowserClient.search(query = "Ahmedabad") +
                        radioBrowserClient.search(query = "India")
            }
            normalizedQuery.contains("china") || normalizedQuery.contains("chinese") -> {
                radioBrowserClient.search(query = "Beijing") +
                        radioBrowserClient.search(query = "Shanghai") +
                        radioBrowserClient.search(query = "Hong Kong") +
                        radioBrowserClient.search(query = "Guangzhou") +
                        radioBrowserClient.search(query = "Shenzhen") +
                        radioBrowserClient.search(query = "Chengdu") +
                        radioBrowserClient.search(query = "Xi'an") +
                        radioBrowserClient.search(query = "China")
            }
            normalizedQuery.contains("japan") || normalizedQuery.contains("japanese") -> {
                radioBrowserClient.search(query = "Tokyo") +
                        radioBrowserClient.search(query = "Osaka") +
                        radioBrowserClient.search(query = "Kyoto") +
                        radioBrowserClient.search(query = "Nagoya") +
                        radioBrowserClient.search(query = "Fukuoka") +
                        radioBrowserClient.search(query = "Sapporo") +
                        radioBrowserClient.search(query = "Kobe") +
                        radioBrowserClient.search(query = "Japan")
            }
            normalizedQuery.contains("brazil") || normalizedQuery.contains("brazilian") -> {
                radioBrowserClient.search(query = "Rio de Janeiro") +
                        radioBrowserClient.search(query = "Sao Paulo") +
                        radioBrowserClient.search(query = "Salvador") +
                        radioBrowserClient.search(query = "Brasilia") +
                        radioBrowserClient.search(query = "Fortaleza") +
                        radioBrowserClient.search(query = "Belo Horizonte") +
                        radioBrowserClient.search(query = "Curitiba") +
                        radioBrowserClient.search(query = "Brazil")
            }
            normalizedQuery.contains("colombia") || normalizedQuery.contains("colombian") -> {
                radioBrowserClient.search(query = "Bogota") +
                        radioBrowserClient.search(query = "Medellin") +
                        radioBrowserClient.search(query = "Cali") +
                        radioBrowserClient.search(query = "Barranquilla") +
                        radioBrowserClient.search(query = "Cartagena") +
                        radioBrowserClient.search(query = "Colombia")
            }
            normalizedQuery.contains("argentina") -> {
                radioBrowserClient.search(query = "Buenos Aires") +
                        radioBrowserClient.search(query = "Cordoba") +
                        radioBrowserClient.search(query = "Rosario") +
                        radioBrowserClient.search(query = "Mendoza") +
                        radioBrowserClient.search(query = "La Plata") +
                        radioBrowserClient.search(query = "Argentina")
            }
            normalizedQuery.contains("united kingdom") || normalizedQuery.contains("uk") || normalizedQuery.contains("british") ||
            normalizedQuery.contains("england") -> {
                radioBrowserClient.search(query = "London") +
                        radioBrowserClient.search(query = "Manchester") +
                        radioBrowserClient.search(query = "Birmingham") +
                        radioBrowserClient.search(query = "Glasgow") +
                        radioBrowserClient.search(query = "Edinburgh") +
                        radioBrowserClient.search(query = "Liverpool") +
                        radioBrowserClient.search(query = "Bristol") +
                        radioBrowserClient.search(query = "Cardiff") +
                        radioBrowserClient.search(query = "Belfast") +
                        radioBrowserClient.search(query = "BBC") +
                        radioBrowserClient.search(query = "United Kingdom")
            }
            normalizedQuery.contains("italy") || normalizedQuery.contains("italian") -> {
                radioBrowserClient.search(query = "Rome") +
                        radioBrowserClient.search(query = "Milan") +
                        radioBrowserClient.search(query = "Naples") +
                        radioBrowserClient.search(query = "Turin") +
                        radioBrowserClient.search(query = "Florence") +
                        radioBrowserClient.search(query = "Venice") +
                        radioBrowserClient.search(query = "Bologna") +
                        radioBrowserClient.search(query = "Palermo") +
                        radioBrowserClient.search(query = "Italy")
            }
            normalizedQuery.contains("spain") || normalizedQuery.contains("spanish") -> {
                radioBrowserClient.search(query = "Madrid") +
                        radioBrowserClient.search(query = "Barcelona") +
                        radioBrowserClient.search(query = "Valencia") +
                        radioBrowserClient.search(query = "Seville") +
                        radioBrowserClient.search(query = "Bilbao") +
                        radioBrowserClient.search(query = "Malaga") +
                        radioBrowserClient.search(query = "Zaragoza") +
                        radioBrowserClient.search(query = "Spain")
            }
            normalizedQuery.contains("south korea") || normalizedQuery.contains("korean") -> {
                radioBrowserClient.search(query = "Seoul") +
                        radioBrowserClient.search(query = "Busan") +
                        radioBrowserClient.search(query = "Incheon") +
                        radioBrowserClient.search(query = "Daegu") +
                        radioBrowserClient.search(query = "Daejeon") +
                        radioBrowserClient.search(query = "Korea")
            }
            normalizedQuery.contains("philippines") || normalizedQuery.contains("filipino") -> {
                radioBrowserClient.search(query = "Manila") +
                        radioBrowserClient.search(query = "Cebu") +
                        radioBrowserClient.search(query = "Davao") +
                        radioBrowserClient.search(query = "Quezon City") +
                        radioBrowserClient.search(query = "Zamboanga") +
                        radioBrowserClient.search(query = "Philippines")
            }
            normalizedQuery.contains("australia") || normalizedQuery.contains("australian") -> {
                radioBrowserClient.search(query = "Sydney") +
                        radioBrowserClient.search(query = "Melbourne") +
                        radioBrowserClient.search(query = "Brisbane") +
                        radioBrowserClient.search(query = "Perth") +
                        radioBrowserClient.search(query = "Adelaide") +
                        radioBrowserClient.search(query = "Canberra") +
                        radioBrowserClient.search(query = "Hobart") +
                        radioBrowserClient.search(query = "Australia")
            }
            normalizedQuery.contains("canada") || normalizedQuery.contains("canadian") -> {
                radioBrowserClient.search(query = "Toronto") +
                        radioBrowserClient.search(query = "Vancouver") +
                        radioBrowserClient.search(query = "Montreal") +
                        radioBrowserClient.search(query = "Calgary") +
                        radioBrowserClient.search(query = "Ottawa") +
                        radioBrowserClient.search(query = "Edmonton") +
                        radioBrowserClient.search(query = "Canada")
            }
            normalizedQuery.contains("mexico") || normalizedQuery.contains("mexican") -> {
                radioBrowserClient.search(query = "Mexico City") +
                        radioBrowserClient.search(query = "Guadalajara") +
                        radioBrowserClient.search(query = "Monterrey") +
                        radioBrowserClient.search(query = "Puebla") +
                        radioBrowserClient.search(query = "Cancun") +
                        radioBrowserClient.search(query = "Mexico")
            }
            else -> emptyList()
        }

        val allCandidateStations = radioBrowserStations + synergyStations

        val rankedStations =
            allCandidateStations
                .mapNotNull { radioStation ->
                    val relevanceScore =
                        radioStation.calculateRelevanceScore(
                            normalizedQuery = normalizedQuery,
                            queryWords = queryWords
                        )

                    if (relevanceScore <= 0) {
                        null
                    } else {
                        RankedRadioStation(
                            station = radioStation,
                            relevanceScore = relevanceScore
                        )
                    }
                }
                .sortedWith(
                    compareByDescending<RankedRadioStation> {
                        it.relevanceScore
                    }.thenByDescending {
                        it.station.votes
                    }.thenByDescending {
                        it.station.clickCount
                    }.thenBy {
                        it.station.name.lowercase()
                    }
                )
                .distinctBy { ranked ->
                    ranked.station.stationUuid.ifBlank {
                        ranked.station.resolvedStreamUrl.ifBlank {
                            ranked.station.streamUrl
                        }
                    }
                }
                .take(limit.coerceIn(1, 100))

        val stations =
            rankedStations
                .map { ranked ->
                    ranked.station.toStation(
                        searchQuery = searchText
                    )
                }
        android.util.Log.d("kencheck", "LiveStationSearchEngine produced: ${stations.size} stations (passed limit parameter: $limit)")
        return SearchResult(
            query = searchText,
            stations = stations
        )
    }

    private data class RankedRadioStation(
        val station: RadioBrowserStation,
        val relevanceScore: Int
    )

    private fun RadioBrowserStation.calculateRelevanceScore(
        normalizedQuery: String,
        queryWords: List<String>
    ): Int {
        val normalizedName = normalize(name)
        val normalizedTags = normalize(tags)
        val normalizedState = normalize(state)
        val normalizedCountry = normalize(countryCode)
        val normalizedLanguage = normalize(language)

        if (normalizedName.isBlank()) {
            return 0
        }

        // 1. Give every station returned by the search a baseline score so it isn't discarded
        var score = 100

        when {
            normalizedName == normalizedQuery -> score += 1_000
            normalizedName.startsWith(normalizedQuery) -> score += 800
            normalizedName.contains(normalizedQuery) -> score += 650
            normalizedTags == normalizedQuery -> score += 550
            normalizedTags.contains(normalizedQuery) -> score += 500
            normalizedState.contains(normalizedQuery) -> score += 700
        }

        if (queryWords.isNotEmpty()) {
            val nameWordMatches = queryWords.count { word -> normalizedName.contains(word) }
            val tagWordMatches = queryWords.count { word -> normalizedTags.contains(word) }
            val locationWordMatches = queryWords.count { word ->
                normalizedState.contains(word) || normalizedCountry.contains(word)
            }
            val languageWordMatches = queryWords.count { word -> normalizedLanguage.contains(word) }

            score += nameWordMatches * 140
            score += tagWordMatches * 90
            score += locationWordMatches * 70
            score += languageWordMatches * 60
        }

        if (resolvedStreamUrl.isNotBlank()) {
            score += 25
        }

        score += votes.coerceAtMost(500) / 20
        score += clickCount.coerceAtMost(2_000) / 100

        return score
    }


    private fun RadioBrowserStation.toStation(
        searchQuery: String
    ): Station {
        val originalUrl =
            streamUrl.trim()

        val resolvedUrl =
            resolvedStreamUrl.trim()

        val playbackUrl =
            resolvedUrl.ifBlank {
                originalUrl
            }

        return Station(
            id = createTemporaryStationId(),
            name = name.trim(),
            streamUrl = playbackUrl,
            genre = searchQuery.trim(),
            callLetters = "",
            city = state.trim(),
            country = countryCode.trim(),
            logoUrl = faviconUrl.trim(),
            sourceType = SourceType.STREAM,
            includedInNavigation = false,
            failedThisSession = false,
            resolvedStreamUrl = resolvedUrl,
            streamVerified = false,
            lastVerified = 0L
        )
    }

    private fun RadioBrowserStation.createTemporaryStationId(): Long {
        val identity =
            stationUuid.ifBlank {
                resolvedStreamUrl.ifBlank {
                    streamUrl
                }
            }

        val hash =
            identity.hashCode().toLong()

        return when {
            hash == Long.MIN_VALUE -> -1L
            hash > 0L -> -hash
            hash == 0L -> -1L
            else -> hash
        }
    }

    private fun normalize(
        value: String
    ): String {
        return value
            .trim()
            .lowercase()
            .replace(
                Regex("[^a-z0-9]+"),
                " "
            )
            .trim()
            .replace(
                Regex("\\s+"),
                " "
            )
    }

    private fun tokenize(
        value: String
    ): List<String> {
        val genericStationWords =
            setOf(
                "radio",
                "station",
                "music",
                "live",
                "online",
                "stream",
                "fm",
                "am",
                "the"
            )

        return normalize(value)
            .split(' ')
            .map { word ->
                word.trim()
            }
            .filter { word ->
                word.length >= 2 &&
                        word !in genericStationWords
            }
            .distinct()
    }

    private fun containsWholeWord(
        text: String,
        word: String
    ): Boolean {
        if (
            text.isBlank() ||
            word.isBlank()
        ) {
            return false
        }

        return text
            .split(' ')
            .any { token ->
                token == word
            }
    }

    private fun containsWholePhrase(
        text: String,
        phrase: String
    ): Boolean {
        if (
            text.isBlank() ||
            phrase.isBlank()
        ) {
            return false
        }

        return " $text ".contains(
            " $phrase "
        )
    }
}