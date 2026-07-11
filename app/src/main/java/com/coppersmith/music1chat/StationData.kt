package com.coppersmith.music1chat

object StationData {

    val RELIABLE_STATIONS = listOf(
        Station(
            id = -1,
            name = "KMNO Maui",
            type = SourceType.STREAM,
            location = "http://kmno.streamguys1.com/live",
            iconUrl = "",
            genre = "Hawaiian"
        ),
        Station(
            id = -2,
            name = "99.1 JOY",
            type = SourceType.STREAM,
            location = "http://gateway.cdnstream1.com/2808_96.aac",
            iconUrl = "",
            genre = "Christian"
        ),
        Station(
            id = -6,
            name = "KUSC Classical",
            type = SourceType.STREAM,
            location = "http://kusc.streamguys1.com/kusc-128.mp3",
            iconUrl = "",
            genre = "Classical"
        ),
        Station(
            id = -9,
            name = "Hawaiian Music Live",
            type = SourceType.STREAM,
            location = "http://107.182.231.54:7768/stream",
            iconUrl = "",
            genre = "Hawaiian"
        ),
        Station(
            id = -10,
            name = "KDFC Classical",
            type = SourceType.STREAM,
            location = "http://kdfc.streamguys1.com/kdfc-128.mp3",
            iconUrl = "",
            genre = "Classical"
        )
    )
}