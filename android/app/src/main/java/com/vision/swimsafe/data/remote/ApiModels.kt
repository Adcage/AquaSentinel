package com.vision.swimsafe.data.remote

data class ApiResponse<T>(
    val code: Int? = null,
    val data: T? = null,
    val message: String? = null,
)

data class PageData<T>(
    val records: List<T> = emptyList(),
    val total: Long = 0,
)

data class LoginRequest(
    val username: String,
    val password: String,
    val deviceId: String? = null,
    val clientType: String = "ANDROID",
    val clientVersion: String = "1.0.0",
)

data class LogoutRequest(
    val deviceId: String? = null,
    val refreshToken: String? = null,
)

data class AlertListRequest(
    val current: Int = 1,
    val pageSize: Int = 20,
    val alertStatus: String? = null,
    val keyword: String? = null,
    val sortField: String = "created_at",
    val sortOrder: String = "descend",
)

data class AlertActionRequest(
    val alertId: Long,
    val actionType: String,
    val actionNote: String? = null,
    val assigneeLifeguardId: Long? = null,
)

data class LifeguardQueryRequest(
    val current: Int = 1,
    val pageSize: Int = 20,
    val userId: Long? = null,
)

data class UserInfoVo(
    val id: Long? = null,
    val username: String? = null,
    val displayName: String? = null,
    val roles: List<String> = emptyList(),
)

data class LoginResultVo(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val user: UserInfoVo? = null,
)

data class AlertRecordVo(
    val id: Long? = null,
    val alertUid: String? = null,
    val cameraId: Long? = null,
    val lifeguardId: Long? = null,
    val alertType: String? = null,
    val alertStatus: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val incidentLocation: String? = null,
    val videoStreamUrl: String? = null,
    val detectionResult: String? = null,
    val createdAt: String? = null,
)

data class LifeguardVo(
    val id: Long? = null,
    val userId: Long? = null,
    val fullName: String? = null,
    val dutyStatus: String? = null,
    val venueId: Long? = null,
)

data class LifeguardLocationLogVo(
    val id: Long? = null,
    val lifeguardId: Long? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val inFence: Int? = null,
    val reportSource: String? = null,
    val reportedAt: String? = null,
)

data class LifeguardLocationReportRequest(
    val lifeguardId: Long,
    val venueId: Long? = null,
    val longitude: Double,
    val latitude: Double,
    val reportSource: String = "ANDROID_APP",
    val reportedAt: String? = null,
)

data class CameraDeviceQueryRequest(
    val current: Int = 1,
    val pageSize: Int = 20,
    val venueId: Long? = null,
    val sortField: String = "created_at",
    val sortOrder: String = "descend",
)

data class CameraDeviceVo(
    val id: Long? = null,
    val venueId: Long? = null,
    val zoneId: Long? = null,
    val cameraCode: String? = null,
    val cameraName: String? = null,
    val streamUrl: String? = null,
    val protocol: String? = null,
    val deviceStatus: String? = null,
    val healthStatus: String? = null,
    val enabled: Int? = null,
)

data class LeaveReportRequest(
    val lifeguardId: Long,
    val leaveReason: String? = null,
    val plannedReturnAt: String? = null,
)

data class DutyStatusUpdateRequest(
    val lifeguardId: Long,
    val dutyStatus: String,
)

data class TodayAlertStatsVo(
    val count: Long? = null,
    val date: String? = null,
)

fun <T> ApiResponse<T>.requireData(fallbackMessage: String): T {
    if (code != 0) {
        throw IllegalStateException(message ?: fallbackMessage)
    }
    return data ?: throw IllegalStateException(fallbackMessage)
}

fun Throwable.parseBackendMessage(fallback: String): String {
    if (this is retrofit2.HttpException) {
        runCatching {
            val body = response()?.errorBody()?.string()
            if (!body.isNullOrBlank()) {
                val json = com.google.gson.JsonParser.parseString(body).asJsonObject
                val msg = json.get("message")?.takeIf { !it.isJsonNull }?.asString
                if (!msg.isNullOrBlank()) return msg
            }
        }
    }
    return message ?: fallback
}
