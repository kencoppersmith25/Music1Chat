package com.coppersmith.music1chat.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets

data class RadioBrowserStation(
    val stationUuid: String,
    val name: String,
    val streamUrl: String,
    val resolvedStreamUrl: String,
    val faviconUrl: String,
    val tags: String,
    val countryCode: String,
    val state: String,
    val language: String,
    val codec: String,
    val bitrate: Int,
    val votes: Int,
    val clickCount: Int
)

class RadioBrowserClient {

    suspend fun search(
        query: String,
        limit: Int = 50
    ): List<RadioBrowserStation> {
        val searchText = query.trim()

        if (searchText.isBlank()) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            val safeLimit = limit.coerceIn(1, 100)

            val searchPhrases =
                buildSearchPhrases(searchText)

            val allMatches =
                mutableListOf<RadioBrowserStation>()

            // First, try the complete station name exactly.
            allMatches += executeSearch(
                buildSearchUrl(
                    parameter = "name",
                    value = searchText,
                    exact = true,
                    limit = safeLimit
                )
            )

            // Then allow the complete phrase anywhere in the station name.
            allMatches += executeSearch(
                buildSearchUrl(
                    parameter = "name",
                    value = searchText,
                    exact = false,
                    limit = safeLimit
                )
            )

            // Also search the complete phrase as a tag.
            allMatches += executeSearch(
                buildSearchUrl(
                    parameter = "tag",
                    value = searchText,
                    exact = false,
                    limit = safeLimit
                )
            )

            // If the full phrase was too restrictive, search shorter phrases.
            searchPhrases
                .filterNot { phrase ->
                    phrase.equals(
                        searchText,
                        ignoreCase = true
                    )
                }
                .forEach { phrase ->
                    allMatches += executeSearch(
                        buildSearchUrl(
                            parameter = "name",
                            value = phrase,
                            exact = false,
                            limit = safeLimit
                        )
                    )
                }

            val rankedStations =
                allMatches
                    .distinctBy { station ->
                        station.stationUuid.ifBlank {
                            station.resolvedStreamUrl.ifBlank {
                                station.streamUrl
                            }
                        }
                    }
                    // Radio Browser can contain multiple station records that
                    // resolve to exactly the same playable stream.
                    .distinctBy { station ->
                        normalizedPlaybackUrl(station)
                    }
                    // It can also list the same station more than once at
                    // different bitrates or alternate stream endpoints.
                    // Example: "City Latin" appeared twice with normal and
                    // high-quality URLs. Keep only one exact normalized name.
                    .distinctBy { station ->
                        normalizedStationIdentity(station.name)
                    }
                    .filter { station ->
                        val stationWords =
                            normalizeForMatching(station.name)
                                .split(' ')
                                .filter { it.isNotBlank() }

                        val queryWords =
                            normalizeForMatching(searchText)
                                .split(' ')
                                .filter { it.isNotBlank() }

                        queryWords.any { queryWord ->
                            stationWords.contains(queryWord)
                        }
                    }
                    .sortedWith(
                        compareByDescending<RadioBrowserStation> { station ->
                            calculateClientMatchScore(
                                stationName = station.name,
                                query = searchText
                            )
                        }.thenByDescending { station ->
                            station.votes
                        }.thenByDescending { station ->
                            station.clickCount
                        }
                    )

            diversifySisterStations(
                stations = rankedStations,
                limit = safeLimit
            )
        }
    }
    private fun buildSearchUrl(
        parameter: String,
        value: String,
        exact: Boolean,
        limit: Int
    ): String {
        val encodedValue =
            URLEncoder.encode(
                value.trim(),
                StandardCharsets.UTF_8.toString()
            )

        val exactParameter =
            when (parameter) {
                "name" -> "&nameExact=$exact"
                "tag" -> "&tagExact=$exact"
                else -> ""
            }

        return "$BASE_URL/json/stations/search" +
                "?$parameter=$encodedValue" +
                exactParameter +
                "&hidebroken=true" +
                "&order=clickcount" +
                "&reverse=true" +
                "&limit=$limit"
    }

    private fun buildSearchPhrases(
        query: String
    ): List<String> {
        val words =
            query
                .trim()
                .split(Regex("\\s+"))
                .filter { word ->
                    word.length >= 2
                }

        if (words.isEmpty()) {
            return emptyList()
        }

        val phrases =
            mutableListOf<String>()

        // Hawaiian Music Live
        phrases += words.joinToString(" ")

        // Hawaiian Music
        // Hawaiian
        for (wordCount in words.size - 1 downTo 1) {
            phrases += words
                .take(wordCount)
                .joinToString(" ")
        }

        // Individual meaningful words.
        phrases += words
            .filterNot { word ->
                word.equals("live", ignoreCase = true) ||
                        word.equals("radio", ignoreCase = true) ||
                        word.equals("fm", ignoreCase = true)
            }

        return phrases
            .map { phrase ->
                phrase.trim()
            }
            .filter { phrase ->
                phrase.isNotBlank()
            }
            .distinctBy { phrase ->
                phrase.lowercase()
            }
    }

    private fun calculateClientMatchScore(
        stationName: String,
        query: String
    ): Int {
        val normalizedStationName =
            normalizeForMatching(stationName)

        val normalizedQuery =
            normalizeForMatching(query)

        val stationWords =
            normalizedStationName
                .split(' ')
                .filter { it.isNotBlank() }

        val queryWords =
            normalizedQuery
                .split(' ')
                .filter { it.isNotBlank() }

        return when {
            normalizedStationName == normalizedQuery ->
                1_000

            normalizedStationName.startsWith(
                "$normalizedQuery "
            ) ->
                850

            stationWords.contains(normalizedQuery) ->
                750

            queryWords.all { word ->
                stationWords.contains(word)
            } ->
                600

            else ->
                queryWords.count { word ->
                    stationWords.contains(word)
                } * 100
        }
    }

    private fun normalizeForMatching(
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
    /**
     * Rearranges results so two consecutive stations are not supplied by the
     * same obvious stream family whenever another choice is available.
     *
     * We deliberately rearrange rather than delete every same-provider entry:
     * one network can operate several genuinely different channels.
     */
    private fun diversifySisterStations(
        stations: List<RadioBrowserStation>,
        limit: Int
    ): List<RadioBrowserStation> {
        if (stations.size <= 1) {
            return stations.take(limit)
        }

        val remaining = stations.toMutableList()
        val diversified = mutableListOf<RadioBrowserStation>()
        var previousProviderFamily: String? = null

        while (remaining.isNotEmpty() && diversified.size < limit) {
            val differentFamilyIndex =
                remaining.indexOfFirst { station ->
                    providerFamily(station) != previousProviderFamily
                }

            val selectedIndex =
                if (differentFamilyIndex >= 0) {
                    differentFamilyIndex
                } else {
                    0
                }

            val selected = remaining.removeAt(selectedIndex)
            diversified += selected
            previousProviderFamily = providerFamily(selected)
        }

        return diversified
    }

    private fun normalizedStationIdentity(
        stationName: String
    ): String {
        return normalizeForMatching(stationName)
    }

    private fun normalizedPlaybackUrl(
        station: RadioBrowserStation
    ): String {
        val playbackUrl =
            station.resolvedStreamUrl.ifBlank {
                station.streamUrl
            }

        return try {
            val url = URL(playbackUrl)
            val normalizedPort =
                when {
                    url.port == -1 -> ""
                    url.protocol.equals("http", ignoreCase = true) &&
                            url.port == 80 -> ""
                    url.protocol.equals("https", ignoreCase = true) &&
                            url.port == 443 -> ""
                    else -> ":${url.port}"
                }

            val normalizedPath =
                url.path
                    .trim()
                    .trimEnd('/')
                    .lowercase()

            "${url.protocol.lowercase()}://${url.host.lowercase()}" +
                    normalizedPort +
                    normalizedPath
        } catch (_: Exception) {
            playbackUrl
                .trim()
                .trimEnd('/')
                .lowercase()
        }
    }

    private fun providerFamily(
        station: RadioBrowserStation
    ): String {
        val playbackUrl =
            station.resolvedStreamUrl.ifBlank {
                station.streamUrl
            }

        val host =
            try {
                URL(playbackUrl)
                    .host
                    .lowercase()
                    .removePrefix("www.")
            } catch (_: Exception) {
                return normalizedPlaybackUrl(station)
            }

        return when {
            // The ride log showed several BreakZ and RauteMusik listings
            // carrying the same synchronized programming.
            host.contains("breakz") ||
                    host.contains("rautemusik") ->
                "breakz-rautemusik"

            else ->
                registrableHost(host)
        }
    }

    private fun registrableHost(
        host: String
    ): String {
        val parts =
            host
                .split('.')
                .filter { part ->
                    part.isNotBlank()
                }

        if (parts.size <= 2) {
            return host
        }

        return parts
            .takeLast(2)
            .joinToString(".")
    }

    private fun executeSearch(
        requestUrl: String
    ): List<RadioBrowserStation> {
        val connection = URL(requestUrl)
            .openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout =
                CONNECT_TIMEOUT_MILLISECONDS
            connection.readTimeout =
                READ_TIMEOUT_MILLISECONDS
            connection.setRequestProperty(
                "User-Agent",
                USER_AGENT
            )
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                throw IllegalStateException(
                    "Radio Browser returned HTTP $responseCode."
                )
            }

            val responseText = connection
                .inputStream
                .bufferedReader()
                .use { reader ->
                    reader.readText()
                }

            parseStations(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseStations(
        responseText: String
    ): List<RadioBrowserStation> {
        val jsonStations = JSONArray(responseText)
        val stations = mutableListOf<RadioBrowserStation>()

        for (index in 0 until jsonStations.length()) {
            val jsonStation = jsonStations.getJSONObject(index)

            val name = jsonStation
                .optString("name")
                .trim()

            val originalUrl = jsonStation
                .optString("url")
                .trim()

            val resolvedUrl = jsonStation
                .optString("url_resolved")
                .trim()

            val playbackUrl = resolvedUrl.ifBlank {
                originalUrl
            }

            if (name.isBlank() || playbackUrl.isBlank()) {
                continue
            }

            stations += RadioBrowserStation(
                stationUuid = jsonStation
                    .optString("stationuuid")
                    .trim(),
                name = name,
                streamUrl = originalUrl,
                resolvedStreamUrl = resolvedUrl,
                faviconUrl = jsonStation
                    .optString("favicon")
                    .trim(),
                tags = jsonStation
                    .optString("tags")
                    .trim(),
                countryCode = jsonStation
                    .optString("countrycode")
                    .trim(),
                state = jsonStation
                    .optString("state")
                    .trim(),
                language = jsonStation
                    .optString("language")
                    .trim(),
                codec = jsonStation
                    .optString("codec")
                    .trim(),
                bitrate = jsonStation.optInt("bitrate"),
                votes = jsonStation.optInt("votes"),
                clickCount = jsonStation.optInt("clickcount")
            )
        }

        return stations
    }

    companion object {
        private const val BASE_URL =
            "https://de1.api.radio-browser.info"

        private const val USER_AGENT =
            "Music1Chat/1.0"

        private const val CONNECT_TIMEOUT_MILLISECONDS =
            10_000

        private const val READ_TIMEOUT_MILLISECONDS =
            15_000
    }
}