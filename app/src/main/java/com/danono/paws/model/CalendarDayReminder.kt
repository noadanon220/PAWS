package com.danono.paws.model

/**
 * Data class representing a calendar day for reminders screen
 */
data class CalendarDayReminder(
    val date: String,               // Date in yyyy-MM-dd format
    val dayName: String,            // Day name (S, M, T, W, T, F, S)
    val dayNumber: Int,             // Day of month (1-31)
    val isToday: Boolean,           // Whether this is today's date
    var isSelected: Boolean,        // Whether this day is currently selected
    val hasReminders: Boolean,      // Whether this day has reminders
    val reminderCount: Int          // Number of reminders on this day
)