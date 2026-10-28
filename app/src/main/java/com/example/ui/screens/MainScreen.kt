package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ScheduleType
import com.example.ui.components.DayTimelineBar
import com.example.ui.components.OutageIntervalCard
import com.example.ui.components.QueueSelectorRow
import com.example.ui.components.StatusHeroCard
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DarkNavySurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.PowerGreen
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkNavyBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ElectricYellow.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ElectricBolt,
                                contentDescription = null,
                                tint = ElectricYellow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Світло",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.testTag("topbar_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Оновити графік",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkNavyBg,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Loading indicator
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = ElectricYellow,
                    trackColor = DarkNavySurface
                )
            }

            // Top bar showing all 12 queues (with RED indicators for those currently without power)
            QueueSelectorRow(
                allQueues = uiState.allQueues,
                selectedQueueCode = uiState.selectedQueueCode,
                onSelectQueue = { viewModel.selectQueue(it) }
            )

            // Main scrollable content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Live Hero Status Card (countdown timer, status badge, share button)
                item {
                    StatusHeroCard(
                        powerStatus = uiState.powerStatus,
                        queueCode = uiState.selectedQueueCode,
                        isLoading = uiState.isLoading,
                        onRefresh = { viewModel.refresh() }
                    )
                }

                // 2. 24-Hour Timeline Bar
                item {
                    DayTimelineBar(
                        intervals = uiState.activeIntervals,
                        currentMinuteOfDay = uiState.currentDayMinute
                    )
                }

                // 3. Schedule Type Switcher (Основний / Запасний графік)
                val scheduleData = uiState.scheduleData
                if (scheduleData != null && scheduleData.hasSecondary) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkNavySurface)
                                .border(1.dp, DarkNavyBorder, RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isPrimary = uiState.selectedScheduleType == ScheduleType.PRIMARY
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isPrimary) ElectricYellow else Color.Transparent)
                                    .clickable { viewModel.setScheduleType(ScheduleType.PRIMARY) }
                                    .padding(vertical = 10.dp)
                                    .testTag("primary_schedule_tab")
                            ) {
                                Text(
                                    text = "⚡ Основний графік",
                                    fontSize = 13.sp,
                                    fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isPrimary) Color(0xFF1E1E1E) else Color.White
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (!isPrimary) ElectricYellow else Color.Transparent)
                                    .clickable { viewModel.setScheduleType(ScheduleType.SECONDARY) }
                                    .padding(vertical = 10.dp)
                                    .testTag("secondary_schedule_tab")
                            ) {
                                Text(
                                    text = "🔄 Запасний графік",
                                    fontSize = 13.sp,
                                    fontWeight = if (!isPrimary) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isPrimary) Color(0xFF1E1E1E) else Color.White
                                )
                            }
                        }
                    }
                }

                // 4. Outage Intervals Header
                item {
                    val intervals = uiState.activeIntervals
                    val totalOutageMin = intervals.sumOf { it.durationMinutes }
                    val totalHours = totalOutageMin / 60
                    val totalMins = totalOutageMin % 60
                    val formattedTotal = when {
                        totalMins == 0 -> "$totalHours год"
                        totalHours == 0 -> "$totalMins хв"
                        else -> "$totalHours год $totalMins хв"
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.selectedScheduleType == ScheduleType.SECONDARY)
                                    "Запасний графік (Черга ${uiState.selectedQueueCode})"
                                else
                                    "Основний графік (Черга ${uiState.selectedQueueCode})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (intervals.isNotEmpty()) {
                                Text(
                                    text = "${intervals.size} відключення",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ElectricYellow
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (intervals.isNotEmpty())
                                "Всього без світла: $formattedTotal за добу"
                            else "Відключень не заплановано",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }

                // 5. Clean Interval Cards (Separated, no overlapping duplicates)
                val intervals = uiState.activeIntervals
                if (intervals.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkNavySurface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .border(1.dp, DarkNavyBorder, RoundedCornerShape(16.dp))
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = PowerGreen,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Світло є весь день!",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "На сьогодні для цієї черги відключень не заплановано",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(intervals) { index, interval ->
                        OutageIntervalCard(
                            interval = interval,
                            currentMinuteOfDay = uiState.currentDayMinute,
                            index = index
                        )
                    }
                }

                // 6. Source & Status Info
                item {
                    val schedule = uiState.scheduleData
                    val lastUpdatedFormatted = schedule?.lastUpdated?.let {
                        val sdf = SimpleDateFormat("HH:mm, d MMMM", Locale.getDefault())
                        sdf.format(Date(it))
                    } ?: "щойно"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkNavySurfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .border(0.8.dp, DarkNavyBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "energy-ua.info • Черга ${uiState.selectedQueueCode}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Статус: ${schedule?.note ?: "Готово"} (Оновлено: $lastUpdatedFormatted)",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
