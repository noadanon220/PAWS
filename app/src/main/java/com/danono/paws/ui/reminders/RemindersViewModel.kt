package com.danono.paws.ui.reminders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.danono.paws.model.Reminder

class RemindersViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is reminders Fragment"
    }
    val text: LiveData<String> = _text

    private val _reminders = MutableLiveData<List<Reminder>>()
    val reminders: LiveData<List<Reminder>> = _reminders

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadReminders(userId: String) {
        _isLoading.value = true
        _isLoading.value = false
    }

    fun updateReminders(remindersList: List<Reminder>) {
        _reminders.value = remindersList
    }
}
