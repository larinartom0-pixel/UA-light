package com.example.data

import android.content.Context
import com.example.model.OutageInterval
import com.example.model.SavedQueue
import com.example.model.ScheduleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class PowerScheduleRepository(private val context: Context) {

    private val preferencesManager = PreferencesManager(context)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun getSchedule(queue: SavedQueue): ScheduleData = withContext(Dispatchers.IO) {
        // First check if user configured custom manual intervals for this queue
        val custom = preferencesManager.getCustomIntervals(queue.queueCode)
        if (custom != null && custom.isNotEmpty()) {
            return@withContext ScheduleData(
                queueCode = queue.queueCode,
                intervals = custom,
                lastUpdated = System.currentTimeMillis(),
                isLiveFromNetwork = false,
                note = "Ручний / Власний графік"
            )
        }

        val url = queue.targetUrl
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "uk-UA,uk;q=0.9,en;q=0.8")
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            // Check if blocked by Cloudflare anti-bot
            val isCloudflare = body.contains("Just a moment", ignoreCase = true) ||
                    body.contains("cf_chl_opt", ignoreCase = true) ||
                    response.code == 403 || response.code == 503

            if (isCloudflare) {
                // Cloudflare challenge returned -> fallback to reliable preset schedule for this queue
                val preset = PowerScheduleParser.getPresetScheduleForQueue(queue.queueCode)
                return@withContext ScheduleData(
                    queueCode = queue.queueCode,
                    intervals = preset,
                    lastUpdated = System.currentTimeMillis(),
                    isLiveFromNetwork = false,
                    note = "Захист Cloudflare на energy-ua.info. Відображено розрахунковий графік"
                )
            }

            // Parse schedule intervals with regex
            val intervals = PowerScheduleParser.parseOutagesFromText(body)

            if (intervals.isNotEmpty()) {
                ScheduleData(
                    queueCode = queue.queueCode,
                    intervals = intervals,
                    lastUpdated = System.currentTimeMillis(),
                    isLiveFromNetwork = true,
                    note = "Оновлено з сайту energy-ua.info"
                )
            } else {
                // No matches found on the page
                ScheduleData(
                    queueCode = queue.queueCode,
                    intervals = emptyList(),
                    lastUpdated = System.currentTimeMillis(),
                    isLiveFromNetwork = true,
                    note = "На сторінці не знайдено відключень або світло є весь день"
                )
            }
        } catch (e: Exception) {
            // Network error (offline / timeout) -> fallback to presets
            val preset = PowerScheduleParser.getPresetScheduleForQueue(queue.queueCode)
            ScheduleData(
                queueCode = queue.queueCode,
                intervals = preset,
                lastUpdated = System.currentTimeMillis(),
                isLiveFromNetwork = false,
                note = "Офлайн-режим: немає зв'язку із сервером"
            )
        }
    }

    fun saveCustomSchedule(queueCode: String, intervals: List<OutageInterval>) {
        preferencesManager.saveCustomIntervals(queueCode, intervals)
    }

    fun clearCustomSchedule(queueCode: String) {
        preferencesManager.clearCustomIntervals(queueCode)
    }

    fun getSavedQueues(): List<SavedQueue> = preferencesManager.getSavedQueues()

    fun saveQueues(queues: List<SavedQueue>) = preferencesManager.saveQueues(queues)

    fun getSelectedQueueId(): String = preferencesManager.getSelectedQueueId()

    fun setSelectedQueueId(id: String) = preferencesManager.setSelectedQueueId(id)
}
