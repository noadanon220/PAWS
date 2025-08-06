package com.danono.paws.model

data class Reminder(
    val id: String = "",
    val title: String = "",
    val location: String = "",
    val dateTime: Long = 0L,
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val reminderType: ReminderType = ReminderType.VET_APPOINTMENT
)

enum class ReminderType(val displayName: String, val emoji: String) {
    VET_APPOINTMENT("Vet Appointment", "🏥"),
    VACCINATION("Vaccination", "💉"),
    GROOMING("Grooming", "✂️"),
    MEDICATION("Medication", "💊"),
    TRAINING("Training", "🎾"),
    WALKING("Walking", "🚶"),
    FEEDING("Feeding", "🍽️"),
    OTHER("Other", "📅")
}