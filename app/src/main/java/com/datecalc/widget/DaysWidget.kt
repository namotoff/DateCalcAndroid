package com.datecalc.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.datecalc.R
import com.datecalc.WidgetConfigureActivity
import java.util.Calendar

class DaysWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("datecalc_widgets", Context.MODE_PRIVATE)
        for (widgetId in appWidgetIds) {
            applyPendingQuickAdd(prefs, widgetId)
            updateWidget(context, appWidgetManager, widgetId)
        }
        scheduleDailyUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_DAILY_UPDATE,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                updateAllWidgets(context)
                scheduleDailyUpdate(context)
            }
            ACTION_WIDGET_PINNED -> {
                val home = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(home)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleDailyUpdate(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val prefs = context.getSharedPreferences("datecalc_widgets", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (id in appWidgetIds) {
            val prefix = "widget_$id"
            editor.remove("${prefix}_event_name")
                .remove("${prefix}_target_day")
                .remove("${prefix}_target_month")
                .remove("${prefix}_target_year")
                .remove("${prefix}_count_down")
        }
        editor.apply()
    }

    companion object {
        const val ACTION_DAILY_UPDATE = "com.datecalc.DAILY_WIDGET_UPDATE"
        const val ACTION_WIDGET_PINNED = "com.datecalc.WIDGET_PINNED"
        private const val REQUEST_CODE = 777
        private const val PENDING_MAX_AGE_MS = 120_000L

        private fun applyPendingQuickAdd(prefs: android.content.SharedPreferences, widgetId: Int) {
            val prefix = "widget_$widgetId"
            if (prefs.contains("${prefix}_target_day")) return

            val pendingDay = prefs.getInt("pending_day", -1)
            val pendingTimestamp = prefs.getLong("pending_timestamp", 0)
            val isFresh = pendingDay > 0 && (System.currentTimeMillis() - pendingTimestamp) <= PENDING_MAX_AGE_MS

            val editor = prefs.edit()
            if (isFresh) {
                editor.putString("${prefix}_event_name", prefs.getString("pending_event_name", "") ?: "")
                    .putInt("${prefix}_target_day", pendingDay)
                    .putInt("${prefix}_target_month", prefs.getInt("pending_month", 0))
                    .putInt("${prefix}_target_year", prefs.getInt("pending_year", 0))
                    .putBoolean("${prefix}_count_down", prefs.getBoolean("pending_count_down", true))
            }
            if (pendingDay > 0) {
                editor.remove("pending_event_name").remove("pending_day").remove("pending_month")
                    .remove("pending_year").remove("pending_count_down").remove("pending_timestamp")
            }
            editor.apply()
        }

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DaysWidgetReceiver::class.java))
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val prefs = context.getSharedPreferences("datecalc_widgets", Context.MODE_PRIVATE)
            val prefix = "widget_$widgetId"

            val eventName = prefs.getString("${prefix}_event_name", "") ?: ""
            val targetDay = prefs.getInt("${prefix}_target_day", 0)
            val targetMonth = prefs.getInt("${prefix}_target_month", 0)
            val targetYear = prefs.getInt("${prefix}_target_year", 0)
            val countDown = prefs.getBoolean("${prefix}_count_down", true)

            val daysLeft = if (targetDay > 0 && targetYear > 0) {
                val targetCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, targetYear)
                    set(Calendar.MONTH, targetMonth)
                    set(Calendar.DAY_OF_MONTH, targetDay)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Math.round((targetCal.timeInMillis - todayCal.timeInMillis).toDouble() / 86400000.0).toInt()
            } else 0

            val isPast = daysLeft < 0
            val absDays = kotlin.math.abs(daysLeft)
            val wordForm = ruDaysWord(absDays)

            val monthNames = listOf("янв", "фев", "мар", "апр", "мая", "июн",
                "июл", "авг", "сен", "окт", "ноя", "дек")
            val monthNamesFull = listOf("января", "февраля", "марта", "апреля", "мая", "июня",
                "июля", "августа", "сентября", "октября", "ноября", "декабря")

            val shortDate = if (targetDay > 0 && targetMonth in monthNames.indices && targetYear > 0) {
                "$targetDay ${monthNames[targetMonth]} $targetYear"
            } else ""

            val label = when {
                eventName.isNotEmpty() -> eventName
                shortDate.isNotEmpty() -> shortDate
                else -> "Настроить виджет"
            }

            val bottomText = when {
                targetDay == 0 || targetYear == 0 -> "Нажмите для настройки"
                daysLeft == 0 -> "Сегодня!"
                countDown -> {
                    if (isPast) "$absDays $wordForm назад" else "Осталось $absDays $wordForm"
                }
                else -> {
                    if (isPast) "Прошло $absDays $wordForm" else "Осталось $absDays $wordForm"
                }
            }

            val topText = when {
                targetDay > 0 && targetMonth in monthNamesFull.indices && targetYear > 0 ->
                    "$targetDay ${monthNamesFull[targetMonth]} $targetYear"
                else -> ""
            }

            val numberText = when {
                targetDay == 0 || targetYear == 0 -> "—"
                daysLeft == 0 -> "0"
                countDown -> if (isPast) "+$absDays" else "$absDays"
                else -> if (!isPast) "-$absDays" else "$absDays"
            }

            val numberColor = when {
                targetDay == 0 || targetYear == 0 -> 0xFF888888.toInt()
                isPast && countDown -> 0xFF888888.toInt()
                !isPast && !countDown -> 0xFF888888.toInt()
                else -> 0xFF007AFF.toInt()
            }

            val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
                setTextViewText(R.id.widget_label, label)
                setTextColor(R.id.widget_label, 0xFF888888.toInt())

                if (topText.isNotEmpty() && topText != label) {
                    setViewVisibility(R.id.widget_top_text, android.view.View.VISIBLE)
                    setTextViewText(R.id.widget_top_text, topText)
                    setTextColor(R.id.widget_top_text, 0xFF888888.toInt())
                } else {
                    setViewVisibility(R.id.widget_top_text, android.view.View.GONE)
                }

                setTextViewText(R.id.widget_number, numberText)
                setTextColor(R.id.widget_number, numberColor)

                setTextViewText(R.id.widget_bottom, bottomText)
                setTextColor(R.id.widget_bottom, 0xFF888888.toInt())

                val configureIntent = Intent(context, WidgetConfigureActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, widgetId, configureIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }

            manager.updateAppWidget(widgetId, views)
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

        fun scheduleDailyUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, DaysWidgetReceiver::class.java).apply {
                action = ACTION_DAILY_UPDATE
            }
            val pending = android.app.PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 1)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP, nextMidnight.timeInMillis, pending
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP, nextMidnight.timeInMillis, pending
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP, nextMidnight.timeInMillis, pending
                    )
                }
            } catch (_: SecurityException) {
                try {
                    alarmManager.setAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP, nextMidnight.timeInMillis, pending
                    )
                } catch (_: Exception) {}
            }
        }
    }
}
