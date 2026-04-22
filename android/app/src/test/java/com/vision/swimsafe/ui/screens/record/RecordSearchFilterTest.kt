package com.vision.swimsafe.ui.screens.record

import com.vision.swimsafe.ui.model.AlarmRecordItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class RecordSearchFilterTest {

    private val records = listOf(
        AlarmRecordItem(
            id = "ALARM-20260321-001",
            type = "溺水预警",
            cameraName = "泳池东区 2 号机位",
            time = "2026-03-21 14:32",
            status = "未处理",
        ),
        AlarmRecordItem(
            id = "ALARM-20260322-002",
            type = "人员越界",
            cameraName = "泳池西区 1 号机位",
            time = "2026-03-22 11:08",
            status = "处理中",
        ),
        AlarmRecordItem(
            id = "ALARM-20260322-003",
            type = "溺水预警",
            cameraName = "泳池东区 3 号机位",
            time = "2026-03-22 09:15",
            status = "已处理",
        ),
    )

    @Test
    fun filter_shouldReturnAllWhenDateAndCameraQueryAreBlank() {
        val filtered = filterAlarmRecords(records, dateQuery = "", cameraQuery = "")

        assertEquals(records, filtered)
    }

    @Test
    fun filter_shouldMatchDatePrefix() {
        val filtered = filterAlarmRecords(records, dateQuery = "2026-03-22", cameraQuery = "")

        assertEquals(listOf("ALARM-20260322-002", "ALARM-20260322-003"), filtered.map { it.id })
    }

    @Test
    fun filter_shouldMatchCameraNameWithTrimAndCaseInsensitive() {
        val filtered = filterAlarmRecords(records, dateQuery = "", cameraQuery = "  3号机位  ")

        assertEquals(listOf("ALARM-20260322-003"), filtered.map { it.id })
    }

    @Test
    fun filter_shouldApplyDateAndCameraQueryTogether() {
        val filtered = filterAlarmRecords(records, dateQuery = "2026-03-22", cameraQuery = "东区")

        assertEquals(listOf("ALARM-20260322-003"), filtered.map { it.id })
    }

    @Test
    fun formatDateFromPickerUtcMillis_shouldReturnBlankWhenMillisIsNull() {
        val formattedDate = formatDateFromPickerUtcMillis(null)

        assertEquals("", formattedDate)
    }

    @Test
    fun formatDateFromPickerUtcMillis_shouldConvertToDateString() {
        val formattedDate = formatDateFromPickerUtcMillis(1767139200000L)

        assertEquals("2025-12-31", formattedDate)
    }

    @Test
    fun buildCalendarMonthTitle_shouldContainChineseYearAndMonth() {
        val title = buildCalendarMonthTitle(YearMonth.of(2029, 3))

        assertEquals("2029年3月", title)
    }

    @Test
    fun chineseWeekdayLabels_shouldStartFromMondayAndUseChineseText() {
        val labels = chineseWeekdayLabels()

        assertEquals(listOf("一", "二", "三", "四", "五", "六", "日"), labels)
    }

    @Test
    fun buildMonthDayGrid_shouldPlaceFirstDayInSundayColumnForMarch2026() {
        val grid = buildMonthDayGrid(YearMonth.of(2026, 3))

        assertEquals(6, grid.size)
        assertEquals(listOf(null, null, null, null, null, null, 1), grid.first())
    }
}
