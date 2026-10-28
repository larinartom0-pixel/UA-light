package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.OutageInterval
import com.example.model.SavedQueue
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("svitlo_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_QUEUES = "saved_queues"
        private const val KEY_SELECTED_ID = "selected_queue_id"
        private const val KEY_CUSTOM_PREFIX = "custom_intervals_"
    }

    fun getSavedQueues(): List<SavedQueue> {
        val jsonStr = prefs.getString(KEY_QUEUES, null)
        if (jsonStr.isNullOrEmpty()) {
            val defaults = listOf(
                SavedQueue(id = "home", name = "🏠 Дім", queueCode = "4.2"),
                SavedQueue(id = "work", name = "💼 Робота", queueCode = "2.1"),
                SavedQueue(id = "parents", name = "👵 Батьки", queueCode = "1.2")
            )
            saveQueues(defaults)
            return defaults
        }

        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<SavedQueue>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SavedQueue(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        queueCode = obj.getString("queueCode"),
                        isCustomUrl = obj.optBoolean("isCustomUrl", false),
                        customUrl = obj.optString("customUrl", "")
                    )
                )
            }
            if (list.isEmpty()) {
                listOf(SavedQueue(id = "home", name = "🏠 Дім", queueCode = "4.2"))
            } else list
        } catch (e: Exception) {
            listOf(SavedQueue(id = "home", name = "🏠 Дім", queueCode = "4.2"))
        }
    }

    fun saveQueues(queues: List<SavedQueue>) {
        val array = JSONArray()
        for (q in queues) {
            val obj = JSONObject()
            obj.put("id", q.id)
            obj.put("name", q.name)
            obj.put("queueCode", q.queueCode)
            obj.put("isCustomUrl", q.isCustomUrl)
            obj.put("customUrl", q.customUrl)
            array.put(obj)
        }
        prefs.edit().putString(KEY_QUEUES, array.toString()).apply()
    }

    fun getSelectedQueueId(): String {
        return prefs.getString(KEY_SELECTED_ID, "home") ?: "home"
    }

    fun setSelectedQueueId(id: String) {
        prefs.edit().putString(KEY_SELECTED_ID, id).apply()
    }

    fun saveCustomIntervals(queueCode: String, intervals: List<OutageInterval>) {
        val array = JSONArray()
        for (i in intervals) {
            val obj = JSONObject()
            obj.put("sMin", i.startMin)
            obj.put("eMin", i.endMin)
            obj.put("sStr", i.startStr)
            obj.put("eStr", i.endStr)
            array.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_PREFIX + queueCode, array.toString()).apply()
    }

    fun getCustomIntervals(queueCode: String): List<OutageInterval>? {
        val str = prefs.getString(KEY_CUSTOM_PREFIX + queueCode, null) ?: return null
        return try {
            val array = JSONArray(str)
            val list = mutableListOf<OutageInterval>()
            for (idx in 0 until array.length()) {
                val obj = array.getJSONObject(idx)
                list.add(
                    OutageInterval(
                        startMin = obj.getInt("sMin"),
                        endMin = obj.getInt("eMin"),
                        startStr = obj.getString("sStr"),
                        endStr = obj.getString("eStr")
                    )
                )
            }
            list.sortedBy { it.startMin }
        } catch (e: Exception) {
            null
        }
    }

    fun clearCustomIntervals(queueCode: String) {
        prefs.edit().remove(KEY_CUSTOM_PREFIX + queueCode).apply()
    }
}
