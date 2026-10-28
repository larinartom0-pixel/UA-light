package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PowerScheduleRepository
import com.example.model.OutageInterval
import com.example.model.PowerStatus
import com.example.model.QueueItem
import com.example.model.ScheduleData
import com.example.model.ScheduleType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

data class MainUiState(
    val allQueues: List<QueueItem> = emptyList(),
    val selectedQueueCode: String = "4.2",
    val selectedScheduleType: ScheduleType = ScheduleType.PRIMARY,
    val scheduleData: ScheduleData? = null,
    val activeIntervals: List<OutageInterval> = emptyList(),
    val powerStatus: PowerStatus = PowerStatus.NoOutagesToday,
    val isLoading: Boolean = false,
    val currentDayMinute: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PowerScheduleRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(
        MainUiState(
            allQueues = QueueItem.ALL_QUEUE_CODES.map { QueueItem(it) },
            selectedQueueCode = repository.getSelectedQueue(),
            selectedScheduleType = try {
                ScheduleType.valueOf(repository.getSelectedScheduleType())
            } catch (e: Exception) {
                ScheduleType.PRIMARY
            }
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadCurrentQueue()
        startLiveTimer()
    }

    private fun loadCurrentQueue() {
        val currentCode = _uiState.value.selectedQueueCode
        fetchSchedule(currentCode)
    }

    fun selectQueue(queueCode: String) {
        if (_uiState.value.selectedQueueCode == queueCode) return
        repository.setSelectedQueue(queueCode)
        _uiState.value = _uiState.value.copy(selectedQueueCode = queueCode)
        fetchSchedule(queueCode)
    }

    fun setScheduleType(type: ScheduleType) {
        repository.setSelectedScheduleType(type.name)
        _uiState.value = _uiState.value.copy(selectedScheduleType = type)
        updateActiveIntervalsAndStatus()
    }

    fun refresh() {
        fetchSchedule(_uiState.value.selectedQueueCode)
    }

    private fun fetchSchedule(queueCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val data = repository.getSchedule(queueCode)
                _uiState.value = _uiState.value.copy(
                    scheduleData = data,
                    isLoading = false
                )
                updateActiveIntervalsAndStatus()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun startLiveTimer() {
        viewModelScope.launch {
            while (isActive) {
                updateActiveIntervalsAndStatus()
                delay(1000)
            }
        }
    }

    private fun updateActiveIntervalsAndStatus() {
        val cal = Calendar.getInstance()
        val nowHours = cal.get(Calendar.HOUR_OF_DAY)
        val nowMins = cal.get(Calendar.MINUTE)
        val nowSecs = cal.get(Calendar.SECOND)
        val currentMinuteOfDay = nowHours * 60 + nowMins

        val data = _uiState.value.scheduleData
        val activeType = _uiState.value.selectedScheduleType
        val intervals = data?.getIntervals(activeType) ?: emptyList()

        // Recalculate status of ALL 12 queues so chips can show RED if power is off right now
        val updatedQueues = QueueItem.ALL_QUEUE_CODES.map { code ->
            val queueSchedule = if (code == data?.queueCode) {
                data.primaryIntervals
            } else {
                repository.getCachedOrPreset(code).primaryIntervals
            }
            val isOffNow = queueSchedule.any { it.isActive(currentMinuteOfDay) }
            QueueItem(code = code, isPowerOffNow = isOffNow)
        }

        val status = calculatePowerStatus(intervals, currentMinuteOfDay, nowSecs)

        _uiState.value = _uiState.value.copy(
            allQueues = updatedQueues,
            activeIntervals = intervals,
            powerStatus = status,
            currentDayMinute = currentMinuteOfDay
        )
    }

    private fun calculatePowerStatus(
        intervals: List<OutageInterval>,
        currentMinuteOfDay: Int,
        nowSecs: Int
    ): PowerStatus {
        if (intervals.isEmpty()) {
            return PowerStatus.NoOutagesToday
        }

        // 1. Is power currently OFF?
        for (interval in intervals) {
            if (interval.startMin <= currentMinuteOfDay && currentMinuteOfDay < interval.endMin) {
                val totalRemainingSeconds = ((interval.endMin - currentMinuteOfDay) * 60L) - nowSecs
                return PowerStatus.NoPower(
                    currentInterval = interval,
                    secondsUntilOn = totalRemainingSeconds.coerceAtLeast(0L)
                )
            }
        }

        // 2. Is power currently ON and outage is upcoming?
        for (interval in intervals) {
            if (currentMinuteOfDay < interval.startMin) {
                val totalRemainingSeconds = ((interval.startMin - currentMinuteOfDay) * 60L) - nowSecs
                return PowerStatus.HasPowerUpcomingOutage(
                    nextInterval = interval,
                    secondsUntilOff = totalRemainingSeconds.coerceAtLeast(0L)
                )
            }
        }

        // 3. All planned outages ended
        return PowerStatus.AllOutagesFinished
    }
}
