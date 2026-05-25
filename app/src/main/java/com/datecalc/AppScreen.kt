package com.datecalc

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.datecalc.logic.DateCalculator
import com.datecalc.logic.DateCalcResult
import com.datecalc.ui.components.DateCard
import com.datecalc.ui.components.ResultBox
import com.datecalc.ui.theme.DateCalcTheme
import java.util.Calendar

private const val APP_VERSION = "1.1"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen() {
    var darkTheme by remember { mutableStateOf(false) }
    DateCalcTheme(darkTheme = darkTheme) {
        val context = LocalContext.current
        var showAbout by remember { mutableStateOf(false) }

        val cal = Calendar.getInstance()
        val todayDay = cal.get(Calendar.DAY_OF_MONTH)
        val todayMonth = cal.get(Calendar.MONTH)
        val todayYear = cal.get(Calendar.YEAR)

        var startDay by remember { mutableIntStateOf(todayDay) }
        var startMonth by remember { mutableIntStateOf(todayMonth) }
        var startYear by remember { mutableIntStateOf(todayYear) }

        var endDay by remember { mutableIntStateOf(todayDay) }
        var endMonth by remember { mutableIntStateOf(todayMonth) }
        var endYear by remember { mutableIntStateOf(todayYear) }

        var includeStart by remember { mutableStateOf(true) }
        var includeEnd by remember { mutableStateOf(true) }

        var result by remember { mutableStateOf(DateCalcResult()) }
        var error by remember { mutableStateOf("") }

        val safeStartDay = startDay.coerceAtMost(DateCalculator.daysInMonth(startYear, startMonth))
        val safeEndDay = endDay.coerceAtMost(DateCalculator.daysInMonth(endYear, endMonth))

        val startDays = remember(startYear, startMonth) { DateCalculator.validDaysForMonth(startYear, startMonth).map { it.toString() } }
        val endDays = remember(endYear, endMonth) { DateCalculator.validDaysForMonth(endYear, endMonth).map { it.toString() } }

        LaunchedEffect(safeStartDay, startMonth, startYear, safeEndDay, endMonth, endYear, includeStart, includeEnd) {
            if (startDay != safeStartDay) startDay = safeStartDay
            if (endDay != safeEndDay) endDay = safeEndDay
            result = DateCalculator.calculate(safeStartDay, startMonth, startYear, safeEndDay, endMonth, endYear, includeStart, includeEnd)
            error = result.error
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Дата-калькулятор", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
                    },
                    actions = {
                        IconButton(onClick = { darkTheme = !darkTheme }) {
                            Text(if (darkTheme) "☀️" else "🌙", fontSize = 16.sp)
                        }
                        IconButton(onClick = {
                            startDay = todayDay; startMonth = todayMonth; startYear = todayYear
                            endDay = todayDay; endMonth = todayMonth; endYear = todayYear
                            includeStart = true; includeEnd = true
                        }) { Text("↺", fontSize = 18.sp) }
                        IconButton(onClick = { showAbout = true }) {
                            Icon(Icons.Filled.Info, "О приложении", tint = Color(0xFF007AFF))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DateCard("Дата начала", "calendar", Color(0xFF007AFF), safeStartDay, startMonth, startYear, startDays, "Учитывать начальную дату", includeStart, { d, m, y -> startDay = d; startMonth = m; startYear = y }, { includeStart = it })
                DateCard("Дата окончания", "clock", Color(0xFFFF9500), safeEndDay, endMonth, endYear, endDays, "Учитывать конечную дату", includeEnd, { d, m, y -> endDay = d; endMonth = m; endYear = y }, { includeEnd = it })

                if (error.isNotEmpty()) {
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f), modifier = Modifier.fillMaxWidth()) {
                        Text(error, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
                    }
                }

                ResultBox(result, DateCalculator.resultDescription(includeStart, includeEnd))
            }
        }

        if (showAbout) {
            AlertDialog(
                onDismissRequest = { showAbout = false },
                title = { Text("Дата-калькулятор") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Версия $APP_VERSION", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Divider()
                        Text("Политика конфиденциальности", color = Color(0xFF007AFF), fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://namotoff.github.io/datecalc-privacy/"))) })
                        Text("Написать разработчику", color = Color(0xFF007AFF), fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:edazin@bk.ru")).apply { putExtra(Intent.EXTRA_SUBJECT, "Дата-калькулятор") }) })
                    }
                },
                confirmButton = { TextButton(onClick = { showAbout = false }) { Text("OK") } }
            )
        }
    }
}
