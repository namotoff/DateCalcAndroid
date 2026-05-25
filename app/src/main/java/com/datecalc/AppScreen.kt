package com.datecalc

import android.content.Intent
import android.net.Uri
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import com.datecalc.ads.AdManager
import com.datecalc.logic.DateCalculator
import com.datecalc.logic.DateCalcResult
import com.datecalc.ui.components.DateCard
import com.datecalc.ui.components.ResultBox
import com.datecalc.ui.components.WheelColumnPicker
import com.datecalc.ui.theme.DateCalcTheme
import java.util.Calendar

private const val APP_VERSION = "2.0"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen() {
    var darkTheme by remember { mutableStateOf(false) }
    DateCalcTheme(darkTheme = darkTheme) {
        val context = LocalContext.current
        var showMenu by remember { mutableStateOf(false) }
        var showWidgetSetup by remember { mutableStateOf(false) }

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
                        if (showWidgetSetup) {
                            Text("Настройка виджета", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
                        } else {
                            Text("Дата-калькулятор", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    navigationIcon = {
                        if (showWidgetSetup) {
                            IconButton(onClick = { showWidgetSetup = false }) {
                                Icon(Icons.Filled.ArrowBack, "Назад", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    },
                    actions = {
                        if (!showWidgetSetup) {
                            IconButton(onClick = { darkTheme = !darkTheme }) {
                                Text(if (darkTheme) "☀️" else "🌙", fontSize = 16.sp)
                            }
                            IconButton(onClick = {
                                startDay = todayDay; startMonth = todayMonth; startYear = todayYear
                                endDay = todayDay; endMonth = todayMonth; endYear = todayYear
                                includeStart = true; includeEnd = true
                            }) { Text("↺", fontSize = 18.sp) }
                            IconButton(onClick = { showMenu = true }) {
                                Text("☰", fontSize = 20.sp, color = Color(0xFF007AFF))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->

            if (showWidgetSetup) {
                WidgetSetupScreen(paddingValues, onDone = {
                    showWidgetSetup = false
                })
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DateCard("Дата начала", "calendar", Color(0xFF007AFF), safeStartDay, startMonth, startYear, startDays, "Учитывать начальную дату", includeStart, { d, m, y -> startDay = d; startMonth = m; startYear = y }, { includeStart = it })
                    DateCard("Дата окончания", "clock", Color(0xFFFF9500), safeEndDay, endMonth, endYear, endDays, "Учитывать конечную дату", includeEnd, { d, m, y -> endDay = d; endMonth = m; endYear = y }, { includeEnd = it })

                    if (error.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f), modifier = Modifier.fillMaxWidth()) {
                            Text(error, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
                        }
                    }

                    ResultBox(result, DateCalculator.resultDescription(includeStart, includeEnd))
                }

                // Banner ad at bottom
                if (AdManager.hasBannerSlot()) {
                    AndroidView(
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
                                AdManager.createBanner(ctx as android.app.Activity, this)
                            }
                        },
                        modifier = Modifier.padding(paddingValues).fillMaxWidth()
                    )
                }
            }
        }

        // Menu BottomSheet
        if (showMenu) {
            ModalBottomSheet(
                onDismissRequest = { showMenu = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Меню", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))

                    MenuChip("ℹ️", "О приложении", Color(0xFF007AFF)) {
                        showMenu = false
                    }
                    MenuChip("📅", "Настроить виджет", Color(0xFF5856D6)) {
                        showMenu = false
                        showWidgetSetup = true
                    }
                    MenuChip("🔒", "Политика конфиденциальности", Color(0xFF34C759)) {
                        showMenu = false
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://namotoff.github.io/datecalc-privacy/")))
                    }
                    MenuChip("✉️", "Написать разработчику", Color(0xFFFF9500)) {
                        showMenu = false
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:edazin@bk.ru")).apply { putExtra(Intent.EXTRA_SUBJECT, "Дата-калькулятор") })
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MenuChip(emoji: String, label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetSetupScreen(paddingValues: PaddingValues, onDone: () -> Unit) {
    val context = LocalContext.current

    val cal = Calendar.getInstance()
    var eventDay by remember { mutableIntStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    var eventMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) }
    var eventYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var eventName by remember { mutableStateOf("") }

    val safeDay = eventDay.coerceAtMost(DateCalculator.daysInMonth(eventYear, eventMonth))
    val days = remember(eventYear, eventMonth) { DateCalculator.validDaysForMonth(eventYear, eventMonth).map { it.toString() } }

    // Calculate preview days
    val previewDays = remember(safeDay, eventMonth, eventYear) {
        val today = Calendar.getInstance()
        val result = DateCalculator.calculate(
            today.get(Calendar.DAY_OF_MONTH), today.get(Calendar.MONTH), today.get(Calendar.YEAR),
            safeDay, eventMonth, eventYear,
            includeStart = false, includeEnd = true
        )
        if (result.error.isEmpty()) result.days else 0
    }

    LaunchedEffect(safeDay) {
        if (eventDay != safeDay) eventDay = safeDay
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Widget preview
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1C1C1E),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    // Label: event name or target date
                    if (eventName.isNotEmpty()) {
                        Text(eventName, color = Color(0xFF8E8E93), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(1.dp))
                    } else {
                        val monthNames = listOf("января", "февраля", "марта", "апреля", "мая", "июня",
                            "июля", "августа", "сентября", "октября", "ноября", "декабря")
                        Text("$safeDay ${monthNames[eventMonth]} $eventYear", color = Color(0xFF8E8E93), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                    // Main number
                    val isPast = previewDays < 0
                    Text(
                        text = "${if (isPast) "+" else ""}${kotlin.math.abs(previewDays)}",
                        color = if (isPast) Color(0xFF8E8E93) else Color(0xFF0A84FF),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Word form
                    Text(
                        text = when {
                            previewDays == 0 -> "Сегодня"
                            isPast -> "${ruDaysWord(kotlin.math.abs(previewDays))} назад"
                            else -> ruDaysWord(previewDays)
                        },
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Event name — native EditText (Compose OutlinedTextField broken on real devices)
        val textColor = if (isSystemInDarkTheme()) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        val hintColor = android.graphics.Color.parseColor("#8E8E93")
        val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        AndroidView(
            factory = { ctx ->
                android.widget.EditText(ctx).apply {
                    hint = "Название события"
                    setSingleLine(true)
                    textSize = 16f
                    setPadding(48, 32, 48, 32)
                    background = null
                    setHintTextColor(hintColor)
                    setTextColor(textColor)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    bgColor,
                    RoundedCornerShape(12.dp)
                ),
            update = { editText ->
                editText.setOnKeyListener { _, _, event ->
                    if (event.action == android.view.KeyEvent.ACTION_UP) {
                        eventName = editText.text.toString()
                    }
                    false
                }
                editText.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        eventName = editText.text.toString()
                    }
                }
            }
        )

        // Date pickers
        Text("Дата события", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WheelColumnPicker(
                items = days, selectedIndex = safeDay - 1, accentColor = Color(0xFF007AFF),
                modifier = Modifier.weight(1f),
                onSelectedChange = { index -> eventDay = days[index].toInt() }
            )
            WheelColumnPicker(
                items = DateCalculator.MONTHS_RU, selectedIndex = eventMonth, accentColor = Color(0xFF007AFF),
                modifier = Modifier.weight(1f),
                onSelectedChange = { index -> eventMonth = index }
            )
            WheelColumnPicker(
                items = DateCalculator.YEARS.map { it.toString() },
                selectedIndex = (eventYear - DateCalculator.YEARS.first()).coerceIn(0, DateCalculator.YEARS.lastIndex),
                accentColor = Color(0xFF007AFF),
                modifier = Modifier.weight(1f),
                onSelectedChange = { index -> eventYear = DateCalculator.YEARS[index] }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Save button
        Button(
            onClick = {
                val prefs = context.getSharedPreferences("datecalc_widget", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("event_name", eventName)
                    .putInt("target_day", safeDay)
                    .putInt("target_month", eventMonth)
                    .putInt("target_year", eventYear)
                    .apply()
                onDone()
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
        ) {
            Text("Сохранить и закрыть", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun ruDaysWord(n: Int): String {
    if (n == 0) return "дней"
    val abs = kotlin.math.abs(n)
    val lastTwo = abs % 100
    val lastOne = abs % 10
    return when {
        lastTwo in 11..19 -> "дней"
        lastOne == 1 -> "день"
        lastOne in 2..4 -> "дня"
        else -> "дней"
    }
}

