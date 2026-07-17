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
            val encodedQuery = URLEncoder.encode(
                searchText,
                StandardCharsets.UTF_8.toString()
            )

            val commonParameters =
                "hidebroken=true" +
                        "&order=clickcount" +
                        "&reverse=true" +
                        "&limit=$safeLimit"

            val nameUrl =
                "$BASE_URL/json/stations/search" +
                        "?name=$encodedQuery" +
                        "&nameExact=false" +
                        "&$commonParameters"

            val tagUrl =
                "$BASE_URL/json/stations/search" +
                        "?tag=$encodedQuery" +
                        "&tagExact=false" +
                        "&$commonParameters"

            val nameMatches = executeSearch(nameUrl)
            val tagMatches = executeSearch(tagUrl)

            (nameMatches + tagMatches)
                .distinctBy { station ->
                    station.stationUuid.ifBlank {
                        station.resolvedStreamUrl.ifBlank {
                            station.streamUrl
                        }
                    }
                }
                .sortedByDescending { station ->
                    station.clickCount
                }
                .take(safeLimit)
        }
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