package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("svitlo_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SELECTED_QUEUE = "selected_queue_code"
        private const val KEY_SCHEDULE_TYPE = "selected_schedule_type"
    }

    fun getSelectedQueue(): String {
        return prefs.getString(KEY_SELECTED_QUEUE, "4.2") ?: "4.2"
    }

    fun setSelectedQueue(queueCode: String) {
        prefs.edit().putString(KEY_SELECTED_QUEUE, queueCode).apply()
    }

    fun getSelectedScheduleType(): String {
        return prefs.getString(KEY_SCHEDULE_TYPE, "PRIMARY") ?: "PRIMARY"
    }

    fun setSelectedScheduleType(type: String) {
        prefs.edit().putString(KEY_SCHEDULE_TYPE, type).apply()
    }
}
