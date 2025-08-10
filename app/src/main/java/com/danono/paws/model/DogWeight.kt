package com.danono.paws.model


data class DogWeight(
    val id: String = "",
    val weight: Double = 0.0,
    val createdDate: Long = 0L,
    val lastModified: Long = 0L
)