package com.example.model

data class ScheduleData(
    val queueCode: String,
    val intervals: List<OutageInterval>,
    val lastUpdated: Long,
    val isLiveFromNetwork: Boolean,
    val note: String,
    val rawTextSnippet: String = ""
)
