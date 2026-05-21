package com.datecalc.logic

import java.util.Calendar

data class DateCalcResult(
    val days: Int = 0,
    val weeks: Int = 0,
    val months: Int = 0,
    val years: Int = 0,
    val remainingDays: Int = 0,
    val error: String = ""
)

object DateCalculator {

    val MONTHS_RU = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )

    val YEARS = (1982..2045).toList()

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    fun isLeapYear(year: Int): Boolean {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        return cal.getActualMaximum(Calendar.DAY_OF_YEAR) == 366
    }

    fun daysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun validDaysForMonth(year: Int, month: Int): List<Int> {
        return (1..daysInMonth(year, month)).toList()
    }

    private fun safeCalendar(day: Int, month: Int, year: Int): Calendar {
        return Calendar.getInstance().apply {
            clearTime()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day.coerceAtMost(daysInMonth(year, month)))
        }
    }

    fun calculate(
        startDay: Int, startMonth: Int, startYear: Int,
        endDay: Int, endMonth: Int, endYear: Int,
        includeStart: Boolean, includeEnd: Boolean
    ): DateCalcResult {
        val sDay = startDay.coerceAtMost(daysInMonth(startYear, startMonth))
        val eDay = endDay.coerceAtMost(daysInMonth(endYear, endMonth))

        val startCal = safeCalendar(sDay, startMonth, startYear)
        val endCal = safeCalendar(eDay, endMonth, endYear)

        if (startCal.after(endCal)) {
            return DateCalcResult(error = "Дата начала не может быть позже даты окончания.")
        }

        var adjustedStart = startCal.clone() as Calendar
        var adjustedEnd = endCal.clone() as Calendar

        if (!includeStart) {
            adjustedStart.add(Calendar.DAY_OF_MONTH, 1)
        }

        if (includeEnd) {
            adjustedEnd.add(Calendar.DAY_OF_MONTH, 1)
        }

        val diffMs = adjustedEnd.timeInMillis - adjustedStart.timeInMillis
        val totalDays = maxOf(0, (diffMs / (1000 * 60 * 60 * 24)).toInt())

        var yearDiff = adjustedEnd.get(Calendar.YEAR) - adjustedStart.get(Calendar.YEAR)
        val checkYear = adjustedStart.clone() as Calendar
        checkYear.add(Calendar.YEAR, yearDiff)
        if (checkYear.after(adjustedEnd)) yearDiff--
        yearDiff = maxOf(0, yearDiff)

        var monthDiff = 0
        var tempCal = adjustedStart.clone() as Calendar
        while (tempCal.before(adjustedEnd)) {
            tempCal.add(Calendar.MONTH, 1)
            if (!tempCal.after(adjustedEnd)) {
                monthDiff++
            } else {
                break
            }
        }

        val afterMonths = adjustedStart.clone() as Calendar
        afterMonths.add(Calendar.MONTH, monthDiff)

        val remainMs = adjustedEnd.timeInMillis - afterMonths.timeInMillis
        val remainDays = maxOf(0, (remainMs / (1000 * 60 * 60 * 24)).toInt())

        return DateCalcResult(
            days = totalDays,
            weeks = totalDays / 7,
            months = monthDiff,
            years = yearDiff,
            remainingDays = remainDays
        )
    }

    fun resultDescription(includeStart: Boolean, includeEnd: Boolean): String {
        return when {
            includeStart && includeEnd -> "Учитываются обе выбранные даты"
            includeStart && !includeEnd -> "Учитывается только начальная дата"
            !includeStart && includeEnd -> "Учитывается только конечная дата"
            else -> "Обе даты исключены из расчета"
        }
    }
}
