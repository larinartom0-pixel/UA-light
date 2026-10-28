package com.example.model

data class QueueItem(
    val code: String,
    val isPowerOffNow: Boolean = false
) {
    val displayName: String get() = "Черга $code"
    val slug: String get() = code.replace(".", "-")
    val url: String get() = "https://energy-ua.info/cherga/$slug"

    companion object {
        val ALL_QUEUE_CODES = listOf(
            "1.1", "1.2",
            "2.1", "2.2",
            "3.1", "3.2",
            "4.1", "4.2",
            "5.1", "5.2",
            "6.1", "6.2"
        )
    }
}
