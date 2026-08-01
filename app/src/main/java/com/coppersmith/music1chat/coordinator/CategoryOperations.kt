package com.coppersmith.music1chat.coordinator

import com.coppersmith.music1chat.session.PlaybackSessionState

data class CategoryDeletionPlan(
    val deletingCurrent: Boolean,
    val preferredReplacementKey: String?
)

object CategoryOperations {

    fun plan(
        deletingKey: String,
        currentState: PlaybackSessionState,
        orderedKeys: List<String>
    ): CategoryDeletionPlan {
        val currentKey =
            if (currentState.isSearch) {
                "search:${currentState.categoryName}"
            } else {
                "category:${currentState.categoryId}"
            }

        val deletingCurrent =
            currentKey.equals(
                deletingKey,
                ignoreCase = true
            )

        val deletingIndex =
            orderedKeys.indexOfFirst { key ->
                key.equals(
                    deletingKey,
                    ignoreCase = true
                )
            }

        val preferredReplacementKey =
            if (
                deletingIndex >= 0 &&
                orderedKeys.size > 1
            ) {
                orderedKeys[
                    (deletingIndex + 1) % orderedKeys.size
                ]
            } else {
                null
            }

        return CategoryDeletionPlan(
            deletingCurrent = deletingCurrent,
            preferredReplacementKey = preferredReplacementKey
        )
    }

    fun replacementKey(
        plan: CategoryDeletionPlan,
        remainingKeys: List<String>
    ): String? {
        if (remainingKeys.isEmpty()) {
            return null
        }

        val preferredKey =
            plan.preferredReplacementKey
                ?.let { preferred ->
                    remainingKeys.firstOrNull { remaining ->
                        remaining.equals(
                            preferred,
                            ignoreCase = true
                        )
                    }
                }

        return preferredKey
            ?: remainingKeys.firstOrNull()
    }
}