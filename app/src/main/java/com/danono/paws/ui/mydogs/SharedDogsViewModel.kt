package com.danono.paws.ui.mydogs

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danono.paws.model.Dog
import com.danono.paws.utilities.FirebaseDataManager
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class SharedDogsViewModel : ViewModel() {

    private val firebaseDataManager = FirebaseDataManager.getInstance()
    private var dogsListener: ListenerRegistration? = null

    // List of all dogs with their IDs
    private val _dogs = MutableLiveData<List<Pair<Dog, String>>>(emptyList())
    val dogs: LiveData<List<Pair<Dog, String>>> = _dogs

    // Currently selected dog for profile view
    private val _selectedDog = MutableLiveData<Dog?>()
    val selectedDog: LiveData<Dog?> = _selectedDog

    // Currently selected dog ID for Firebase operations
    private val _selectedDogId = MutableLiveData<String?>()
    val selectedDogId: LiveData<String?> = _selectedDogId

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        // Start listening to dogs data when ViewModel is created
        setupDogsListener()
    }

    /**
     * Add a dog to the local list (for immediate UI updates)
     * This is called when a dog is successfully saved to Firebase
     */
    fun addDog(dog: Dog, dogId: String) {
        val currentList = _dogs.value.orEmpty().toMutableList()
        currentList.add(Pair(dog, dogId))
        _dogs.value = currentList
    }

    /**
     * Select a dog for detailed view
     */
    fun selectDog(dog: Dog, dogId: String) {
        _selectedDog.value = dog
        _selectedDogId.value = dogId
    }

    /**
     * Load dogs from Firestore using the FirebaseDataManager
     */
    fun loadDogsFromFirestore() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = firebaseDataManager.getDogs()
            result.fold(
                onSuccess = { dogsList ->
                    _dogs.value = dogsList
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    Log.e("SharedDogsViewModel", "Failed to load dogs: ${exception.message}")
                    _error.value = "Failed to load dogs: ${exception.message}"
                    _dogs.value = emptyList()
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Setup real-time listener for dogs using FirebaseDataManager
     */
    private fun setupDogsListener() {
        dogsListener = firebaseDataManager.addDogsListener { dogsList ->
            _dogs.value = dogsList
            _isLoading.value = false
            _error.value = null
        }

        if (dogsListener == null) {
            _error.value = "Failed to setup dogs listener - user not logged in"
        }
    }

    /**
     * Update a specific dog
     */
    fun updateDog(dogId: String, updatedDog: Dog) {
        viewModelScope.launch {
            val result = firebaseDataManager.updateDog(dogId, updatedDog)
            result.fold(
                onSuccess = {
                    // Update local list
                    val currentList = _dogs.value.orEmpty().toMutableList()
                    val index = currentList.indexOfFirst { it.second == dogId }
                    if (index != -1) {
                        currentList[index] = Pair(updatedDog, dogId)
                        _dogs.value = currentList
                    }

                    // Update selected dog if it's the same one
                    if (_selectedDogId.value == dogId) {
                        _selectedDog.value = updatedDog
                    }
                },
                onFailure = { exception ->
                    Log.e("SharedDogsViewModel", "Failed to update dog: ${exception.message}")
                    _error.value = "Failed to update dog: ${exception.message}"
                }
            )
        }
    }

    /**
     * Delete a dog
     */
    fun deleteDog(dogId: String) {
        viewModelScope.launch {
            val result = firebaseDataManager.deleteDog(dogId)
            result.fold(
                onSuccess = {
                    // Remove from local list
                    val currentList = _dogs.value.orEmpty().toMutableList()
                    currentList.removeAll { it.second == dogId }
                    _dogs.value = currentList

                    // Clear selection if this dog was selected
                    if (_selectedDogId.value == dogId) {
                        _selectedDog.value = null
                        _selectedDogId.value = null
                    }
                },
                onFailure = { exception ->
                    Log.e("SharedDogsViewModel", "Failed to delete dog: ${exception.message}")
                    _error.value = "Failed to delete dog: ${exception.message}"
                }
            )
        }
    }

    /**
     * Refresh dogs data
     */
    fun refreshDogs() {
        loadDogsFromFirestore()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Get dog by ID
     */
    fun getDogById(dogId: String): Dog? {
        return _dogs.value?.find { it.second == dogId }?.first
    }

    /**
     * Check if a dog exists in the current list
     */
    fun hasDog(dogId: String): Boolean {
        return _dogs.value?.any { it.second == dogId } == true
    }

    /**
     * Get the count of dogs
     */
    fun getDogsCount(): Int {
        return _dogs.value?.size ?: 0
    }

    override fun onCleared() {
        super.onCleared()
        // Remove the listener when ViewModel is cleared
        dogsListener?.remove()
    }
}