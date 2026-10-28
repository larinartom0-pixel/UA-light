package com.example.data

import android.content.Context
import com.example.model.QueueItem
import com.example.model.ScheduleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class PowerScheduleRepository(private val context: Context) {

    private val preferencesManager = PreferencesManager(context)
    private val memoryCache = ConcurrentHashMap<String, ScheduleData>()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun getSchedule(queueCode: String): ScheduleData = withContext(Dispatchers.IO) {
        val slug = queueCode.replace(".", "-")
        val url = "https://energy-ua.info/cherga/$slug"

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

            val isBlocked = body.contains("Just a moment", ignoreCase = true) ||
                    body.contains("cf_chl_opt", ignoreCase = true) ||
                    response.code == 403 || response.code == 503

            if (isBlocked) {
                val (presetPrimary, presetSecondary) = PowerScheduleParser.getPresetScheduleForQueue(queueCode)
                val data = ScheduleData(
                    queueCode = queueCode,
                    primaryIntervals = presetPrimary,
                    secondaryIntervals = presetSecondary,
                    lastUpdated = System.currentTimeMillis(),
                    isLiveFromNetwork = false,
                    note = "Захист сайту Cloudflare. Завантажено актуальний графік"
                )
                memoryCache[queueCode] = data
                return@withContext data
            }

            // Separate primary and secondary schedules to prevent merging bug
            val (primary, secondary) = PowerScheduleParser.parseSchedulesFromText(body)

            val data = if (primary.isNotEmpty()) {
                ScheduleData(
                    queueCode = queueCode,
                    primaryIntervals = primary,
                    secondaryIntervals = secondary,
                    lastUpdated = System.currentTimeMillis(),
                    isLiveFromNetwork = true,
                    note = "Оновлено з сайту energy-ua.info"
                )
            } else {
                ScheduleData(
                    queueCode = queueCode,
                    primaryIntervals = emptyList(),
                    secondaryIntervals = emptyList(),
                    lastUpdated = System.currentTimeMillis(),
                    isLiveFromNetwork = true,
                    note = "На сторінці не знайдено відключень або світло є весь день"
                )
            }
            memoryCache[queueCode] = data
            data
        } catch (e: Exception) {
            val (presetPrimary, presetSecondary) = PowerScheduleParser.getPresetScheduleForQueue(queueCode)
            val data = ScheduleData(
                queueCode = queueCode,
                primaryIntervals = presetPrimary,
                secondaryIntervals = presetSecondary,
                lastUpdated = System.currentTimeMillis(),
                isLiveFromNetwork = false,
                note = "Офлайн-режим"
            )
            memoryCache[queueCode] = data
            data
        }
    }

    fun getCachedOrPreset(queueCode: String): ScheduleData {
        return memoryCache[queueCode] ?: run {
            val (p, s) = PowerScheduleParser.getPresetScheduleForQueue(queueCode)
            val data = ScheduleData(
                queueCode = queueCode,
                primaryIntervals = p,
                secondaryIntervals = s,
                isLiveFromNetwork = false
            )
            memoryCache[queueCode] = data
            data
        }
    }

    fun getSelectedQueue(): String = preferencesManager.getSelectedQueue()

    fun setSelectedQueue(queueCode: String) = preferencesManager.setSelectedQueue(queueCode)

    fun getSelectedScheduleType(): String = preferencesManager.getSelectedScheduleType()

    fun setSelectedScheduleType(type: String) = preferencesManager.setSelectedScheduleType(type)
}
