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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
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

            val isPast = daysLeft < 0
            val absDays = kotlin.math.abs(daysLeft)
            val wordForm = ruDaysWord(absDays)

            // Label above number: event name or date
            val monthNames = listOf("января", "февраля", "марта", "апреля", "мая", "июня",
                "июля", "августа", "сентября", "октября", "ноября", "декабря")
            val label = when {
                eventName.isNotEmpty() -> eventName
                targetDay > 0 -> "$targetDay ${monthNames[targetMonth]}"
                else -> ""
            }

            // Bottom line: word form or "Сегодня"
            val bottomText = when {
                daysLeft == 0 && targetDay > 0 -> "Сегодня"
                isPast -> "$wordForm назад"
                daysLeft > 0 -> wordForm
                else -> "настроить"
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(R.drawable.widget_background))
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    // Top label
                    if (label.isNotEmpty()) {
                        Text(
                            text = label,
                            style = TextStyle(
                                color = ColorProvider(R.color.widget_subtitle),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Number + word on same line — compact, no overlap
                    Row(
                        verticalAlignment = Alignment.Vertical.Bottom
                    ) {
                        Text(
                            text = if (isPast) "+$absDays" else "$absDays",
                            style = TextStyle(
                                color = if (isPast) ColorProvider(R.color.widget_subtitle)
                                    else ColorProvider(R.color.widget_accent),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = bottomText,
                            style = TextStyle(
                                color = ColorProvider(R.color.widget_subtitle),
                                fontSize = 13.sp
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
