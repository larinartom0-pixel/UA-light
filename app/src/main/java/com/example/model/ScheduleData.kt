package com.example.model

enum class ScheduleType {
    PRIMARY,
    SECONDARY
}

data class ScheduleData(
    val queueCode: String,
    val primaryIntervals: List<OutageInterval>,
    val secondaryIntervals: List<OutageInterval> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isLiveFromNetwork: Boolean = true,
    val note: String = ""
) {
    val hasSecondary: Boolean
        get() = secondaryIntervals.isNotEmpty()

    fun getIntervals(type: ScheduleType): List<OutageInterval> {
        return if (type == ScheduleType.SECONDARY && hasSecondary) {
            secondaryIntervals
        } else {
            primaryIntervals
        }
    }
}
