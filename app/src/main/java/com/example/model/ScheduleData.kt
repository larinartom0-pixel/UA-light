package com.example.model

enum class ScheduleDay {
    TODAY,
    TOMORROW
}

enum class ScheduleType {
    PRIMARY,
    SECONDARY
}

data class ScheduleData(
    val queueCode: String,
    val primaryIntervals: List<OutageInterval>,
    val secondaryIntervals: List<OutageInterval> = emptyList(),
    val tomorrowPrimaryIntervals: List<OutageInterval> = emptyList(),
    val tomorrowSecondaryIntervals: List<OutageInterval> = emptyList(),
    val todayDate: String = "",
    val tomorrowDate: String = "",
    val tomorrowAvailable: Boolean = false,
    val isEmergency: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isLiveFromNetwork: Boolean = true,
    val note: String = ""
) {
    val hasTomorrow: Boolean
        get() = tomorrowAvailable || tomorrowPrimaryIntervals.isNotEmpty() || tomorrowSecondaryIntervals.isNotEmpty()

    fun hasSecondaryForDay(day: ScheduleDay): Boolean {
        return when (day) {
            ScheduleDay.TODAY -> secondaryIntervals.isNotEmpty()
            ScheduleDay.TOMORROW -> tomorrowSecondaryIntervals.isNotEmpty()
        }
    }

    fun getIntervals(day: ScheduleDay, type: ScheduleType): List<OutageInterval> {
        return when (day) {
            ScheduleDay.TODAY -> {
                if (type == ScheduleType.SECONDARY && secondaryIntervals.isNotEmpty()) {
                    secondaryIntervals
                } else {
                    primaryIntervals
                }
            }
            ScheduleDay.TOMORROW -> {
                if (type == ScheduleType.SECONDARY && tomorrowSecondaryIntervals.isNotEmpty()) {
                    tomorrowSecondaryIntervals
                } else {
                    tomorrowPrimaryIntervals
                }
            }
        }
    }

    fun getIntervals(type: ScheduleType): List<OutageInterval> {
        return getIntervals(ScheduleDay.TODAY, type)
    }
}
