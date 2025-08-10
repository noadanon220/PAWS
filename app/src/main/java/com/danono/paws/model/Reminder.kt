package com.danono.paws.model

data class Reminder(
    val id: String = "",
    // Title now mirrors the selected type's displayName to keep backward compatibility
    val title: String = "",
    val reminderType: ReminderType = ReminderType.VET_APPOINTMENT,
    val dateTime: Long = 0L,
    val notes: String = "",
    val dogId: String = "",
    val dogName: String = "",
    val location: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L
)
