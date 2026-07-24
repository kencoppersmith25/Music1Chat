package com.coppersmith.music1chat.data

import com.coppersmith.music1chat.models.SourceType
import com.coppersmith.music1chat.models.Station

object DevelopmentStationData {
    val reliableStations = listOf(
        Station(
            id = -1L,
            name = "KMNO Maui",
            streamUrl = "http://kmno.streamguys1.com/live",
            genre = "Hawaiian",
            city = "Maui",
            sourceType = SourceType.STREAM,
            logoUrl = "https://kmno.org/wp-content/uploads/2021/04/KMNO-Logo-1.png"
        ),
        Station(
            id = -2L,
            name = "99.1 JOY",
            streamUrl = "http://gateway.cdnstream1.com/2808_96.aac",
            genre = "Christian",
            sourceType = SourceType.STREAM,
            logoUrl = "https://www.991joyfm.com/wp-content/themes/joyfm/images/logo.png"
        ),
        Station(
            id=-11L,
            name="Jazz24",
            streamUrl="https://live.wostreaming.net/direct/ppm-jazz24mp3-ibc1",
            genre="Jazz",
            city="Tacoma",
            country="USA",
            sourceType=SourceType.STREAM,
            logoUrl = "https://www.jazz24.org/wp-content/uploads/2016/09/Jazz24-Logo-Horizontal-White.png"
        ),
        Station(
            id=-12L,
            name="WBGO Jazz 88.3",
            streamUrl="https://wbgo.streamguys1.com/wbgo",
            genre="Jazz",
            city="Newark",
            country="USA",
            sourceType=SourceType.STREAM
        ),

        Station(
            id=-13L,
            name="K-LOVE",
            streamUrl="https://maestro.emfcdn.com/stream_for/klove/aac",
            genre="Christian",
            city="Rocklin",
            country="USA",
            sourceType=SourceType.STREAM
        ),
        Station(
            id=-14L,
            name="181.FM The Buzz",
            streamUrl="https://listen.181fm.com/181-buzz_128k.mp3",
            genre="Rock",
            city="Waynesboro",
            country="USA",
            sourceType=SourceType.STREAM
        ),
        Station(
            id=-15L,
            name="SomaFM Groove Salad",
            streamUrl="https://ice2.somafm.com/groovesalad-128-mp3",
            genre="Ambient",
            city="San Francisco",
            country="USA",
            sourceType=SourceType.STREAM
        ),
        Station(
            id=-16L,
            name="BBC Radio 3",
            streamUrl="https://live.amperwave.net/playlist/ppm-jazz24aac256-ibc1.m3u?source=sonos",
            genre="Classical",
            city="London",
            country="UK",
            sourceType=SourceType.STREAM
        )
    )
    val problemStations = listOf(
        Station(
            id = -6L,
            name = "KUSC Classical",
            streamUrl = "http://kusc.streamguys1.com/kusc-128.mp3",
            genre = "Classical",
            city = "Los Angeles",
            country = "USA",
            sourceType = SourceType.STREAM
        ),
        Station(
            id = -9L,
            name = "Hawaiian Music Live",
            streamUrl = "http://107.182.231.54:7768/stream",
            genre = "Hawaiian",
            sourceType = SourceType.STREAM
        ),
        Station(
            id = -10L,
            name = "KDFC Classical",
            streamUrl = "http://kdfc.streamguys1.com/kdfc-128.mp3",
            genre = "Classical",
            city = "San Francisco",
            country = "USA",
            sourceType = SourceType.STREAM
        )
    )
}
