package com.danono.paws.model
/**
 * Data class representing a day item in the day selector
 */
data class DayItem(
    val dayName: String,        // Day abbreviation (Mon, Tue, etc.)
    val dayNumber: Int,         // Day of month (1-31)
    val isToday: Boolean,       // Whether this is today's date
    val date: String,           // Full date in yyyy-MM-dd format
    val completedWalks: Int,    // Number of completed walks for this day
    val totalWalks: Int,        // Total number of scheduled walks for this day
    val monthName: String       // Month abbreviation (Jan, Feb, etc.)
)