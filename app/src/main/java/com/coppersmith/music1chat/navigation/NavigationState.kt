package com.coppersmith.music1chat.navigation

data class NavigationState(
    val currentCategoryId: Long? = null,
    val currentStationId: Long? = null,
    val isPlaying: Boolean = false
)