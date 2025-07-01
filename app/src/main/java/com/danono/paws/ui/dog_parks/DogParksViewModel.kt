package com.danono.paws.ui.dog_parks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DogParksViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dog parks Fragment"
    }
    val text: LiveData<String> = _text
}