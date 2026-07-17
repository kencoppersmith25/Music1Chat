package com.coppersmith.music1chat.search

import com.coppersmith.music1chat.models.Station

class StationSearchEngine {

    fun search(
        query: String,
        stations: List<Station>
    ): SearchResult {
        val searchText = query
            .trim()
            .lowercase()

        if (searchText.isBlank()) {
            return SearchResult(
                query = "",
                stations = emptyList()
            )
        }

        val matches = stations
            .filter { station ->
                station.matchesSearch(searchText)
            }
            .sortedWith(
                compareBy<Station>(
                    { it.genre.lowercase() },
                    { it.name.lowercase() }
                )
            )

        return SearchResult(
            query = searchText,
            stations = matches
        )
    }

    private fun Station.matchesSearch(
        searchText: String
    ): Boolean {
        val normalizedGenre = genre
            .trim()
            .lowercase()

        val exactGenreMatch =
            normalizedGenre == searchText

        val stationInformation = buildString {
            append(name)
            append(' ')
            append(callLetters)
            append(' ')
            append(city)
            append(' ')
            append(country)
        }.lowercase()

        val stationInformationMatch =
            stationInformation.contains(searchText)

        return exactGenreMatch || stationInformationMatch
    }
}