package com.example.model

sealed interface PowerStatus {
    /**
     * Light is currently OFF.
     */
    data class NoPower(
        val currentInterval: OutageInterval,
        val secondsUntilOn: Long
    ) : PowerStatus {
        val hoursRemaining: Long get() = secondsUntilOn / 3600
        val minutesRemaining: Long get() = (secondsUntilOn % 3600) / 60
        val secondsRemaining: Long get() = secondsUntilOn % 60

        val formattedCountdown: String
            get() = String.format("%02d:%02d:%02d", hoursRemaining, minutesRemaining, secondsRemaining)

        val humanReadableRemaining: String
            get() {
                val h = hoursRemaining
                val m = minutesRemaining
                return when {
                    h > 0 && m > 0 -> "$h год $m хв"
                    h > 0 -> "$h год"
                    m > 0 -> "$m хв"
                    else -> "${secondsRemaining} сек"
                }
            }
    }

    /**
     * Light is currently ON, with an upcoming outage today.
     */
    data class HasPowerUpcomingOutage(
        val nextInterval: OutageInterval,
        val secondsUntilOff: Long
    ) : PowerStatus {
        val hoursRemaining: Long get() = secondsUntilOff / 3600
        val minutesRemaining: Long get() = (secondsUntilOff % 3600) / 60
        val secondsRemaining: Long get() = secondsUntilOff % 60

        val formattedCountdown: String
            get() = String.format("%02d:%02d:%02d", hoursRemaining, minutesRemaining, secondsRemaining)

        val humanReadableRemaining: String
            get() {
                val h = hoursRemaining
                val m = minutesRemaining
                return when {
                    h > 0 && m > 0 -> "$h год $m хв"
                    h > 0 -> "$h год"
                    m > 0 -> "$m хв"
                    else -> "${secondsRemaining} сек"
                }
            }
    }

    /**
     * All planned outages for today have passed.
     */
    object AllOutagesFinished : PowerStatus

    /**
     * No outages planned today or light is available all day.
     */
    object NoOutagesToday : PowerStatus
}
