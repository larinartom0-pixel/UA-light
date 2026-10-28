package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OutageInterval
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.OutageRed
import com.example.ui.theme.PowerGreen

@Composable
fun DayTimelineBar(
    intervals: List<OutageInterval>,
    currentMinuteOfDay: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("day_timeline_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, DarkNavyBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Шкала доби (24 години)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Current time tag
                val curH = currentMinuteOfDay / 60
                val curM = currentMinuteOfDay % 60
                val formattedTime = String.format("%02d:%02d", curH, curM)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElectricBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Зараз $formattedTime",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 24 Hour Bar Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val totalMinutes = 1440f

                    // 1. Base Green fill (Power ON by default)
                    drawRoundRect(
                        color = PowerGreen,
                        size = Size(w, h),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    // 2. Draw Red segments for planned outages
                    for (interval in intervals) {
                        val startFraction = interval.startMin.toFloat() / totalMinutes
                        val endFraction = interval.endMin.toFloat() / totalMinutes
                        val startX = startFraction * w
                        val blockW = ((endFraction - startFraction) * w).coerceAtLeast(2f)

                        drawRect(
                            color = OutageRed,
                            topLeft = Offset(startX, 0f),
                            size = Size(blockW, h)
                        )
                    }

                    // 3. Hourly grid tick lines (every 2 hours)
                    for (hour in 1..23) {
                        val x = (hour * 60f / totalMinutes) * w
                        val isMajor = hour % 4 == 0
                        drawLine(
                            color = Color.Black.copy(alpha = if (isMajor) 0.5f else 0.25f),
                            start = Offset(x, if (isMajor) 0f else h * 0.4f),
                            end = Offset(x, h),
                            strokeWidth = if (isMajor) 2f else 1f
                        )
                    }

                    // 4. Current Time Needle / Marker
                    val currentFraction = (currentMinuteOfDay.toFloat() / totalMinutes).coerceIn(0f, 1f)
                    val needleX = currentFraction * w

                    // Vertical line
                    drawLine(
                        color = ElectricBlue,
                        start = Offset(needleX, 0f),
                        end = Offset(needleX, h),
                        strokeWidth = 3.dp.toPx()
                    )

                    // Top indicator triangle
                    val path = Path().apply {
                        moveTo(needleX - 5.dp.toPx(), 0f)
                        lineTo(needleX + 5.dp.toPx(), 0f)
                        lineTo(needleX, 7.dp.toPx())
                        close()
                    }
                    drawPath(path = path, color = ElectricBlue)

                    // Bottom indicator triangle
                    val bottomPath = Path().apply {
                        moveTo(needleX - 5.dp.toPx(), h)
                        lineTo(needleX + 5.dp.toPx(), h)
                        lineTo(needleX, h - 7.dp.toPx())
                        close()
                    }
                    drawPath(path = bottomPath, color = ElectricBlue)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hour labels (00, 04, 08, 12, 16, 20, 24)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "24:00").forEach { label ->
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(PowerGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Світло є", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(OutageRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Відключення", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(ElectricBlue)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Поточний час", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}
