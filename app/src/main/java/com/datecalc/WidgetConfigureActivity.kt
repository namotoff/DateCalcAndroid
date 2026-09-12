package com.datecalc

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import com.datecalc.ui.components.WheelColumnPicker
import com.datecalc.logic.DateCalculator
import com.datecalc.widget.DaysWidgetReceiver
import kotlinx.coroutines.launch
import java.util.Calendar

class WidgetConfigureActivity : ComponentActivity() {

    private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    companion object {
        private const val REQUEST_BIND = 1001
        const val EXTRA_PREFILL_DAY = "com.datecalc.EXTRA_PREFILL_DAY"
        const val EXTRA_PREFILL_MONTH = "com.datecalc.EXTRA_PREFILL_MONTH"
        const val EXTRA_PREFILL_YEAR = "com.datecalc.EXTRA_PREFILL_YEAR"
        /** Длиннее в строку списка и в сам виджет всё равно не помещается. */
        const val NAME_MAX_LENGTH = 30
    }

    /** После сохранения существующего виджета уводим на рабочий стол — там виден результат. */
    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun pinNewWidget(name: String, day: Int, month: Int, year: Int, countDown: Boolean) {
        val provider = ComponentName(this, DaysWidgetReceiver::class.java)
        val manager = AppWidgetManager.getInstance(this)

        if (!manager.isRequestPinAppWidgetSupported) {
            Toast.makeText(
                this,
                "Лаунчер не поддерживает быстрое добавление. Зажмите пустое место на рабочем столе, откройте «Виджеты» и выберите «Дата-калькулятор»",
                Toast.LENGTH_LONG
            ).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // Реальный appWidgetId станет известен только после подтверждения в системном
        // диалоге, поэтому дата временно сохраняется и подхватывается в
        // DaysWidgetReceiver.onUpdate() для нового виджета (успех-callback у
        // requestPinAppWidget не на всех лаунчерах доставляется надёжно).
        getSharedPreferences("datecalc_widgets", Context.MODE_PRIVATE).edit()
            .putString("pending_event_name", name)
            .putInt("pending_day", day)
            .putInt("pending_month", month)
            .putInt("pending_year", year)
            .putBoolean("pending_count_down", countDown)
            .putLong("pending_timestamp", System.currentTimeMillis())
            .apply()

        // Сам callback приходит только ПОСЛЕ того, как пользователь подтвердит
        // добавление в системном диалоге — на этом этапе уже безопасно уводить
        // пользователя на домашний экран, не перебивая диалог подтверждения.
        val homePendingIntent = android.app.PendingIntent.getBroadcast(
            this, System.currentTimeMillis().toInt(),
            Intent(this, DaysWidgetReceiver::class.java).apply {
                action = DaysWidgetReceiver.ACTION_WIDGET_PINNED
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        manager.requestPinAppWidget(provider, null, homePendingIntent)
        finish()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_BIND && resultCode == RESULT_OK) {
            val widgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) ?: pendingWidgetId
            setContent {
                WidgetEditScreen(
                    widgetId = widgetId,
                    onSaved = { eventName: String, day: Int, month: Int, year: Int, countDown: Boolean ->
                        val prefs = getSharedPreferences("datecalc_widgets", Context.MODE_PRIVATE)
                        val prefix = "widget_$widgetId"
                        prefs.edit()
                            .putString("${prefix}_event_name", eventName)
                            .putInt("${prefix}_target_day", day)
                            .putInt("${prefix}_target_month", month)
                            .putInt("${prefix}_target_year", year)
                            .putBoolean("${prefix}_count_down", countDown)
                            .commit()
                        // Обновляем виджет
                        DaysWidgetReceiver.updateAllWidgets(this@WidgetConfigureActivity)
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        }
                        setResult(RESULT_OK, resultValue)
                        goHome()
                        finish()
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val prefillDay = intent?.getIntExtra(EXTRA_PREFILL_DAY, -1)?.takeIf { it > 0 }
        val prefillMonth = intent?.getIntExtra(EXTRA_PREFILL_MONTH, -1)?.takeIf { it >= 0 }
        val prefillYear = intent?.getIntExtra(EXTRA_PREFILL_YEAR, -1)?.takeIf { it > 0 }
        val isQuickAdd = appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID &&
            prefillDay != null && prefillMonth != null && prefillYear != null

        setContent {
            when {
                appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID -> {
                    WidgetEditScreen(
                        widgetId = appWidgetId,
                        onSaved = { eventName: String, day: Int, month: Int, year: Int, countDown: Boolean ->
                            val prefs = getSharedPreferences("datecalc_widgets", Context.MODE_PRIVATE)
                            val prefix = "widget_$appWidgetId"
                            prefs.edit()
                                .putString("${prefix}_event_name", eventName)
                                .putInt("${prefix}_target_day", day)
                                .putInt("${prefix}_target_month", month)
                                .putInt("${prefix}_target_year", year)
                                .putBoolean("${prefix}_count_down", countDown)
                                .commit()
                            // Обновляем виджет
                            DaysWidgetReceiver.updateAllWidgets(this@WidgetConfigureActivity)
                            val resultValue = Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            setResult(RESULT_OK, resultValue)
                            goHome()
                            finish()
                        },
                        onCancel = {
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    )
                }
                isQuickAdd -> {
                    WidgetEditScreen(
                        widgetId = -1,
                        initialDay = prefillDay,
                        initialMonth = prefillMonth,
                        initialYear = prefillYear,
                        onSaved = { name, day, month, year, down -> pinNewWidget(name, day, month, year, down) },
                        onCancel = {
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    )
                }
                else -> {
                    WidgetListScreen(
                        onSelect = { id ->
                            setContent {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                WidgetEditScreen(
                                    widgetId = id,
                                    onSaved = { name, day, month, year, down ->
                                        val prefix = "widget_$id"
                                        context.getSharedPreferences("datecalc_widgets", Context.MODE_PRIVATE).edit()
                                            .putString("${prefix}_event_name", name)
                                            .putInt("${prefix}_target_day", day)
                                            .putInt("${prefix}_target_month", month)
                                            .putInt("${prefix}_target_year", year)
                                            .putBoolean("${prefix}_count_down", down)
                                            .commit()
                                        com.datecalc.widget.DaysWidgetReceiver.updateAllWidgets(context)
                                        goHome()
                                        finish()
                                    },
                                    onCancel = {
                                        setResult(RESULT_CANCELED)
                                        finish()
                                    }
                                )
                            }
                        },
                        onAddNew = {
                            setContent {
                                WidgetEditScreen(
                                    widgetId = -1,
                                    onSaved = { name, day, month, year, down -> pinNewWidget(name, day, month, year, down) },
                                    onCancel = {
                                        setResult(RESULT_CANCELED)
                                        finish()
                                    }
                                )
                            }
                        },
                        onCancel = {
                            setResult(RESULT_CANCELED)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetListScreen(onSelect: (Int) -> Unit, onAddNew: () -> Unit, onCancel: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("datecalc_widgets", Context.MODE_PRIVATE) }
    val manager = remember { AppWidgetManager.getInstance(context) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var deleteId by remember { mutableIntStateOf(-1) }
    val allIds = remember(refreshKey) {
        val bound = manager.getAppWidgetIds(ComponentName(context, DaysWidgetReceiver::class.java))?.toList() ?: emptyList()
        // Виджет, потерявший привязку к приложению, из getAppWidgetIds() пропадает, но
        // остаётся на рабочем столе и продолжает открывать настройку. Поэтому в список
        // добавляем и те id, для которых сохранены настройки.
        val configured = prefs.all.keys.mapNotNull { key ->
            key.removePrefix("widget_").removeSuffix("_target_day")
                .takeIf { key.startsWith("widget_") && key.endsWith("_target_day") }
                ?.toIntOrNull()
        }
        (bound + configured).distinct().sorted()
    }

    val monthNames = listOf("янв", "фев", "мар", "апр", "мая", "июн",
        "июл", "авг", "сен", "окт", "ноя", "дек")

    if (deleteId > 0) {
        val deleteName = (prefs.getString("widget_${deleteId}_event_name", "") ?: "").ifBlank { "виджет" }
        AlertDialog(
            onDismissRequest = { deleteId = -1 },
            title = { Text("Удалить «$deleteName»?") },
            text = { Text("Настройки виджета будут удалены. Сам виджет нужно убрать с рабочего стола вручную: нажмите на него и удерживайте, затем выберите «Удалить».") },
            confirmButton = {
                TextButton(onClick = {
                    val prefix = "widget_$deleteId"
                    prefs.edit().remove("${prefix}_event_name")
                        .remove("${prefix}_target_day")
                        .remove("${prefix}_target_month")
                        .remove("${prefix}_target_year")
                        .remove("${prefix}_count_down")
                        .apply()
                    // Снять виджет с рабочего стола может только лаунчер — у приложения
                    // такого API нет. Поэтому просто сбрасываем его в пустое состояние.
                    DaysWidgetReceiver.updateAllWidgets(context)
                    deleteId = -1
                    refreshKey++
                    android.widget.Toast.makeText(context, "Чтобы убрать виджет, удерживайте его на рабочем столе и выберите «Удалить»", android.widget.Toast.LENGTH_LONG).show()
                }) { Text("Удалить", color = Color(0xFFFF3B30)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteId = -1 }) { Text("Отмена") }
            },
            containerColor = Color(0xFF2C2C2E),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF1C1C1E)) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .safeDrawingPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Выберите виджет", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (allIds.isEmpty()) {
                        Text("Нет виджетов", fontSize = 16.sp, color = Color.Gray)
                    } else {
                        allIds.forEach { id ->
                            val prefix = "widget_$id"
                            val name = prefs.getString("${prefix}_event_name", "") ?: ""
                            val day = prefs.getInt("${prefix}_target_day", 0)
                            val month = prefs.getInt("${prefix}_target_month", 0)
                            val year = prefs.getInt("${prefix}_target_year", 0)
                            val monthLabel = if (month in monthNames.indices) monthNames[month] else ""
                            val dateLabel = if (day > 0 && year > 0) "$day $monthLabel $year" else ""
                            // Номера по позиции в списке скакали при удалении, поэтому виджет
                            // опознаётся собственным названием, а дата идёт подсказкой.
                            val display = when {
                                name.isNotBlank() -> name
                                dateLabel.isNotEmpty() -> dateLabel
                                else -> "Не настроен"
                            }
                            val hint = if (name.isNotBlank()) dateLabel else ""
                            Surface(
                                onClick = { onSelect(id) },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF2C2C2E),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(display, fontSize = 16.sp, color = Color.White,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (hint.isNotEmpty()) {
                                            Text(hint, fontSize = 13.sp, color = Color.Gray)
                                        }
                                    }
                                    IconButton(onClick = { deleteId = id }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Удалить", tint = Color(0xFFFF3B30))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onAddNew() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Добавить виджет", fontWeight = FontWeight.Bold, color = Color.White) }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Закрыть", color = Color.White) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetEditScreen(
    widgetId: Int,
    onSaved: (String, Int, Int, Int, Boolean) -> Unit,
    onCancel: () -> Unit,
    initialDay: Int? = null,
    initialMonth: Int? = null,
    initialYear: Int? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("datecalc_widgets", Context.MODE_PRIVATE) }
    val prefix = "widget_$widgetId"
    val isNew = widgetId < 0

    val storedName = prefs.getString("${prefix}_event_name", "") ?: ""
    var eventName by remember { mutableStateOf(storedName) }
    // Пока пользователь не тронул поле, название следует за выбранной датой,
    // чтобы обязательное поле не требовало ручного ввода на ровном месте.
    var nameTouched by remember { mutableStateOf(storedName.isNotBlank()) }
    var nameError by remember { mutableStateOf("") }
    var targetDay by remember { mutableIntStateOf(
        (if (isNew && initialDay != null) initialDay else prefs.getInt("${prefix}_target_day", Calendar.getInstance().get(Calendar.DAY_OF_MONTH))).coerceAtLeast(1)
    ) }
    var targetMonth by remember { mutableIntStateOf(
        if (isNew && initialMonth != null) initialMonth else prefs.getInt("${prefix}_target_month", Calendar.getInstance().get(Calendar.MONTH))
    ) }
    var targetYear by remember { mutableIntStateOf(
        if (isNew && initialYear != null) initialYear else prefs.getInt("${prefix}_target_year", Calendar.getInstance().get(Calendar.YEAR))
    ) }
    var countDown by remember { mutableStateOf(prefs.getBoolean("${prefix}_count_down", true)) }

    val monthNames = listOf("января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря")
    val shortMonths = listOf("янв", "фев", "мар", "апр", "мая", "июн",
        "июл", "авг", "сен", "окт", "ноя", "дек")
    val maxDay = DateCalculator.daysInMonth(targetYear, targetMonth)

    val suggestedName = "$targetDay ${shortMonths.getOrElse(targetMonth) { "" }} $targetYear"
    LaunchedEffect(targetDay, targetMonth, targetYear, nameTouched) {
        if (!nameTouched) eventName = suggestedName
    }

    /** Название должно быть непустым и не совпадать с названием другого виджета. */
    fun validationError(candidate: String): String {
        val trimmed = candidate.trim()
        if (trimmed.isEmpty()) return "Введите название события"
        val clash = prefs.all.keys.any { key ->
            if (!key.startsWith("widget_") || !key.endsWith("_event_name")) return@any false
            val otherId = key.removePrefix("widget_").removeSuffix("_event_name").toIntOrNull()
            if (otherId == null || otherId == widgetId) return@any false
            val otherName = (prefs.getString(key, "") ?: "").trim()
            otherName.isNotEmpty() && otherName.equals(trimmed, ignoreCase = true)
        }
        return if (clash) "Такое название уже есть, измените его" else ""
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF1C1C1E)) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Редактирование виджета", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = eventName,
                    onValueChange = {
                        eventName = it.take(WidgetConfigureActivity.NAME_MAX_LENGTH)
                        nameTouched = true
                        if (nameError.isNotEmpty()) nameError = ""
                    },
                    label = { Text("Название события") },
                    isError = nameError.isNotEmpty(),
                    supportingText = if (nameError.isNotEmpty()) {
                        { Text(nameError, color = Color(0xFFFF3B30)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF007AFF),
                        cursorColor = Color(0xFF007AFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        // Без этого в состоянии ошибки Material берёт свой тёмный цвет
                        // по умолчанию, и введённый текст пропадает на тёмном фоне.
                        errorTextColor = Color.White,
                        errorCursorColor = Color(0xFFFF3B30),
                        errorBorderColor = Color(0xFFFF3B30),
                        errorLabelColor = Color(0xFFFF3B30)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Целевая дата:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("День", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = Color.White, modifier = Modifier.padding(bottom = 4.dp))
                        WheelColumnPicker(
                            items = (1..maxDay).map { it.toString() },
                            selectedIndex = (targetDay - 1).coerceIn(0, maxDay - 1),
                            onSelectedChange = { targetDay = it + 1 },
                            modifier = Modifier.fillMaxWidth(), itemHeight = 32.dp, fontSize = 24f,
                            accentColor = Color.White, selectedTextColor = Color.White,
                            unselectedTextColor = Color(0xFF888888), pickerBgColor = Color(0xFF2C2C2E),
                            isCircular = false
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1.5f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Месяц", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = Color.White, modifier = Modifier.padding(bottom = 4.dp))
                        WheelColumnPicker(
                            items = monthNames,
                            selectedIndex = targetMonth.coerceIn(0, 11),
                            onSelectedChange = { targetMonth = it },
                            modifier = Modifier.fillMaxWidth(), itemHeight = 32.dp, fontSize = 18f,
                            accentColor = Color.White, selectedTextColor = Color.White,
                            unselectedTextColor = Color(0xFF888888), pickerBgColor = Color(0xFF2C2C2E),
                            isCircular = true
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Год", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = Color.White, modifier = Modifier.padding(bottom = 4.dp))
                        WheelColumnPicker(
                            items = (2020..2040).map { it.toString() },
                            selectedIndex = (targetYear - 2020).coerceIn(0, 20),
                            onSelectedChange = { targetYear = it + 2020 },
                            modifier = Modifier.fillMaxWidth(), itemHeight = 32.dp, fontSize = 23f,
                            accentColor = Color.White, selectedTextColor = Color.White,
                            unselectedTextColor = Color(0xFF888888), pickerBgColor = Color(0xFF2C2C2E),
                            isCircular = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = countDown,
                        onClick = { countDown = true },
                        label = { Text("Обратный отсчёт (-1)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF007AFF),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF2C2C2E),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !countDown,
                        onClick = { countDown = false },
                        label = { Text("Отсчёт дней (+1)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF007AFF),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF2C2C2E),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onCancel,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) { Text("Отмена", color = Color.White) }

                    Button(
                        onClick = {
                            val error = validationError(eventName)
                            if (error.isEmpty()) {
                                onSaved(eventName.trim(), targetDay, targetMonth, targetYear, countDown)
                            } else {
                                nameError = error
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) { Text("Готово", fontWeight = FontWeight.Bold, color = Color.White) }
                }
            }
        }
    }
}
