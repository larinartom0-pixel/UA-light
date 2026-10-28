package com.example.model

/**
 * Represents a single planned power outage interval.
 *
 * @param startMin Minute of day (0..1439) when outage starts (e.g. 18:00 -> 1080)
 * @param endMin Minute of day (0..1440) when outage ends (e.g. 20:00 -> 1200, 24:00/00:00 -> 1440)
 * @param startStr String representation (e.g. "18:00")
 * @param endStr String representation (e.g. "20:00" or "24:00")
 */
data class OutageInterval(
    val startMin: Int,
    val endMin: Int,
    val startStr: String,
    val endStr: String
) {
    val durationMinutes: Int
        get() = (endMin - startMin).coerceAtLeast(0)

    val durationFormatted: String
        get() {
            val hours = durationMinutes / 60
            val mins = durationMinutes % 60
            return when {
                mins == 0 -> "$hours год"
                hours == 0 -> "$mins хв"
                else -> "$hours год $mins хв"
            }
        }

    fun isActive(currentMinutes: Int): Boolean {
        return currentMinutes in startMin until endMin
    }

    fun isPassed(currentMinutes: Int): Boolean {
        return currentMinutes >= endMin
    }

    fun isUpcoming(currentMinutes: Int): Boolean {
        return currentMinutes < startMin
    }
}
