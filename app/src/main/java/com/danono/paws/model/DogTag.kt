package com.danono.paws.model

data class DogTag(
    val label: String,
    val category: Category
) {
    enum class Category {
        PERSONALITY,
        BEHAVIOR_WITH_DOGS,
        BEHAVIOR_WITH_HUMANS,
        ACTIVITY_LEVEL,
        SPECIAL_NOTES
    }
}
