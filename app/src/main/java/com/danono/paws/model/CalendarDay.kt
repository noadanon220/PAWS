package com.danono.paws.model

/**
 * Data class representing a day in the calendar
 */
data class CalendarDay(
    val date: String,               // Date in yyyy-MM-dd format
    val dayAbbreviation: String,    // Day abbreviation (Mon, Tue, etc.)
    val dayNumber: Int,             // Day of month (1-31)
    val isToday: Boolean,           // Whether this is today's date
    var isSelected: Boolean,        // Whether this day is currently selected
    var walkData: WalkData          // Walk completion data for this day
)