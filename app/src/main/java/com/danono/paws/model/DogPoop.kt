package com.danono.paws.model

data class DogPoop(
    val id: String = "",
    val color: String = "", // Color name or hex value
    val consistency: String = "", //  "Normal", "Soft", "Hard", "Liquid"
    val notes: String = "",
    val imageUrl: String = "", // Optional image
    val createdDate: Long = 0L,
    val lastModified: Long = 0L
)