package com.coppersmith.music1chat.models

data class Station(
    val id:Long,
    var name:String,
    var streamUrl:String,
    var genre:String,
    var city:String="",
    var country:String="",
    var logoUrl:String="",
    var sourceType:SourceType=SourceType.STREAM,
    var includedInNavigation:Boolean=true,
    var failedThisSession:Boolean=false,
    var resolvedStreamUrl:String="",
    var streamVerified:Boolean=false,
    var lastVerified:Long=0L
)