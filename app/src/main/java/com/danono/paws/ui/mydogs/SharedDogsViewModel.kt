package com.danono.paws.ui.mydogs

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.danono.paws.model.Dog
import com.google.firebase.firestore.FirebaseFirestore

class SharedDogsViewModel : ViewModel() {

    // List of all dogs
    private val _dogs = MutableLiveData<List<Dog>>(emptyList())
    val dogs: LiveData<List<Dog>> = _dogs

    // Currently selected dog for profile view
    private val _selectedDog = MutableLiveData<Dog?>()
    val selectedDog: LiveData<Dog?> = _selectedDog

    fun addDog(dog: Dog) {
        _dogs.value = _dogs.value.orEmpty() + dog
    }

    fun selectDog(dog: Dog) {
        _selectedDog.value = dog
    }

    fun loadDogsFromFirestore(userId: String) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("dogs")
            .get()
            .addOnSuccessListener { result ->
                val dogsList = result.mapNotNull { it.toObject(Dog::class.java) }
                _dogs.value = dogsList
            }
            .addOnFailureListener {
                Log.e("SharedDogsViewModel", "Failed to load dogs: ${it.message}")
            }
    }
}