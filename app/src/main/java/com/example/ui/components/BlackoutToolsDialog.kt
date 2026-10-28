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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.theme.DarkNavyBorder
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.DarkNavySurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.PowerGreen
import kotlin.math.roundToInt

@Composable
fun BlackoutToolsDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var powerBankMah by remember { mutableFloatStateOf(20000f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkNavySurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "Корисне під час блекаутів",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkNavySurfaceVariant,
                    contentColor = ElectricYellow
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Повербанк", fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Поради", fontSize = 13.sp) }
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (selectedTab == 0) {
                    // Powerbank calculator
                    val mah = powerBankMah.roundToInt()
                    // Real capacity approx 65-70% after voltage step up/losses (3.7V to 5V)
                    val effectiveMah = (mah * 0.7f).toInt()
                    val phoneCharges = String.format("%.1f", effectiveMah / 4200f)
                    val routerHours = String.format("%.1f", effectiveMah / 1200f)
                    val ledLampHours = String.format("%.0f", effectiveMah / 400f)

                    Text(
                        text = "Ємність повербанка: $mah mAh",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Slider(
                        value = powerBankMah,
                        onValueChange = { powerBankMah = it },
                        valueRange = 5000f..40000f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricYellow,
                            activeTrackColor = ElectricYellow
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(10000f, 20000f, 30000f).forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (mah == preset.toInt()) ElectricYellow.copy(alpha = 0.25f) else DarkNavySurfaceVariant)
                                    .border(1.dp, if (mah == preset.toInt()) ElectricYellow else DarkNavyBorder, RoundedCornerShape(8.dp))
                                    .clickable { powerBankMah = preset }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${preset.toInt() / 1000}k", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Calculation Results
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkNavySurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Smartphone, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Смартфони (~4200 mAh):", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.weight(1f))
                                Text("~$phoneCharges р.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PowerGreen)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Router, contentDescription = null, tint = ElectricYellow, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Wi-Fi роутер (через тригер):", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.weight(1f))
                                Text("~$routerHours год", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PowerGreen)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("USB LED лампочка:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.weight(1f))
                                Text("~$ledLampHours год", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PowerGreen)
                            }
                        }
                    }
                } else {
                    // Blackout Survival Tips
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TipItem(
                            title = "Захист техніки від стрибків",
                            desc = "Вимикайте чутливі прилади (холодильник, пралку, ПК) з розеток під час відключення. Вмикайте лише через 15-20 хв після повернення струму."
                        )
                        TipItem(
                            title = "Збереження холоду",
                            desc = "Не відчиняйте холодильник без нагальної потреби. Повний холодильник тримає холод до 6 годин, морозильна камера — до 36 годин."
                        )
                        TipItem(
                            title = "Запас води та зарядка",
                            desc = "Тримайте завжди зарядженими повербанки й телефон на рівні >80%. Зробіть запас технічної та питної води на 3 доби."
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricYellow, contentColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Зрозуміло", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun TipItem(title: String, desc: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavySurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircleOutline, contentDescription = null, tint = PowerGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f), lineHeight = 16.sp)
        }
    }
}
