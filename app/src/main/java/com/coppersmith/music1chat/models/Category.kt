package com.coppersmith.music1chat.models

data class Category(
    val id: Long,
    var name: String,
    var type: CategoryType,
    var includedInNavigation: Boolean = true,
    var sortOrder: Int = 0,
    var stationIds: MutableList<Long> = mutableListOf(),
    var lastRefresh: Long = 0L
) {
    fun getDisplayName(stationCount: Int, startupRestoreComplete: Boolean): String {
        val isSearch = type == CategoryType.SEARCH
        return if (isSearch) {
            if (name.isBlank()) {
                "Search"
            } else if (stationCount == 0 && !startupRestoreComplete) {
                "Search $name"
            } else {
                "Search $name ($stationCount)"
            }
        } else {
            if (stationCount == 0 && !startupRestoreComplete) {
                name
            } else {
                "$name ($stationCount)"
            }
        }
    }
}