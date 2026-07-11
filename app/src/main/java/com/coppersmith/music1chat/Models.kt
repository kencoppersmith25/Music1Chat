
package com.coppersmith.music1chat

data class Station(
    val id: Int,
    val name: String,
    val type: SourceType,
    val location: String,
    val iconUrl: String = "",
    val genre: String = "General",
    val tags: String = "",
    val state: String = ""
)

data class Mode(
    val name: String,
    val stations: List<Station>
)

enum class RadioCommand {
    NEXT_STATION,
    PREVIOUS_STATION,
    TOGGLE_PLAYBACK,
    UNKNOWN
}

enum class SourceType {
    STREAM,
    FOLDER,
    PLAYLIST,
    FILE
}