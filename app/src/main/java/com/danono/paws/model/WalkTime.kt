package com.danono.paws.model

data class WalkTime(
    val id: String,
    val name: String,
    val time: String, // Format: "HH:mm"
    val emoji: String,
    val isCompleted: Boolean = false,
    val date: String = "", // For tracking specific dates
    val recurrence: WalkRecurrence = WalkRecurrence.DAILY
)

enum class WalkRecurrence {
    DAILY,
    WEEKDAYS,
    WEEKENDS,
    CUSTOM
}
