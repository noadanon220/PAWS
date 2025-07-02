package com.danono.paws.ui.mydogs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.danono.paws.model.Dog

class SharedDogsViewModel : ViewModel() {

    // Corrected type: MutableLiveData<List<Dog>>, not MutableList
    private val _dogs = MutableLiveData<List<Dog>>(emptyList())
    val dogs: LiveData<List<Dog>> = _dogs

    fun addDog(dog: Dog) {
        _dogs.value = _dogs.value.orEmpty() + dog
    }
}
