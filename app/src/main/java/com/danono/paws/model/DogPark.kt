package com.danono.paws.model

data class DogPark(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rating: Float = 0f,
    val description: String = "",
    val facilities: List<String> = emptyList(), // e.g., "Water fountain", "Fenced area", "Parking"
    val openingHours: String = "",
    val phoneNumber: String = "",
    val website: String = "",
    val photos: List<String> = emptyList(),
    val distance: Double = 0.0, // Distance from user's location in KM
    val isFavorite: Boolean = false
)

data class ParkFacility(
    val name: String,
    val icon: Int // drawable resource
)