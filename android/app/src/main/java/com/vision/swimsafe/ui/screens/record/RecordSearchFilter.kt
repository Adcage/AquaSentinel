package com.vision.swimsafe.ui.screens.record

import com.vision.swimsafe.ui.model.AlarmRecordItem
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

const val ALL_DATE_OPTION = "全部日期"
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun filterAlarmRecords(
    records: List<AlarmRecordItem>,
    dateQuery: String,
    cameraQuery: String,
): List<AlarmRecordItem> {
    val normalizedDate = dateQuery.trim()
    val normalizedCamera = normalizeCameraKeyword(cameraQuery)

    return records.filter { record ->
        val matchDate = normalizedDate.isBlank() || record.time.startsWith(normalizedDate)
        val matchCamera = normalizedCamera.isBlank() || normalizeCameraKeyword(record.cameraName).contains(normalizedCamera)
        matchDate && matchCamera
    }
}

fun formatDateFromPickerUtcMillis(selectedDateMillis: Long?): String {
    if (selectedDateMillis == null) {
        return ""
    }
    val localDate = Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return localDate.format(dateFormatter)
}

private fun normalizeCameraKeyword(input: String): String {
    return input.lowercase().replace(" ", "").trim()
}
