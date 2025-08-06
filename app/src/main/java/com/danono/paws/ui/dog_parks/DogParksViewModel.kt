package com.danono.paws.ui.dog_parks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danono.paws.model.DogPark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DogParksViewModel : ViewModel() {

    private val _parks = MutableLiveData<List<DogPark>>()
    val parks: LiveData<List<DogPark>> = _parks

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadParksNearLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Simulate API call delay
                delay(1500)

                // In a real app, you would call a places API here
                // For now, we'll return sample parks near the location
                val sampleParks = getSampleParksNearLocation(latitude, longitude)
                _parks.value = sampleParks

            } catch (e: Exception) {
                _error.value = "Failed to load parks: ${e.message}"
                _parks.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchParks(query: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Simulate API call delay
                delay(1000)

                // In a real app, you would search using Google Places API
                val searchResults = getSampleParksNearLocation(latitude, longitude)
                    .filter { park ->
                        park.name.contains(query, ignoreCase = true) ||
                                park.address.contains(query, ignoreCase = true) ||
                                park.facilities.any { it.contains(query, ignoreCase = true) }
                    }

                _parks.value = searchResults

            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSampleParks() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                delay(800)

                _parks.value = getGlobalSampleParks()

            } catch (e: Exception) {
                _error.value = "Failed to load sample parks: ${e.message}"
                _parks.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getSampleParksNearLocation(userLat: Double, userLon: Double): List<DogPark> {
        // Generate sample parks around user location
        return listOf(
            DogPark(
                id = "1",
                name = "Central Dog Park",
                address = "123 Main St, Your City",
                latitude = userLat + 0.01,
                longitude = userLon + 0.01,
                rating = 4.5f,
                description = "A large fenced dog park with separate areas for small and large dogs.",
                facilities = listOf("Fenced Area", "Water Station", "Parking", "Benches"),
                openingHours = "6:00 AM - 10:00 PM",
                phoneNumber = "+1-555-0123"
            ),
            DogPark(
                id = "2",
                name = "Riverside Dog Run",
                address = "456 River Rd, Your City",
                latitude = userLat - 0.008,
                longitude = userLon + 0.015,
                rating = 4.2f,
                description = "Beautiful dog park along the river with trails and swimming access.",
                facilities = listOf("River Access", "Trails", "Parking", "Waste Bags"),
                openingHours = "Dawn to Dusk",
                phoneNumber = "+1-555-0456"
            ),
            DogPark(
                id = "3",
                name = "Neighborhood Dog Area",
                address = "789 Oak Ave, Your City",
                latitude = userLat + 0.005,
                longitude = userLon - 0.012,
                rating = 3.8f,
                description = "Small neighborhood dog park perfect for local walks.",
                facilities = listOf("Small Area", "Benches", "Waste Bags"),
                openingHours = "24/7",
                phoneNumber = ""
            ),
            DogPark(
                id = "4",
                name = "Adventure Dog Park",
                address = "321 Hill St, Your City",
                latitude = userLat - 0.015,
                longitude = userLon - 0.008,
                rating = 4.7f,
                description = "Large off-leash area with agility equipment and training facilities.",
                facilities = listOf("Agility Equipment", "Training Area", "Large Field", "Parking", "Water Station"),
                openingHours = "5:00 AM - 11:00 PM",
                phoneNumber = "+1-555-0789"
            ),
            DogPark(
                id = "5",
                name = "Shady Grove Dog Park",
                address = "654 Pine St, Your City",
                latitude = userLat + 0.018,
                longitude = userLon - 0.005,
                rating = 4.1f,
                description = "Quiet park with lots of shade trees and walking paths.",
                facilities = listOf("Shaded Areas", "Walking Paths", "Benches", "Water Fountain"),
                openingHours = "6:00 AM - 9:00 PM",
                phoneNumber = "+1-555-0654"
            )
        )
    }

    private fun getGlobalSampleParks(): List<DogPark> {
        // Default sample parks when no location is available
        return listOf(
            DogPark(
                id = "sample1",
                name = "Downtown Dog Park",
                address = "Downtown Area",
                latitude = 32.0853, // Tel Aviv coordinates as example
                longitude = 34.7818,
                rating = 4.3f,
                description = "Popular urban dog park in the city center.",
                facilities = listOf("Fenced Area", "Water Station", "Parking"),
                openingHours = "6:00 AM - 10:00 PM"
            ),
            DogPark(
                id = "sample2",
                name = "Beach Dog Area",
                address = "Coastal Road",
                latitude = 32.0892,
                longitude = 34.7767,
                rating = 4.6f,
                description = "Dog-friendly beach area with sand and sea access.",
                facilities = listOf("Beach Access", "Showers", "Parking"),
                openingHours = "24/7"
            ),
            DogPark(
                id = "sample3",
                name = "Forest Trail Dogs",
                address = "Forest Area",
                latitude = 32.0799,
                longitude = 34.7899,
                rating = 4.0f,
                description = "Natural forest area with hiking trails for dogs.",
                facilities = listOf("Forest Trails", "Natural Environment", "Parking"),
                openingHours = "Dawn to Dusk"
            )
        )
    }
}