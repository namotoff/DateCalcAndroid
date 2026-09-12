package com.datecalc.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Месяцы в Calendar нумеруются с нуля: 0 = январь, 11 = декабрь. */
private const val JAN = 0
private const val FEB = 1
private const val MAR = 2
private const val APR = 3
private const val DEC = 11

class DateCalculatorTest {

    private fun calc(
        sd: Int, sm: Int, sy: Int, ed: Int, em: Int, ey: Int,
        includeStart: Boolean = true, includeEnd: Boolean = true
    ) = DateCalculator.calculate(sd, sm, sy, ed, em, ey, includeStart, includeEnd)

    @Test
    fun обычныйГодДаёт365Дней() {
        assertEquals(365, calc(1, JAN, 2026, 31, DEC, 2026).days)
    }

    @Test
    fun високосныйГодДаёт366Дней() {
        assertEquals(366, calc(1, JAN, 2024, 31, DEC, 2024).days)
    }

    @Test
    fun февральВисокосногоГодаСодержит29Дней() {
        assertEquals(29, DateCalculator.daysInMonth(2024, FEB))
        assertEquals(28, DateCalculator.daysInMonth(2025, FEB))
        assertTrue(DateCalculator.isLeapYear(2024))
        assertTrue(!DateCalculator.isLeapYear(2025))
    }

    @Test
    fun столетниеГодыНеВисокосныеКроме2000() {
        assertTrue(!DateCalculator.isLeapYear(1900))
        assertTrue(DateCalculator.isLeapYear(2000))
    }

    @Test
    fun переходЧерез29ФевраляУчитываетЛишнийДень() {
        assertEquals(2, calc(28, FEB, 2024, 1, MAR, 2024, includeEnd = false).days)
        assertEquals(1, calc(28, FEB, 2025, 1, MAR, 2025, includeEnd = false).days)
    }

    @Test
    fun однаИТаЖеДатаСОбеимиГраницамиДаётОдинДень() {
        assertEquals(1, calc(12, MAR, 2026, 12, MAR, 2026).days)
    }

    @Test
    fun однаИТаЖеДатаБезГраницДаётНоль() {
        assertEquals(0, calc(12, MAR, 2026, 12, MAR, 2026, includeStart = false, includeEnd = false).days)
    }

    @Test
    fun датаНачалаПозжеКонцаДаётОшибку() {
        val r = calc(2, JAN, 2026, 1, JAN, 2026)
        assertTrue("ожидалась ошибка, получено: $r", r.error.isNotEmpty())
    }

    @Test
    fun несуществующее31АпреляПриводитсяК30() {
        assertEquals(30, DateCalculator.daysInMonth(2026, APR))
        // 31 апреля трактуется как 30 апреля, значит до 30 апреля включительно — один день
        assertEquals(1, calc(31, APR, 2026, 30, APR, 2026).days)
    }

    @Test
    fun неделиЭтоЦелыеСемидневки() {
        val r = calc(1, JAN, 2026, 14, JAN, 2026)
        assertEquals(14, r.days)
        assertEquals(2, r.weeks)
    }

    @Test
    fun целыеМесяцыИОстаток() {
        // 1 января -> 15 марта, обе границы исключены: ровно 2 месяца и 14 дней
        val r = calc(1, JAN, 2026, 15, MAR, 2026, includeStart = true, includeEnd = false)
        assertEquals(2, r.months)
        assertEquals(14, r.remainingDays)
    }

    @Test
    fun годыСчитаютсяПолными() {
        assertEquals(1, calc(1, JAN, 2025, 1, JAN, 2026, includeEnd = false).years)
        assertEquals(0, calc(1, JAN, 2025, 31, DEC, 2025, includeEnd = false).years)
    }

    @Test
    fun весьДиапазонПриложенияСчитаетсяБезСбоев() {
        val r = calc(1, JAN, 1982, 31, DEC, 2045)
        assertEquals(23376, r.days)
        assertTrue(r.error.isEmpty())
    }
}
