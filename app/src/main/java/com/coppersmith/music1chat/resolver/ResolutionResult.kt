package com.coppersmith.music1chat.resolver

data class ResolutionResult(
    val success: Boolean,
    val resolvedUrl: String? = null,
    val verified: Boolean = false,
    val errorMessage: String? = null
)