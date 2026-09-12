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
                Text("Дата-калькулятор Unlimited", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(20.dp))

                listOf("Виджет для домашнего экрана").forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("\u2713", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF), modifier = Modifier.width(28.dp))
                        Text(feature, fontSize = 15.sp, color = featureColor)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = onSubscribe, shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Подписаться — 99 \u20BD/мес", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onRestore) { Text("Восстановить покупку", fontSize = 14.sp, color = Color(0xFF007AFF)) }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onNotNow) { Text("Не сейчас", fontSize = 14.sp, color = mutedColor) }
                Spacer(modifier = Modifier.height(12.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Оплата списывается через RuStore.",
                        fontSize = 11.sp, color = mutedColor, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Подписка продлевается автоматически. Отмена в любой момент.",
                        fontSize = 11.sp, color = mutedColor, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
