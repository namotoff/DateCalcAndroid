package com.datecalc.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.datecalc.MainActivity
import com.datecalc.logic.DateCalculator
import java.util.Calendar

class DaysWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DaysWidget()
}

class DaysWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = context.getSharedPreferences("datecalc_widget", Context.MODE_PRIVATE)
            val eventName = prefs.getString("event_name", "") ?: ""
            val targetDay = prefs.getInt("target_day", 0)
            val targetMonth = prefs.getInt("target_month", 0)
            val targetYear = prefs.getInt("target_year", 0)

            val daysLeft = if (targetDay > 0 && targetMonth >= 0 && targetYear > 0) {
                val cal = Calendar.getInstance()
                val result = DateCalculator.calculate(
                    cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH), cal.get(Calendar.YEAR),
                    targetDay, targetMonth, targetYear,
                    includeStart = false, includeEnd = true
                )
                if (result.error.isEmpty()) result.days else 0
            } else 0

            val wordForm = ruDaysWord(daysLeft)

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(com.datecalc.R.drawable.widget_background))
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    if (eventName.isNotEmpty()) {
                        Text(
                            text = eventName.uppercase(),
                            style = TextStyle(
                                color = ColorProvider(android.graphics.Color.parseColor("#8E8E93")),
                                fontSize = 11.sp
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.Start,
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = "$daysLeft",
                            style = TextStyle(
                                color = ColorProvider(android.graphics.Color.parseColor("#0A84FF")),
                                fontSize = 40.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = wordForm,
                            style = TextStyle(
                                color = ColorProvider(android.graphics.Color.parseColor("#8E8E93")),
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
