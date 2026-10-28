package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.ElectricYellow

@Composable
fun QueueSelectorRow(
    savedQueues: List<SavedQueue>,
    selectedQueueId: String?,
    onSelectQueue: (String) -> Unit,
    onAddQueueClick: () -> Unit,
    onEditQueueClick: (SavedQueue) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        savedQueues.forEach { queue ->
            val isSelected = queue.id == selectedQueueId

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) ElectricYellow.copy(alpha = 0.2f) else DarkNavySurface
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) ElectricYellow else DarkNavyBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectQueue(queue.id) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .testTag("queue_chip_${queue.id}")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = queue.name,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) ElectricYellow else Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${queue.displayQueueCode})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) ElectricYellow.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Add New Queue button chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(DarkNavySurface)
                .border(1.dp, DarkNavyBorder, RoundedCornerShape(14.dp))
                .clickable { onAddQueueClick() }
                .padding(horizontal = 12.dp, vertical = 9.dp)
                .testTag("add_queue_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Додати чергу",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Додати",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}
