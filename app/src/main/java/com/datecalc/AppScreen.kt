package com.datecalc

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datecalc.billing.BillingFactory
import com.datecalc.billing.PaywallScreen
import com.datecalc.billing.SubscriptionManager
import com.datecalc.logic.DateCalculator
import com.datecalc.logic.DateCalcResult
import com.datecalc.ui.components.DateCard
import com.datecalc.ui.components.ResultBox
import com.datecalc.ui.theme.DateCalcTheme
import com.datecalc.widget.BatteryOptimizationPrompt
import com.datecalc.widget.DaysWidgetReceiver
import com.datecalc.ui.components.WheelColumnPicker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen() {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    val billingClient = remember { BillingFactory.create(context) }
    val prefs = remember { context.getSharedPreferences("datecalc_prefs", Context.MODE_PRIVATE) }

    var darkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme", false)) }
    var hasAccess by remember { mutableStateOf(true) }
    var isSubscribed by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        billingClient.initialize()
        isSubscribed = SubscriptionManager.isSubscribed(context).first()
        if (!isSubscribed) {
            val storePurchased = billingClient.checkPurchases()
            if (storePurchased) {
                SubscriptionManager.setSubscribed(context, true)
                isSubscribed = true
                hasAccess = true
            } else {
                hasAccess = SubscriptionManager.hasAccess(context).first()
            }
        } else {
            hasAccess = true
        }
    }

    DateCalcTheme(darkTheme = darkTheme) {
        if (!showPaywall) {
            BatteryOptimizationPrompt()
        }
        if (showPaywall && !hasAccess) {
            PaywallScreen(
                onSubscribe = {
                    scope.launch {
                        try {
                            val success = billingClient.purchase(activity)
                            if (success) {
                                SubscriptionManager.setSubscribed(context, true)
                                isSubscribed = true; hasAccess = true; showPaywall = false
                            }
                        } catch (_: Exception) {}
                    }
                },
                onRestore = {
                    scope.launch {
                        try {
                            if (billingClient.checkPurchases()) {
                                SubscriptionManager.setSubscribed(context, true)
                                isSubscribed = true; hasAccess = true; showPaywall = false
                            }
                        } catch (_: Exception) {}
                    }
                },
                onNotNow = { showPaywall = false }
            )
        } else {
            MainContent(
                darkTheme = darkTheme,
                        onToggleTheme = {
                            darkTheme = !darkTheme
                            prefs.edit().putBoolean("dark_theme", darkTheme).apply()
                        },
                isSubscribed = isSubscribed,
                onShowPaywall = { showPaywall = true },
                onShowMenu = { showMenu = true }
            )
        }

        if (showMenu) {
            ModalBottomSheet(
                onDismissRequest = { showMenu = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Меню", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (!isSubscribed) {
                        MenuItemRow("💎", "Unlimited") {
                            showMenu = false
                            showPaywall = true
                        }
                    }
                    MenuItemRow("📅", "Настроить виджет") {
                        showMenu = false
                        val intent = android.content.Intent(context, com.datecalc.WidgetConfigureActivity::class.java)
                        context.startActivity(intent)
                    }
                    MenuItemRow("ℹ️", "О приложении") {
                        showMenu = false
                        showAbout = true
                    }
                    MenuItemRow("🔒", "Политика конфиденциальности") {
                        showMenu = false
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://namotoff.github.io/datecalc-privacy/")))
                    }
                    MenuItemRow("✉️", "Написать разработчику") {
                        showMenu = false
                        context.startActivity(Intent(Intent.ACTION_SENDTO,
                            Uri.parse("mailto:edazin@bk.ru")).apply {
                            putExtra(Intent.EXTRA_SUBJECT, "Дата-калькулятор")
                        })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (showAbout) {
            AlertDialog(
                onDismissRequest = { showAbout = false },
                title = { Text("Дата-калькулятор", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Версия 1.2", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Text("Калькулятор дней между датами с виджетом на рабочий стол.", fontSize = 14.sp)
                        Text("Разработчик: Tlt Bios", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAbout = false }) { Text("OK") }
                }
            )
        }
    }
}

@Composable
private fun MenuItemRow(emoji: String, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 18.sp, modifier = Modifier.width(28.dp))
        Text(title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f))
        Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    isSubscribed: Boolean,
    onShowPaywall: () -> Unit,
    onShowMenu: () -> Unit
) {
    val cal = Calendar.getInstance()
    val context = LocalContext.current
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

    val startMaxDay = remember(startYear, startMonth) { DateCalculator.daysInMonth(startYear, startMonth) }
    val endMaxDay = remember(endYear, endMonth) { DateCalculator.daysInMonth(endYear, endMonth) }

    val safeStartDay = startDay.coerceAtMost(startMaxDay)
    val safeEndDay = endDay.coerceAtMost(endMaxDay)

    val startDays = remember(startYear, startMonth) {
        DateCalculator.validDaysForMonth(startYear, startMonth).map { it.toString() }
    }
    val endDays = remember(endYear, endMonth) {
        DateCalculator.validDaysForMonth(endYear, endMonth).map { it.toString() }
    }

    LaunchedEffect(safeStartDay, startMonth, startYear, safeEndDay, endMonth, endYear, includeStart, includeEnd) {
        if (startDay != safeStartDay) startDay = safeStartDay
        if (endDay != safeEndDay) endDay = safeEndDay
        result = DateCalculator.calculate(safeStartDay, startMonth, startYear, safeEndDay, endMonth, endYear, includeStart, includeEnd)
        error = result.error
    }

    fun resetFields() {
        startDay = todayDay; startMonth = todayMonth; startYear = todayYear
        endDay = todayDay; endMonth = todayMonth; endYear = todayYear
        includeStart = true; includeEnd = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Дата-калькулятор", fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground)
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (darkTheme) "Светлая тема" else "Тёмная тема",
                            tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { resetFields() }) {
                        Icon(Icons.Filled.Refresh, "Сбросить", tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onShowMenu) {
                        Text("☰", fontSize = 20.sp, color = Color(0xFF007AFF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 10.dp)
        ) {
            val scale = (maxHeight / 660.dp).coerceIn(0.85f, 1.25f)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp * scale),
                verticalArrangement = Arrangement.spacedBy(4.dp * scale)
            ) {
                DateCard(
                    title = "Дата начала", icon = "calendar", accentColor = Color(0xFF007AFF),
                    selectedDay = safeStartDay, selectedMonth = startMonth, selectedYear = startYear,
                    availableDays = startDays, toggleTitle = "Учитывать начальную дату", toggleChecked = includeStart,
                    onDateChange = { d, m, y -> startDay = d; startMonth = m; startYear = y },
                    onToggleChange = { includeStart = it },
                    scale = scale
                )

                DateCard(
                    title = "Дата окончания", icon = "clock", accentColor = Color(0xFFFF9500),
                    selectedDay = safeEndDay, selectedMonth = endMonth, selectedYear = endYear,
                    availableDays = endDays, toggleTitle = "Учитывать конечную дату", toggleChecked = includeEnd,
                    onDateChange = { d, m, y -> endDay = d; endMonth = m; endYear = y },
                    onToggleChange = { includeEnd = it },
                    scale = scale
                )

                if (error.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(error, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
                    }
                }

                ResultBox(
                    result = result,
                    description = DateCalculator.resultDescription(includeStart, includeEnd),
                    scale = scale
                )

                if (error.isEmpty() && result.days != 0) {
                    WidgetAddBanner(
                        endDay = safeEndDay,
                        endMonth = endMonth,
                        endYear = endYear,
                        context = context,
                        scale = scale
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetAddBanner(
    endDay: Int,
    endMonth: Int,
    endYear: Int,
    context: Context,
    scale: Float = 1f
) {
    Surface(
        onClick = {
            val intent = Intent(context, WidgetConfigureActivity::class.java).apply {
                putExtra(WidgetConfigureActivity.EXTRA_PREFILL_DAY, endDay)
                putExtra(WidgetConfigureActivity.EXTRA_PREFILL_MONTH, endMonth)
                putExtra(WidgetConfigureActivity.EXTRA_PREFILL_YEAR, endYear)
            }
            context.startActivity(intent)
        },
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF007AFF).copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp * scale, vertical = 10.dp * scale),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📅", fontSize = (16f * scale).sp, modifier = Modifier.padding(end = 8.dp * scale))
            Text("Добавить в виджет", fontSize = (14f * scale).sp, fontWeight = FontWeight.SemiBold,
                color = Color(0xFF007AFF), modifier = Modifier.weight(1f))
            Text("›", fontSize = (18f * scale).sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigDialog(context: Context, onDismiss: () -> Unit) {
    val prefs = remember { context.getSharedPreferences("datecalc_widgets", Context.MODE_PRIVATE) }
    val manager = remember { AppWidgetManager.getInstance(context) }
    val allIds = remember { manager.getAppWidgetIds(ComponentName(context, DaysWidgetReceiver::class.java))?.toList() ?: emptyList() }

    var editingWidgetId by remember { mutableIntStateOf(-1) }
    var eventName by remember { mutableStateOf("") }
    var targetDay by remember { mutableIntStateOf(1) }
    var targetMonth by remember { mutableIntStateOf(0) }
    var targetYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    val monthNames = listOf("января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря")
    val maxDay = DateCalculator.daysInMonth(targetYear, targetMonth)

    fun loadWidget(id: Int) {
        val prefix = "widget_$id"
        eventName = prefs.getString("${prefix}_event_name", "") ?: ""
        targetDay = prefs.getInt("${prefix}_target_day", 1).coerceAtLeast(1)
        targetMonth = prefs.getInt("${prefix}_target_month", 0)
        targetYear = prefs.getInt("${prefix}_target_year", Calendar.getInstance().get(Calendar.YEAR))
        editingWidgetId = id
    }

    fun saveWidget() {
        val id = editingWidgetId
        if (id < 0) return
        val prefix = "widget_$id"
        prefs.edit()
            .putString("${prefix}_event_name", eventName)
            .putInt("${prefix}_target_day", targetDay)
            .putInt("${prefix}_target_month", targetMonth)
            .putInt("${prefix}_target_year", targetYear)
            .apply()
        val intent = Intent(context, com.datecalc.widget.DaysWidgetReceiver::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id))
        }
        context.sendBroadcast(intent)
    }

    if (editingWidgetId >= 0) {
        AlertDialog(
            onDismissRequest = { editingWidgetId = -1 },
            title = { Text("Виджет #$editingWidgetId", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = eventName,
                        onValueChange = { eventName = it },
                        label = { Text("Название события") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Целевая дата:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("День", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF007AFF), modifier = Modifier.padding(bottom = 4.dp))
                            WheelColumnPicker(
                                items = (1..maxDay).map { it.toString() },
                                selectedIndex = (targetDay - 1).coerceIn(0, maxDay - 1),
                                onSelectedChange = { targetDay = it + 1 },
                                modifier = Modifier.fillMaxWidth(), itemHeight = 36.dp, fontSize = 18f,
                                accentColor = Color(0xFF007AFF), isCircular = false
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Месяц", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF9500), modifier = Modifier.padding(bottom = 4.dp))
                            WheelColumnPicker(
                                items = monthNames,
                                selectedIndex = targetMonth.coerceIn(0, 11),
                                onSelectedChange = { targetMonth = it },
                                modifier = Modifier.fillMaxWidth(), itemHeight = 36.dp, fontSize = 13f,
                                accentColor = Color(0xFFFF9500), isCircular = true
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Год", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF34C759), modifier = Modifier.padding(bottom = 4.dp))
                            WheelColumnPicker(
                                items = (2020..2040).map { it.toString() },
                                selectedIndex = (targetYear - 2020).coerceIn(0, 20),
                                onSelectedChange = { targetYear = it + 2020 },
                                modifier = Modifier.fillMaxWidth(), itemHeight = 36.dp, fontSize = 18f,
                                accentColor = Color(0xFF34C759), isCircular = false
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { saveWidget(); editingWidgetId = -1 }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { editingWidgetId = -1 }) { Text("Отмена") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Виджеты (${allIds.size}/12)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (allIds.isEmpty()) {
                        Text("Нет виджетов. Добавьте на домашний экран",
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        allIds.forEach { id ->
                            val prefix = "widget_$id"
                            val name = prefs.getString("${prefix}_event_name", "") ?: ""
                            val day = prefs.getInt("${prefix}_target_day", 0)
                            val month = prefs.getInt("${prefix}_target_month", 0)
                            val year = prefs.getInt("${prefix}_target_year", 0)
                            val monthLabel = if (month in monthNames.indices) monthNames[month] else ""
                            val display = when {
                                name.isNotEmpty() && day > 0 -> "$name ($day $monthLabel $year)"
                                day > 0 -> "$day $monthLabel $year"
                                else -> "Виджет #$id (не настроен)"
                            }
                            Surface(
                                onClick = { loadWidget(id) },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(display, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (allIds.size < 12) {
                    TextButton(onClick = {
                        android.widget.Toast.makeText(context,
                            "Зажмите пустое место на рабочем столе, откройте «Виджеты» и выберите «Дата-калькулятор»",
                            android.widget.Toast.LENGTH_LONG).show()
                        onDismiss()
                    }) { Text("+ Добавить") }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Готово") }
            }
        )
    }
}

@Composable
private fun SpinnerPicker(
    label: String,
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    items.getOrElse(selectedIndex) { "" },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text("▼", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}
