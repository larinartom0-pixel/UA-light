package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.model.QueueItem
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.OutageRed
import com.example.ui.theme.OutageRedBg
import com.example.ui.theme.PowerGreen

@Composable
fun QueueSelectorRow(
    allQueues: List<QueueItem>,
    selectedQueueCode: String,
    onSelectQueue: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            allQueues.forEach { queue ->
                val isSelected = queue.code == selectedQueueCode
                val isOffNow = queue.isPowerOffNow

                val chipBg = when {
                    isSelected && isOffNow -> OutageRedBg
                    isSelected -> ElectricYellow.copy(alpha = 0.2f)
                    isOffNow -> OutageRedBg.copy(alpha = 0.5f)
                    else -> DarkNavySurface
                }

                val borderColor = when {
                    isSelected && isOffNow -> OutageRed
                    isSelected -> ElectricYellow
                    isOffNow -> OutageRed.copy(alpha = 0.6f)
                    else -> DarkNavyBorder
                }

                val textColor = when {
                    isSelected && isOffNow -> Color.White
                    isSelected -> ElectricYellow
                    isOffNow -> Color(0xFFFFB4B4)
                    else -> Color.White
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(chipBg)
                        .border(
                            width = if (isSelected) 1.8.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectQueue(queue.code) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("queue_chip_${queue.code}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Status dot: RED if power is off right now, GREEN if power is on
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOffNow) OutageRed else PowerGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = queue.code,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
