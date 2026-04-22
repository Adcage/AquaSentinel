package com.vision.swimsafe.data.remote

import com.vision.swimsafe.ui.model.AlarmBrief
import com.vision.swimsafe.ui.model.AlarmDetailUiState
import com.vision.swimsafe.ui.model.AlarmRecordItem
import com.vision.swimsafe.ui.model.LocationRecord

object RemoteMapper {

    fun actionToBackend(action: RemoteAlarmAction): String = when (action) {
        RemoteAlarmAction.DISPATCH -> "ASSIGN"
        RemoteAlarmAction.ACKNOWLEDGED -> "CONFIRM"
        RemoteAlarmAction.RESOLVED -> "DONE"
        RemoteAlarmAction.FALSE_ALARM -> "FALSE_ALARM"
    }

    fun parseCoordinateText(value: String?): Pair<Double, Double>? {
        if (value.isNullOrBlank()) {
            return null
        }
        val parts = value.split(",")
        if (parts.size != 2) {
            return null
        }
        val latitude = parts[0].trim().toDoubleOrNull() ?: return null
        val longitude = parts[1].trim().toDoubleOrNull() ?: return null
        return latitude to longitude
    }

    fun buildAlertActionRequest(
        alarmId: Long,
        action: RemoteAlarmAction,
        note: String?,
        currentLifeguardId: Long?,
    ): AlertActionRequest {
        val backendAction = actionToBackend(action)
        if (action == RemoteAlarmAction.DISPATCH && currentLifeguardId == null) {
            throw IllegalArgumentException("确认出警需要当前救生员ID")
        }
        return AlertActionRequest(
            alertId = alarmId,
            actionType = backendAction,
            actionNote = note,
            assigneeLifeguardId = if (backendAction == "ASSIGN") currentLifeguardId else null,
        )
    }

    fun alertTypeToText(value: String?): String = when (value?.uppercase()) {
        "DROWNING" -> "溺水预警"
        "CROSS_BORDER" -> "人员越界"
        "OVER_CAPACITY" -> "超员告警"
        else -> "其他报警"
    }

    fun alertStatusToText(value: String?): String = when (value?.uppercase()) {
        "PENDING" -> "未处理"
        "ASSIGNED", "CONFIRMED" -> "处理中"
        "DONE" -> "已处理"
        "FALSE_ALARM" -> "误报"
        else -> "未知"
    }

    fun dutyStatusToText(value: String?): String = when (value?.uppercase()) {
        "ON_DUTY" -> "在岗中"
        "OFF_DUTY", "LEAVE" -> "离岗"
        "OUT_OF_FENCE" -> "围栏外"
        else -> "未知"
    }

    fun toAlarmRecordItem(vo: AlertRecordVo): AlarmRecordItem = AlarmRecordItem(
        id = (vo.id ?: 0L).toString(),
        type = alertTypeToText(vo.alertType),
        cameraName = "摄像头#${vo.cameraId ?: "--"}",
        time = prettyTime(vo.createdAt),
        status = alertStatusToText(vo.alertStatus),
    )

    fun toAlarmBrief(vo: AlertRecordVo): AlarmBrief = AlarmBrief(
        id = (vo.id ?: 0L).toString(),
        type = alertTypeToText(vo.alertType),
        cameraName = "摄像头#${vo.cameraId ?: "--"}",
        locationDescription = vo.incidentLocation.orEmpty().ifBlank { "系统未返回具体位置" },
        emergencyContact = listOf(vo.emergencyContactName, vo.emergencyContactPhone)
            .filter { !it.isNullOrBlank() }
            .joinToString(" ")
            .ifBlank { "暂无联系人" },
        time = prettyTime(vo.createdAt),
        status = alertStatusToText(vo.alertStatus),
    )

    fun toAlarmDetailUiState(vo: AlertRecordVo): AlarmDetailUiState = AlarmDetailUiState(
        alarm = toAlarmBrief(vo),
        detectionResult = vo.detectionResult.orEmpty().ifBlank { "AI检测到异常行为，请及时查看视频流确认现场情况" },
        videoStatusText = if (vo.cameraId != null) "视频流已接入" else "暂无视频流地址",
        videoStreamUrl = vo.videoStreamUrl,
        cameraId = vo.cameraId,
        handlingTimeline = listOf(
            "触发时间：${prettyTime(vo.createdAt)}",
            "当前状态：${alertStatusToText(vo.alertStatus)}",
            "类型：${alertTypeToText(vo.alertType)}",
        ),
    )

    fun toLocationRecord(vo: LifeguardLocationLogVo): LocationRecord = LocationRecord(
        time = prettyTime(vo.reportedAt),
        status = if (vo.inFence == 1) "成功" else "围栏外",
        coordinateText = "${vo.latitude ?: 0.0}, ${vo.longitude ?: 0.0}",
    )

    fun prettyTime(value: String?): String {
        if (value.isNullOrBlank()) {
            return "--"
        }
        return try {
            val instant = java.time.Instant.parse(value)
            val localDateTime = instant.atZone(java.time.ZoneId.of("Asia/Shanghai")).toLocalDateTime()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            localDateTime.format(formatter)
        } catch (e: Exception) {
            value.replace("T", " ").replace("Z", "")
        }
    }
}

enum class RemoteAlarmAction {
    DISPATCH,
    ACKNOWLEDGED,
    RESOLVED,
    FALSE_ALARM,
}
