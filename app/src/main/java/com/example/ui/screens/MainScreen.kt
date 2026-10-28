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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.model.SavedQueue
import com.example.ui.components.AddQueueDialog
import com.example.ui.components.BlackoutToolsDialog
import com.example.ui.components.DayTimelineBar
import com.example.ui.components.OutageIntervalCard
import com.example.ui.components.PasteScheduleDialog
import com.example.ui.components.QueueSelectorRow
import com.example.ui.components.ScreenFlashlightDialog
import com.example.ui.components.StatusHeroCard
import com.example.ui.theme.DarkNavyBg
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DarkNavySurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.OutageRed
import com.example.ui.theme.PowerGreen
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()

    var showAddQueueDialog by remember { mutableStateOf(false) }
    var editingQueue by remember { mutableStateOf<SavedQueue?>(null) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var showFlashlightDialog by remember { mutableStateOf(false) }
    var showToolsDialog by remember { mutableStateOf(false) }

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
                    // Screen Lantern
                    IconButton(
                        onClick = { showFlashlightDialog = true },
                        modifier = Modifier.testTag("flashlight_topbar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FlashlightOn,
                            contentDescription = "Екран-ліхтарик",
                            tint = ElectricYellow
                        )
                    }

                    // Paste text from Telegram
                    IconButton(
                        onClick = { showPasteDialog = true },
                        modifier = Modifier.testTag("paste_topbar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentPaste,
                            contentDescription = "Вставити текст графіка",
                            tint = Color.White
                        )
                    }

                    // Tools & Powerbank Calculator
                    IconButton(
                        onClick = { showToolsDialog = true },
                        modifier = Modifier.testTag("tools_topbar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BatteryChargingFull,
                            contentDescription = "Калькулятор та поради",
                            tint = PowerGreen
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
            // Loading progress bar
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = ElectricYellow,
                    trackColor = DarkNavySurface
                )
            }

            // Saved Queues / Locations horizontal bar
            QueueSelectorRow(
                savedQueues = uiState.savedQueues,
                selectedQueueId = uiState.selectedQueue?.id,
                onSelectQueue = { viewModel.selectQueue(it) },
                onAddQueueClick = { showAddQueueDialog = true },
                onEditQueueClick = { editingQueue = it }
            )

            // Main scrollable content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hero Status Card with live countdown & pulsing ring
                item {
                    StatusHeroCard(
                        powerStatus = uiState.powerStatus,
                        selectedQueue = uiState.selectedQueue,
                        isLoading = uiState.isLoading,
                        onRefresh = { viewModel.refresh() }
                    )
                }

                // 2. 24-Hour Timeline Bar
                item {
                    DayTimelineBar(
                        intervals = uiState.scheduleData?.intervals ?: emptyList(),
                        currentMinuteOfDay = uiState.currentDayMinute
                    )
                }

                // 3. Schedule Header & Details
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Графік на сьогодні",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            val intervals = uiState.scheduleData?.intervals ?: emptyList()
                            val totalOutageMin = intervals.sumOf { it.durationMinutes }
                            val totalHours = totalOutageMin / 60
                            val totalMins = totalOutageMin % 60
                            val formattedTotal = when {
                                totalMins == 0 -> "$totalHours год"
                                totalHours == 0 -> "$totalMins хв"
                                else -> "$totalHours год $totalMins хв"
                            }

                            Text(
                                text = if (intervals.isNotEmpty())
                                    "${intervals.size} відключення (всього без світла: $formattedTotal)"
                                else "Відключень не заплановано",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }

                        // Edit / Reset buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { editingQueue = uiState.selectedQueue },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Редагувати чергу",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.resetToDefaultSchedule()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Графік скинуто до джерела")
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RestartAlt,
                                    contentDescription = "Скинути до стандартного",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Outage interval list
                val intervals = uiState.scheduleData?.intervals ?: emptyList()
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
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Світло є весь день!",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "На сторінці не знайдено годин відключень на сьогодні або обмеження не діють. Можна заряджати павербанки!",
                                    fontSize = 13.sp,
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

                // 5. Source & status footer
                item {
                    val schedule = uiState.scheduleData
                    val lastUpdatedFormatted = schedule?.lastUpdated?.let {
                        val sdf = SimpleDateFormat("HH:mm:ss, d MMMM", Locale("uk"))
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
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Інформація про дані",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Статус: ${schedule?.note ?: "Готово"}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Text(
                                text = "Останнє оновлення: $lastUpdatedFormatted",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Dialogs
    if (showAddQueueDialog) {
        AddQueueDialog(
            initialQueue = null,
            onDismiss = { showAddQueueDialog = false },
            onSave = { name, code, url ->
                viewModel.addQueue(name, code, url)
                scope.launch {
                    snackbarHostState.showSnackbar("Додано чергу $code ($name)")
                }
            }
        )
    }

    editingQueue?.let { queueToEdit ->
        AddQueueDialog(
            initialQueue = queueToEdit,
            onDismiss = { editingQueue = null },
            onSave = { name, code, url ->
                viewModel.updateQueue(
                    queueToEdit.copy(
                        name = name,
                        queueCode = code,
                        isCustomUrl = url.isNotBlank(),
                        customUrl = url
                    )
                )
                editingQueue = null
                scope.launch {
                    snackbarHostState.showSnackbar("Оновлено чергу $code")
                }
            },
            onDelete = { idToDelete ->
                viewModel.deleteQueue(idToDelete)
                editingQueue = null
                scope.launch {
                    snackbarHostState.showSnackbar("Чергу видалено")
                }
            }
        )
    }

    if (showPasteDialog) {
        PasteScheduleDialog(
            currentQueueName = uiState.selectedQueue?.name ?: "Поточна черга",
            onDismiss = { showPasteDialog = false },
            onApply = { rawText ->
                val success = viewModel.importPastedSchedule(rawText)
                if (success) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Графік успішно імпортовано!")
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Не вдалося розпізнати інтервали")
                    }
                }
            }
        )
    }

    if (showFlashlightDialog) {
        ScreenFlashlightDialog(
            onDismiss = { showFlashlightDialog = false }
        )
    }

    if (showToolsDialog) {
        BlackoutToolsDialog(
            onDismiss = { showToolsDialog = false }
        )
    }
}
