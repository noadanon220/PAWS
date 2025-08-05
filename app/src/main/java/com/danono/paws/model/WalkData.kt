package com.danono.paws.model

/**
 * Data class representing walk completion status for a day
 */
data class WalkData(
    val morningCompleted: Boolean = false,
    val afternoonCompleted: Boolean = false,
    val eveningCompleted: Boolean = false
) {
    /**
     * Get completion count (0-3)
     */
    fun getCompletionCount(): Int {
        var count = 0
        if (morningCompleted) count++
        if (afternoonCompleted) count++
        if (eveningCompleted) count++
        return count
    }

    /**
     * Check if all walks are completed
     */
    fun isAllCompleted(): Boolean {
        return morningCompleted && afternoonCompleted && eveningCompleted
    }
}