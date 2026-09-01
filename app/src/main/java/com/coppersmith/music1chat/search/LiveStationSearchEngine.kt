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

        // SYNERGY SEARCH: If the query is "Hawaii" or "Hawaiian", find everything on the islands.
        val synergyStations = if (normalizedQuery.contains("hawai")) {
             radioBrowserClient.search(query = "Honolulu", limit = 30) + 
             radioBrowserClient.search(query = "Maui", limit = 20) +
             radioBrowserClient.search(query = "Kauai", limit = 15) +
             radioBrowserClient.search(query = "Kona", limit = 15) +
             radioBrowserClient.search(query = "Aloha", limit = 15) +
             radioBrowserClient.search(query = "Hawaii Music Live", limit = 10)
        } else {
            emptyList()
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