package com.datecalc.billing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaywallScreen(
    trialDaysRemaining: Int,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit,
    onNotNow: () -> Unit
) {
    val cardBg = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val featureColor = MaterialTheme.colorScheme.onSurfaceVariant
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Дата-калькулятор Премиум", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(12.dp))

                if (trialDaysRemaining > 0) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF007AFF).copy(alpha = 0.12f)) {
                        Text("Пробный период: $trialDaysRemaining дн. осталось", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF007AFF), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                } else {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f)) {
                        Text("Пробный период завершён", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                listOf("Расчёт дней, недель и месяцев", "Учёт начальной и конечной даты",
                    "Неограниченные вычисления", "Без рекламы").forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("\u2713", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF), modifier = Modifier.width(28.dp))
                        Text(feature, fontSize = 15.sp, color = featureColor)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = onSubscribe, shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Подписаться", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("149 \u20BD/мес \u00B7 7 дней бесплатно", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onRestore) { Text("Восстановить покупку", fontSize = 14.sp, color = Color(0xFF007AFF)) }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onNotNow) { Text("Не сейчас", fontSize = 14.sp, color = mutedColor) }
                Spacer(modifier = Modifier.height(12.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Оплата списывается через магазин приложений (RuStore/Google Play).",
                        fontSize = 11.sp, color = mutedColor, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Подписка продлевается автоматически. Отмена в любой момент в настройках подписки магазина.",
                        fontSize = 11.sp, color = mutedColor, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Бесплатный пробный период: 7 дней. По окончании — 149 \u20BD/мес.",
                        fontSize = 11.sp, color = mutedColor, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
