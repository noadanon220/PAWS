package com.danono.paws.model

data class Dog(
    val name: String = "",
    val birthDate: Long = 0L,
    val gender: Boolean = true,
    val weight: Double = 0.0,
    val color: List<String> = emptyList(),
    val imageUrl: String = "",
    val tags: List<String> = emptyList(),
    val breedName: String = ""
)
