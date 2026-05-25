package com.datecalc.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.datecalc.MainActivity
import com.datecalc.R
import com.datecalc.logic.DateCalculator
import java.util.Calendar

class DaysWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DaysWidget()
}

class DaysWidget : GlanceAppWidget() {

    @Suppress("RestrictedApi", "ResourceType")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = context.getSharedPreferences("datecalc_widget", Context.MODE_PRIVATE)
            val eventName = prefs.getString("event_name", "") ?: ""
            val targetDay = prefs.getInt("target_day", 0)
            val targetMonth = prefs.getInt("target_month", 0)
            val targetYear = prefs.getInt("target_year", 0)

            val cal = Calendar.getInstance()
            val todayDay = cal.get(Calendar.DAY_OF_MONTH)
            val todayMonth = cal.get(Calendar.MONTH)
            val todayYear = cal.get(Calendar.YEAR)

            val daysLeft = if (targetDay > 0 && targetMonth >= 0 && targetYear > 0) {
                val result = DateCalculator.calculate(
                    todayDay, todayMonth, todayYear,
                    targetDay, targetMonth, targetYear,
                    includeStart = false, includeEnd = true
                )
                if (result.error.isEmpty()) result.days else 0
            } else 0

            val isPast = daysLeft <= 0 && (targetDay > 0)
            val wordForm = ruDaysWord(kotlin.math.abs(daysLeft))

            // Format target date: "25 мая 2026"
            val targetDateStr = if (targetDay > 0) {
                val monthNames = listOf("января", "февраля", "марта", "апреля", "мая", "июня",
                    "июля", "августа", "сентября", "октября", "ноября", "декабря")
                "$targetDay ${monthNames[targetMonth]} $targetYear"
            } else ""

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(R.drawable.widget_background))
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Vertical.Bottom
                ) {
                    // Event name — subtle top label
                    if (eventName.isNotEmpty()) {
                        Text(
                            text = eventName,
                            style = TextStyle(
                                color = ColorProvider(R.color.widget_subtitle),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(1.dp))
                    } else if (targetDateStr.isNotEmpty()) {
                        Text(
                            text = targetDateStr,
                            style = TextStyle(
                                color = ColorProvider(R.color.widget_subtitle),
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(1.dp))
                    }

                    // Main number + word
                    Column(
                        horizontalAlignment = Alignment.Horizontal.Start
                    ) {
                        Text(
                            text = if (isPast && daysLeft != 0) "+" else "" + "${kotlin.math.abs(daysLeft)}",
                            style = TextStyle(
                                color = if (isPast) ColorProvider(R.color.widget_subtitle)
                                    else ColorProvider(R.color.widget_accent),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (daysLeft == 0 && targetDay > 0) "Сегодня"
                                else if (isPast) "$wordForm назад"
                                else wordForm,
                            style = TextStyle(
                                color = ColorProvider(R.color.widget_subtitle),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
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
}
