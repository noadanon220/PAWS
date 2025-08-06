package com.danono.paws.ui.reminders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RemindersViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is reminders Fragment"
    }
    val text: LiveData<String> = _text

    private val _reminders = MutableLiveData<List<com.danono.paws.model.Reminder>>()
    val reminders: LiveData<List<com.danono.paws.model.Reminder>> = _reminders

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadReminders(userId: String) {
        _isLoading.value = true
        // Firebase loading logic will be handled in the Fragment
        _isLoading.value = false
    }

    fun updateReminders(remindersList: List<com.danono.paws.model.Reminder>) {
        _reminders.value = remindersList
    }
}