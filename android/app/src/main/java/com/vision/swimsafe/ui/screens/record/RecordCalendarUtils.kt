package com.vision.swimsafe.ui.screens.record

import java.time.YearMonth

fun buildCalendarMonthTitle(yearMonth: YearMonth): String {
    return "${yearMonth.year}年${yearMonth.monthValue}月"
}

fun chineseWeekdayLabels(): List<String> {
    return listOf("一", "二", "三", "四", "五", "六", "日")
}

fun buildMonthDayGrid(yearMonth: YearMonth): List<List<Int?>> {
    val firstDayOffset = yearMonth.atDay(1).dayOfWeek.value - 1
    val daysInMonth = yearMonth.lengthOfMonth()
    val cells = mutableListOf<Int?>()

    repeat(firstDayOffset) {
        cells += null
    }
    for (day in 1..daysInMonth) {
        cells += day
    }
    while (cells.size % 7 != 0) {
        cells += null
    }
    while (cells.size < 42) {
        cells += null
    }

    return cells.chunked(7)
}
