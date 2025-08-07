package com.danono.paws.model


enum class ReminderType(val displayName: String, val emoji: String) {
    VET_APPOINTMENT("Vet Appointment", "💉"),
    VACCINATION("Vaccination", "💉"),
    GROOMING("Grooming", "✂️"),
    MEDICATION("Medication", "💊"),
    TRAINING("Training", "🏅"),
    WALKING("Walk", "🐾"),
    FEEDING("Feeding", "🍽️"),
    BATH("Bath", "🛁"),
    NAIL_CLIPPING("Nail Clipping", "🦶"),
    DEWORMING("Deworming", "🪱"),
    CHECKUP("Checkup", "👩‍⚕️"),
    OTHER("Other", "📝");

    companion object {
        fun fromString(name: String?): ReminderType {
            return values().find { it.name == name } ?: OTHER
        }
    }
}

