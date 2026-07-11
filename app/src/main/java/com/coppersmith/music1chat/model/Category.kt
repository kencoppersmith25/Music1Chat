package com.coppersmith.music1chat.model

data class Category(

    val id: Long,

    var name: String,

    var type: CategoryType = CategoryType.USER,

    var searchTerm: String? = null,

    var includedInNavigation: Boolean = true,

    var sortOrder: Int = 0
)