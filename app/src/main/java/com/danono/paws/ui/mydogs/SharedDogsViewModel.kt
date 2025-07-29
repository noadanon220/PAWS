package com.danono.paws.ui.mydogs

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.danono.paws.model.Dog
import com.google.firebase.firestore.FirebaseFirestore

class SharedDogsViewModel : ViewModel() {

    // List of all dogs with their IDs
    private val _dogs = MutableLiveData<List<Pair<Dog, String>>>(emptyList())
    val dogs: LiveData<List<Pair<Dog, String>>> = _dogs

    // Currently selected dog for profile view
    private val _selectedDog = MutableLiveData<Dog?>()
    val selectedDog: LiveData<Dog?> = _selectedDog

    // Currently selected dog ID for Firebase operations
    private val _selectedDogId = MutableLiveData<String?>()
    val selectedDogId: LiveData<String?> = _selectedDogId

    fun addDog(dog: Dog, dogId: String) {
        val currentList = _dogs.value.orEmpty().toMutableList()
        currentList.add(Pair(dog, dogId))
        _dogs.value = currentList
    }

    fun selectDog(dog: Dog, dogId: String) {
        _selectedDog.value = dog
        _selectedDogId.value = dogId
    }

    fun loadDogsFromFirestore(userId: String) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("dogs")
            .get()
            .addOnSuccessListener { result ->
                val dogsList = mutableListOf<Pair<Dog, String>>()
                for (document in result) {
                    val dog = document.toObject(Dog::class.java)
                    val dogId = document.id
                    dogsList.add(Pair(dog, dogId))
                }
                _dogs.value = dogsList
            }
            .addOnFailureListener { exception ->
                Log.e("SharedDogsViewModel", "Failed to load dogs: ${exception.message}")
            }
    }
}