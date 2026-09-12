package com.datecalc.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import com.datecalc.logic.DateCalculator

@Composable
fun DateCard(
    title: String,
    icon: String,
    accentColor: Color,
    selectedDay: Int,
    selectedMonth: Int,
    selectedYear: Int,
    availableDays: List<String>,
    toggleTitle: String,
    toggleChecked: Boolean,
    onDateChange: (day: Int, month: Int, year: Int) -> Unit,
    onToggleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f
) {
    val cardBg = MaterialTheme.colorScheme.surface
    val cardStroke = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val toggleColor = MaterialTheme.colorScheme.onSurfaceVariant

    val dayIndex = (selectedDay - 1).coerceIn(0, availableDays.lastIndex.coerceAtLeast(0))
    val yearIndex = (selectedYear - DateCalculator.YEARS.first()).coerceIn(0, DateCalculator.YEARS.lastIndex)

    val vPad = 10.dp * scale
    val headerSpacer = 8.dp * scale
    val labelSpacer = 4.dp * scale
    val wheelItemHeight = 32.dp * scale
    val dayFont = 20f * scale
    val monthFont = 14f * scale
    val yearFont = 19f * scale
    val titleFontSize = (15f * scale).sp
    val labelFontSize = (11f * scale).sp
    val togglePad = 10.dp * scale

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, cardStroke, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = vPad)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = RoundedCornerShape(8.dp), color = accentColor, modifier = Modifier.size(26.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (icon) { "calendar" -> Icons.Filled.DateRange else -> Icons.Filled.Schedule },
                            contentDescription = title, tint = Color.White, modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = titleFontSize, fontWeight = FontWeight.SemiBold, color = textColor)
            }

            Spacer(modifier = Modifier.height(headerSpacer))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("День", fontSize = labelFontSize, fontWeight = FontWeight.SemiBold, color = labelColor,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("Месяц", fontSize = labelFontSize, fontWeight = FontWeight.SemiBold, color = labelColor,
                    modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                Text("Год", fontSize = labelFontSize, fontWeight = FontWeight.SemiBold, color = labelColor,
                    modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(labelSpacer))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                WheelColumnPicker(
                    items = availableDays,
                    selectedIndex = dayIndex,
                    onSelectedChange = { idx ->
                        val newDay = idx + 1
                        val maxDay = DateCalculator.validDaysForMonth(selectedYear, selectedMonth).last()
                        when {
                            newDay > maxDay -> {
                                if (selectedMonth == 11) {
                                    onDateChange(newDay - maxDay, 0, selectedYear + 1)
                                } else {
                                    onDateChange(newDay - maxDay, selectedMonth + 1, selectedYear)
                                }
                            }
                            newDay < 1 -> {
                                val prevMonth = if (selectedMonth == 0) 11 else selectedMonth - 1
                                val prevYear = if (selectedMonth == 0) selectedYear - 1 else selectedYear
                                val prevMaxDay = DateCalculator.validDaysForMonth(prevYear, prevMonth).last()
                                onDateChange(prevMaxDay, prevMonth, prevYear)
                            }
                            else -> onDateChange(newDay, selectedMonth, selectedYear)
                        }
                    },
                    modifier = Modifier.weight(1f), itemHeight = wheelItemHeight, fontSize = dayFont, accentColor = accentColor,
                    isCircular = false
                )

                WheelColumnPicker(
                    items = DateCalculator.MONTHS_RU,
                    selectedIndex = selectedMonth,
                    onSelectedChange = { newMonth ->
                        val diff = newMonth - selectedMonth
                        val newYear = when {
                            diff < -6 -> selectedYear + 1
                            diff > 6 -> selectedYear - 1
                            else -> selectedYear
                        }
                        val clampedYear = newYear.coerceIn(DateCalculator.YEARS.first(), DateCalculator.YEARS.last())
                        val maxDay = DateCalculator.validDaysForMonth(clampedYear, newMonth).last()
                        val newDay = selectedDay.coerceAtMost(maxDay)
                        onDateChange(newDay, newMonth, clampedYear)
                    },
                    modifier = Modifier.weight(1.5f), itemHeight = wheelItemHeight, fontSize = monthFont, accentColor = accentColor,
                    isCircular = true
                )

                WheelColumnPicker(
                    items = DateCalculator.YEARS.map { it.toString() },
                    selectedIndex = yearIndex,
                    onSelectedChange = { idx ->
                        val newYear = DateCalculator.YEARS.elementAtOrNull(idx) ?: selectedYear
                        val maxDay = DateCalculator.validDaysForMonth(newYear, selectedMonth).last()
                        val newDay = selectedDay.coerceAtMost(maxDay)
                        onDateChange(newDay, selectedMonth, newYear)
                    },
                    modifier = Modifier.weight(1.3f), itemHeight = wheelItemHeight, fontSize = yearFont, accentColor = accentColor,
                    isCircular = false
                )
            }

            Spacer(modifier = Modifier.height(togglePad))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Switch(checked = toggleChecked, onCheckedChange = onToggleChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = accentColor, checkedThumbColor = Color.White),
                    modifier = Modifier.height(20.dp).scale(scale))
                Spacer(modifier = Modifier.width(6.dp))
                Text(toggleTitle, fontSize = (12f * scale).sp, color = toggleColor,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
            }
        }
    }
}
