package com.danono.paws.model

data class DogNote(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val createdDate: Long = 0L,
    val lastModified: Long = 0L
)