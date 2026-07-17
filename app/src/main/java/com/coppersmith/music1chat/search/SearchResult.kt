package com.coppersmith.music1chat.search

import com.coppersmith.music1chat.models.Station

data class SearchResult(
    val query: String,
    val stations: List<Station>
)