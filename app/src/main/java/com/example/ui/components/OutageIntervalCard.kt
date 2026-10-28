package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.model.OutageInterval
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DarkNavySurfaceVariant
import com.example.ui.theme.OutageRed
import com.example.ui.theme.OutageRedBg
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg

@Composable
fun OutageIntervalCard(
    interval: OutageInterval,
    currentMinuteOfDay: Int,
    index: Int,
    modifier: Modifier = Modifier
) {
    val isActive = interval.isActive(currentMinuteOfDay)
    val isPassed = interval.isPassed(currentMinuteOfDay)
    val isUpcoming = interval.isUpcoming(currentMinuteOfDay)

    val cardBg = when {
        isActive -> OutageRedBg.copy(alpha = 0.6f)
        isPassed -> DarkNavySurface.copy(alpha = 0.5f)
        else -> DarkNavySurface
    }

    val borderColor = when {
        isActive -> OutageRed
        isPassed -> DarkNavyBorder.copy(alpha = 0.5f)
        else -> DarkNavyBorder
    }

    val accentColor = when {
        isActive -> OutageRed
        isPassed -> Color.Gray
        else -> WarningAmber
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("outage_interval_card_$index"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Outage number / Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = when {
                            isActive -> Icons.Filled.PowerOff
                            isPassed -> Icons.Filled.Check
                            else -> Icons.Filled.HourglassTop
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${interval.startStr}  —  ${interval.endStr}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPassed) Color.White.copy(alpha = 0.5f) else Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Тривалість: ${interval.durationFormatted}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Status Badge
            val (statusText, badgeBg, badgeColor) = when {
                isActive -> Triple("Зараз триває", OutageRed.copy(alpha = 0.2f), OutageRed)
                isPassed -> Triple("Минуло", Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.5f))
                else -> Triple("Заплановано", WarningAmberBg.copy(alpha = 0.6f), WarningAmber)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .border(0.8.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = statusText,
                    color = badgeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
