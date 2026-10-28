package com.example.model

data class SavedQueue(
    val id: String,
    val name: String,
    val queueCode: String,
    val isCustomUrl: Boolean = false,
    val customUrl: String = ""
) {
    val displayQueueCode: String
        get() = queueCode.replace("-", ".")

    val targetUrl: String
        get() = if (isCustomUrl && customUrl.isNotBlank()) {
            customUrl
        } else {
            val slug = queueCode.replace(".", "-")
            "https://energy-ua.info/cherga/$slug"
        }
}
