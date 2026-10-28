package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PowerScheduleParser
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DarkNavySurfaceVariant
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.OutageRed
import com.example.ui.theme.PowerGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PasteScheduleDialog(
    currentQueueName: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }

    val parsedIntervals by remember {
        derivedStateOf {
            if (rawText.isBlank()) emptyList()
            else PowerScheduleParser.parseOutagesFromText(rawText)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkNavySurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Вставити графік із тексту",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Скопіюйте повідомлення з Telegram або ДТЕК та вставте сюди. Програма автоматично знайде часові проміжки (наприклад: 10:00 - 14:00, з 18:00 до 22:00):",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = {
                        Text(
                            "Вставте текст тут...",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("paste_text_field"),
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

                Spacer(modifier = Modifier.height(12.dp))

                // Live preview of parsed intervals
                if (parsedIntervals.isNotEmpty()) {
                    Text(
                        text = "Розпізнано ${parsedIntervals.size} інтервали(-ів):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PowerGreen
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        parsedIntervals.forEach { interval ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OutageRed.copy(alpha = 0.2f))
                                    .border(1.dp, OutageRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${interval.startStr} - ${interval.endStr} (${interval.durationFormatted})",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else if (rawText.isNotBlank()) {
                    Text(
                        text = "⚠️ Не знайдено часових проміжків у тексті",
                        fontSize = 12.sp,
                        color = ElectricYellow
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsedIntervals.isNotEmpty()) {
                        onApply(rawText)
                        onDismiss()
                    }
                },
                enabled = parsedIntervals.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricYellow,
                    contentColor = Color(0xFF1E1E1E)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("apply_parsed_schedule_button")
            ) {
                Text("Застосувати", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}
