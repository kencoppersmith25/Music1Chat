package com.coppersmith.music1chat.models

data class Category(
    val id:Long,
    var name:String,
    var type:CategoryType,
    var includedInNavigation:Boolean=true,
    var sortOrder:Int=0,
    var stationIds:MutableList<Long> = mutableListOf(),
    var lastRefresh:Long=0L
)