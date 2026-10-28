package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SavedQueue
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DarkNavySurfaceVariant
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.OutageRed

@Composable
fun AddQueueDialog(
    initialQueue: SavedQueue? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, queueCode: String, customUrl: String) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val standardQueues = listOf(
        "1.1", "1.2", "2.1", "2.2",
        "3.1", "3.2", "4.1", "4.2",
        "5.1", "5.2", "6.1", "6.2"
    )
    val namePresets = listOf("🏠 Дім", "💼 Робота", "👵 Батьки", "🏖️ Дача")

    var name by remember { mutableStateOf(initialQueue?.name ?: "🏠 Дім") }
    var selectedQueueCode by remember { mutableStateOf(initialQueue?.queueCode ?: "4.2") }
    var customUrl by remember { mutableStateOf(initialQueue?.customUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkNavySurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (initialQueue == null) "Додати адресу / чергу" else "Редагувати чергу",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Name Input
                Text("Назва локації:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("queue_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkNavySurfaceVariant,
                        unfocusedContainerColor = DarkNavySurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = ElectricYellow,
                        unfocusedIndicatorColor = DarkNavyBorder
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Name presets row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    namePresets.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkNavySurfaceVariant)
                                .border(0.8.dp, DarkNavyBorder, RoundedCornerShape(8.dp))
                                .clickable { name = preset }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(preset, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Standard queue selector grid
                Text("Оберіть чергу відключення:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(130.dp)
                ) {
                    items(standardQueues) { q ->
                        val isSelected = selectedQueueCode == q || selectedQueueCode == q.replace(".", "-")
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ElectricYellow else DarkNavySurfaceVariant)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) ElectricYellow else DarkNavyBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedQueueCode = q }
                                .padding(vertical = 8.dp)
                                .testTag("cherga_option_$q")
                        ) {
                            Text(
                                text = q,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isSelected) Color(0xFF1E1E1E) else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Джерело: energy-ua.info/cherga/${selectedQueueCode.replace(".", "-")}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, selectedQueueCode, customUrl)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricYellow,
                    contentColor = Color(0xFF1E1E1E)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_queue_button")
            ) {
                Text("Зберегти", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (initialQueue != null && onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete(initialQueue.id)
                            onDismiss()
                        }
                    ) {
                        Text("Видалити", color = OutageRed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Скасувати", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    )
}
