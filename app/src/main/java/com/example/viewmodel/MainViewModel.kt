package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PowerScheduleParser
import com.example.data.PowerScheduleRepository
import com.example.model.OutageInterval
import com.example.model.PowerStatus
import com.example.model.SavedQueue
import com.example.model.ScheduleData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class MainUiState(
    val savedQueues: List<SavedQueue> = emptyList(),
    val selectedQueue: SavedQueue? = null,
    val scheduleData: ScheduleData? = null,
    val powerStatus: PowerStatus = PowerStatus.NoOutagesToday,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentDayMinute: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PowerScheduleRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadQueues()
        startLiveTimer()
    }

    private fun loadQueues() {
        val queues = repository.getSavedQueues()
        val selectedId = repository.getSelectedQueueId()
        val current = queues.find { it.id == selectedId } ?: queues.firstOrNull()

        _uiState.value = _uiState.value.copy(
            savedQueues = queues,
            selectedQueue = current
        )

        if (current != null) {
            fetchSchedule(current)
        }
    }

    fun selectQueue(queueId: String) {
        val queue = _uiState.value.savedQueues.find { it.id == queueId } ?: return
        repository.setSelectedQueueId(queueId)
        _uiState.value = _uiState.value.copy(selectedQueue = queue)
        fetchSchedule(queue)
    }

    fun refresh() {
        val current = _uiState.value.selectedQueue ?: return
        fetchSchedule(current)
    }

    private fun fetchSchedule(queue: SavedQueue) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val data = repository.getSchedule(queue)
                _uiState.value = _uiState.value.copy(
                    scheduleData = data,
                    isLoading = false
                )
                recalculateStatus()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Помилка оновлення"
                )
            }
        }
    }

    fun addQueue(name: String, queueCode: String, customUrl: String = "") {
        val isCustom = customUrl.isNotBlank()
        val newQueue = SavedQueue(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Черга $queueCode" },
            queueCode = queueCode.trim(),
            isCustomUrl = isCustom,
            customUrl = customUrl.trim()
        )
        val updated = _uiState.value.savedQueues + newQueue
        repository.saveQueues(updated)
        _uiState.value = _uiState.value.copy(savedQueues = updated)
        selectQueue(newQueue.id)
    }

    fun updateQueue(queue: SavedQueue) {
        val updated = _uiState.value.savedQueues.map { if (it.id == queue.id) queue else it }
        repository.saveQueues(updated)
        _uiState.value = _uiState.value.copy(
            savedQueues = updated,
            selectedQueue = if (_uiState.value.selectedQueue?.id == queue.id) queue else _uiState.value.selectedQueue
        )
        if (_uiState.value.selectedQueue?.id == queue.id) {
            fetchSchedule(queue)
        }
    }

    fun deleteQueue(queueId: String) {
        val currentList = _uiState.value.savedQueues
        if (currentList.size <= 1) return // Keep at least one
        val updated = currentList.filter { it.id != queueId }
        repository.saveQueues(updated)

        val nextSelected = if (_uiState.value.selectedQueue?.id == queueId) {
            updated.first()
        } else {
            _uiState.value.selectedQueue ?: updated.first()
        }

        _uiState.value = _uiState.value.copy(
            savedQueues = updated,
            selectedQueue = nextSelected
        )
        selectQueue(nextSelected.id)
    }

    fun importPastedSchedule(rawText: String): Boolean {
        val current = _uiState.value.selectedQueue ?: return false
        val parsed = PowerScheduleParser.parseOutagesFromText(rawText)
        if (parsed.isEmpty()) return false

        repository.saveCustomSchedule(current.queueCode, parsed)
        _uiState.value = _uiState.value.copy(
            scheduleData = ScheduleData(
                queueCode = current.queueCode,
                intervals = parsed,
                lastUpdated = System.currentTimeMillis(),
                isLiveFromNetwork = false,
                note = "Імпортовано з тексту (${parsed.size} інтервалів)"
            )
        )
        recalculateStatus()
        return true
    }

    fun resetToDefaultSchedule() {
        val current = _uiState.value.selectedQueue ?: return
        repository.clearCustomSchedule(current.queueCode)
        fetchSchedule(current)
    }

    private fun startLiveTimer() {
        viewModelScope.launch {
            while (isActive) {
                recalculateStatus()
                delay(1000)
            }
        }
    }

    private fun recalculateStatus() {
        val intervals = _uiState.value.scheduleData?.intervals ?: emptyList()
        val cal = Calendar.getInstance()
        val nowHours = cal.get(Calendar.HOUR_OF_DAY)
        val nowMins = cal.get(Calendar.MINUTE)
        val nowSecs = cal.get(Calendar.SECOND)

        val currentMinuteOfDay = nowHours * 60 + nowMins
        _uiState.value = _uiState.value.copy(currentDayMinute = currentMinuteOfDay)

        if (intervals.isEmpty()) {
            _uiState.value = _uiState.value.copy(powerStatus = PowerStatus.NoOutagesToday)
            return
        }

        // 1. Check if power is currently OFF
        for (interval in intervals) {
            if (interval.startMin <= currentMinuteOfDay && currentMinuteOfDay < interval.endMin) {
                val totalRemainingSeconds = ((interval.endMin - currentMinuteOfDay) * 60L) - nowSecs
                _uiState.value = _uiState.value.copy(
                    powerStatus = PowerStatus.NoPower(
                        currentInterval = interval,
                        secondsUntilOn = totalRemainingSeconds.coerceAtLeast(0L)
                    )
                )
                return
            }
        }

        // 2. Check if power is ON and there is an upcoming outage
        for (interval in intervals) {
            if (currentMinuteOfDay < interval.startMin) {
                val totalRemainingSeconds = ((interval.startMin - currentMinuteOfDay) * 60L) - nowSecs
                _uiState.value = _uiState.value.copy(
                    powerStatus = PowerStatus.HasPowerUpcomingOutage(
                        nextInterval = interval,
                        secondsUntilOff = totalRemainingSeconds.coerceAtLeast(0L)
                    )
                )
                return
            }
        }

        // 3. All planned outages ended
        _uiState.value = _uiState.value.copy(powerStatus = PowerStatus.AllOutagesFinished)
    }
}
