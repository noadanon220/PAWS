package com.danono.paws.model

data class Reminder(
    val id: String = "",
    val title: String = "",
    val reminderType: ReminderType = ReminderType.VET_APPOINTMENT,
    val dateTime: Long = 0L,
    val notes: String = "",
    val dogId: String = "",
    val dogName: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L
)

