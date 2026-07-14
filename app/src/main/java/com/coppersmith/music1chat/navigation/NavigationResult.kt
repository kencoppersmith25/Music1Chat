package com.coppersmith.music1chat.navigation

data class NavigationResult(
    val state: NavigationState,
    val selectionChanged: Boolean = false,
    val shouldStartPlayback: Boolean = false,
    val statusMessage: String? = null
)