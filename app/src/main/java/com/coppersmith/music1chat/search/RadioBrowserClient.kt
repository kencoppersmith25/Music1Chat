package com.coppersmith.music1chat.search

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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

    private val baseUrls = listOf(
        "https://de1.api.radio-browser.info",
        "https://all.api.radio-browser.info",
        "https://de2.api.radio-browser.info",
        "https://fi1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info"
    )

    suspend fun search(
        query: String,
        limit: Int = 50
    ): List<RadioBrowserStation> {
        val searchText = query.trim()
        if (searchText.isBlank()) {
            return emptyList()
        }

        val safeLimit = limit.coerceIn(1, 100)
        var hadDirectoryFailure = false

        for (baseUrl in baseUrls) {
            try {
                return executeMultiSearch(baseUrl, searchText, safeLimit)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                hadDirectoryFailure = true
            }
        }

        if (hadDirectoryFailure) {
            throw IllegalStateException("Station search is temporarily unavailable. Please try again.")
        }

        return emptyList()
    }

    private suspend fun executeMultiSearch(
        baseUrl: String,
        text: String,
        finalLimit: Int
    ): List<RadioBrowserStation> = withContext(Dispatchers.IO) {
        val normalized = text.lowercase()
        val deferredSearches = mutableListOf<kotlinx.coroutines.Deferred<List<RadioBrowserStation>>>()

        coroutineScope {
            // 1. Core Searches
            deferredSearches += async { fetch(baseUrl, "name", text, finalLimit) }
            deferredSearches += async { fetch(baseUrl, "tag", text, finalLimit) }

            // 2. Ultra Hawaiian Synergy
            if (normalized.contains("hawai")) {
                deferredSearches += async { fetch(baseUrl, "state", "Hawaii", 30) }
                deferredSearches += async { fetch(baseUrl, "tag", "hawaiian", 30) }
                deferredSearches += async { fetch(baseUrl, "name", "Honolulu", 30) }
                deferredSearches += async { fetch(baseUrl, "name", "Maui", 20) }
                deferredSearches += async { fetch(baseUrl, "name", "Kauai", 15) }
                deferredSearches += async { fetch(baseUrl, "name", "Kona", 15) }
                deferredSearches += async { fetch(baseUrl, "name", "Aloha", 15) }
                deferredSearches += async { fetch(baseUrl, "name", "Hawaii Music Live", 10) }
            }
        }

        val collected = mutableListOf<RadioBrowserStation>()
        var successfulRequests = 0

        deferredSearches.awaitAll().forEach { results ->
            if (results.isNotEmpty()) {
                collected += results
                successfulRequests++
            }
        }

        if (successfulRequests == 0 && collected.isEmpty()) {
            throw IllegalStateException("No results or server error.")
        }

        // Filter invalid records
        val validStations = collected.filter { station ->
            val cleanedName = station.name.trim()
            val playbackUrl = station.resolvedStreamUrl.ifBlank { station.streamUrl }.trim()
            cleanedName.isNotBlank() && playbackUrl.isNotBlank()
        }

        // Deduplicate by UUID or Cleaned URL
        val seen = mutableSetOf<String>()
        val deduplicated = validStations.filter { station ->
            val uuid = station.stationUuid.trim().lowercase()
            val rawUrl = station.resolvedStreamUrl.ifBlank { station.streamUrl }.trim().lowercase()
            val key = uuid.ifBlank { rawUrl }
            if (seen.contains(key)) {
                false
            } else {
                seen.add(key)
                true
            }
        }

        val limited = deduplicated.take(finalLimit)
        interleaveStations(limited)
    }

    private fun interleaveStations(stations: List<RadioBrowserStation>): List<RadioBrowserStation> {
        val count = stations.size
        if (count <= 1) return stations

        val stride = 10
        val result = arrayOfNulls<RadioBrowserStation>(count)

        for (i in 0 until count) {
            val targetIndex = ((i * stride) % count) + ((i * stride) / count)
            val safeIndex = targetIndex.coerceIn(0, count - 1)

            if (result[safeIndex] == null) {
                result[safeIndex] = stations[i]
            } else {
                for (j in 0 until count) {
                    val fallbackIndex = (safeIndex + j) % count
                    if (result[fallbackIndex] == null) {
                        result[fallbackIndex] = stations[i]
                        break
                    }
                }
            }
        }

        return result.filterNotNull()
    }

    private fun fetch(
        baseUrl: String,
        field: String,
        text: String,
        limit: Int
    ): List<RadioBrowserStation> {
        val encodedText = URLEncoder.encode(text.trim(), StandardCharsets.UTF_8.toString())
        val urlString = "$baseUrl/json/stations/search?" +
                "$field=$encodedText" +
                "&${field}Exact=false" +
                "&hidebroken=true" +
                "&order=clickcount" +
                "&reverse=true" +
                "&limit=$limit"

        val connection = URL(urlString).openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLISECONDS
            connection.readTimeout = READ_TIMEOUT_MILLISECONDS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return emptyList()
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            parseStations(responseText)
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    private fun parseStations(responseText: String): List<RadioBrowserStation> {
        val jsonStations = JSONArray(responseText)
        val stations = mutableListOf<RadioBrowserStation>()

        for (index in 0 until jsonStations.length()) {
            val jsonStation = jsonStations.getJSONObject(index)

            val name = jsonStation.optString("name").trim()
            val originalUrl = jsonStation.optString("url").trim()
            val resolvedUrl = jsonStation.optString("url_resolved").trim()
            val playbackUrl = resolvedUrl.ifBlank { originalUrl }

            if (name.isBlank() || playbackUrl.isBlank()) {
                continue
            }

            stations += RadioBrowserStation(
                stationUuid = jsonStation.optString("stationuuid").trim(),
                name = name,
                streamUrl = originalUrl,
                resolvedStreamUrl = resolvedUrl,
                faviconUrl = jsonStation.optString("favicon").trim(),
                tags = jsonStation.optString("tags").trim(),
                countryCode = jsonStation.optString("countrycode").trim(),
                state = jsonStation.optString("state").trim(),
                language = jsonStation.optString("language").trim(),
                codec = jsonStation.optString("codec").trim(),
                bitrate = jsonStation.optInt("bitrate"),
                votes = jsonStation.optInt("votes"),
                clickCount = jsonStation.optInt("clickcount")
            )
        }

        return stations
    }

    companion object {
        private const val USER_AGENT = "NoHandsRadio/1.1 Android"
        private const val CONNECT_TIMEOUT_MILLISECONDS = 6_000
        private const val READ_TIMEOUT_MILLISECONDS = 8_000
    }
}