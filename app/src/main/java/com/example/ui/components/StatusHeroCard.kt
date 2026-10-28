package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PowerStatus
import com.example.ui.theme.DarkNavySurfaceVariant
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.OutageRed
import com.example.ui.theme.OutageRedBg
import com.example.ui.theme.OutageRedGlow
import com.example.ui.theme.PowerGreen
import com.example.ui.theme.PowerGreenBg
import com.example.ui.theme.PowerGreenGlow

@Composable
fun StatusHeroCard(
    powerStatus: PowerStatus,
    queueCode: String,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val isOutage = powerStatus is PowerStatus.NoPower

    val cardBg = if (isOutage) {
        Brush.verticalGradient(listOf(OutageRedBg, Color(0xFF1F0909), Color(0xFF140707)))
    } else {
        Brush.verticalGradient(listOf(PowerGreenBg, Color(0xFF062B21), Color(0xFF091F18)))
    }

    val glowColor = if (isOutage) OutageRedGlow else PowerGreenGlow
    val mainColor = if (isOutage) OutageRed else PowerGreen

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("status_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(cardBg)
                .border(1.5.dp, mainColor.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top status indicator badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(mainColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOutage) "ЗАРАЗ ВІДКЛЮЧЕННЯ" else "СВІТЛО Є",
                            color = mainColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.8.sp
                        )
                    }

                    // Cherga badge
                    Text(
                        text = "Черга $queueCode",
                        color = ElectricYellow,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pulsing Icon Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(glowColor.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                        .border(2.dp, mainColor.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = when (powerStatus) {
                            is PowerStatus.NoPower -> Icons.Filled.PowerOff
                            is PowerStatus.HasPowerUpcomingOutage -> Icons.Filled.Lightbulb
                            is PowerStatus.AllOutagesFinished -> Icons.Filled.CheckCircle
                            is PowerStatus.NoOutagesToday -> Icons.Filled.ElectricBolt
                        },
                        contentDescription = "Статус живлення",
                        tint = mainColor,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main headline & countdown
                when (powerStatus) {
                    is PowerStatus.NoPower -> {
                        Text(
                            text = powerStatus.formattedCountdown,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Орієнтовно увімкнуть о ${powerStatus.currentInterval.endStr}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = OutageRedGlow
                        )
                        Text(
                            text = "(триває відключення ${powerStatus.currentInterval.startStr} - ${powerStatus.currentInterval.endStr})",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    is PowerStatus.HasPowerUpcomingOutage -> {
                        Text(
                            text = powerStatus.formattedCountdown,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "До відключення: ${powerStatus.humanReadableRemaining}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = PowerGreenGlow
                        )
                        Text(
                            text = "Початок о ${powerStatus.nextInterval.startStr} (до ${powerStatus.nextInterval.endStr})",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    is PowerStatus.AllOutagesFinished -> {
                        Text(
                            text = "Світло є до кінця доби",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "🎉 На сьогодні всі заплановані відключення вже закінчилися!",
                            fontSize = 14.sp,
                            color = PowerGreenGlow,
                            textAlign = TextAlign.Center
                        )
                    }
                    is PowerStatus.NoOutagesToday -> {
                        Text(
                            text = "Світло є весь день!",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Графіків відключень не зафіксовано або обмеження знято",
                            fontSize = 14.sp,
                            color = PowerGreenGlow,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val statusText = when (powerStatus) {
                                is PowerStatus.NoPower ->
                                    "⚡ У мене (Черга $queueCode) зараз НЕМАЄ світла. Орієнтовно увімкнуть о ${powerStatus.currentInterval.endStr} (залишилось ~${powerStatus.humanReadableRemaining})."
                                is PowerStatus.HasPowerUpcomingOutage ->
                                    "💡 У мене (Черга $queueCode) зараз СВІТЛО Є! Наступне відключення о ${powerStatus.nextInterval.startStr} (через ~${powerStatus.humanReadableRemaining})."
                                is PowerStatus.AllOutagesFinished ->
                                    "🎉 У мене (Черга $queueCode) всі відключення на сьогодні вже закінчилися! Світло є."
                                is PowerStatus.NoOutagesToday ->
                                    "☀️ У мене (Черга $queueCode) світло є весь день без відключень!"
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, statusText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Поділитися статусом"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_status_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Поділитися", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Поділитись", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onRefresh,
                        enabled = !isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("refresh_status_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = mainColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Оновити", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isLoading) "Завантаження..." else "Оновити", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
