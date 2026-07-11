package com.coppersmith.music1chat.repository

import com.coppersmith.music1chat.model.Category
import com.coppersmith.music1chat.model.CategoryType

class CategoryRepository {

    private val categories = mutableListOf<Category>()

    fun getAll(): List<Category> =
        categories.sortedBy { it.sortOrder }

    fun getNavigationCategories(): List<Category> =
        categories
            .filter { it.includedInNavigation }
            .sortedBy { it.sortOrder }

    fun getById(id: Long): Category? =
        categories.find { it.id == id }

    fun add(category: Category) {
        categories.add(category)
    }

    fun remove(categoryId: Long) {
        categories.removeAll { it.id == categoryId }
    }

    fun rename(categoryId: Long, newName: String) {
        getById(categoryId)?.name = newName
    }

    fun setNavigation(categoryId: Long, enabled: Boolean) {
        getById(categoryId)?.includedInNavigation = enabled
    }

    fun move(categoryId: Long, newPosition: Int) {

        val ordered = categories.sortedBy { it.sortOrder }.toMutableList()

        val category = ordered.find { it.id == categoryId } ?: return

        ordered.remove(category)

        ordered.add(
            newPosition.coerceIn(0, ordered.size),
            category
        )

        ordered.forEachIndexed { index, item ->
            item.sortOrder = index
        }

        categories.clear()
        categories.addAll(ordered)
    }

    fun clear() {
        categories.clear()
    }

    fun seedDefaults() {

        if (categories.isNotEmpty()) return

        add(
            Category(
                id = 1,
                name = "Classical",
                type = CategoryType.STANDARD,
                sortOrder = 0
            )
        )

        add(
            Category(
                id = 2,
                name = "Jazz",
                type = CategoryType.STANDARD,
                sortOrder = 1
            )
        )

        add(
            Category(
                id = 3,
                name = "Rock",
                type = CategoryType.STANDARD,
                sortOrder = 2
            )
        )
    }
}