package com.vision.swimsafe.ui.model

data class HomeUiState(
    val statusTitle: String,
    val venueName: String,
    val currentTime: String,
    val todayAlarmCount: Int,
    val online: Boolean,
    val outOfFence: Boolean,
    val networkWarning: String?,
    val activeAlarm: AlarmBrief?,
    val isOffDuty: Boolean = false,
)

data class AlarmBrief(
    val id: String,
    val type: String,
    val cameraName: String,
    val locationDescription: String,
    val emergencyContact: String,
    val time: String,
    val status: String,
)

data class AlarmDetailUiState(
    val alarm: AlarmBrief,
    val detectionResult: String,
    val videoStatusText: String,
    val videoStreamUrl: String?,
    val cameraId: Long?,
    val handlingTimeline: List<String>,
)

data class LocationRecord(
    val time: String,
    val status: String,
    val coordinateText: String,
)

data class LocationUiState(
    val reportStatus: String,
    val lastReportTime: String,
    val signalStrength: String,
    val coordinateText: String,
    val outOfFence: Boolean,
    val records: List<LocationRecord>,
)

data class AlarmRecordItem(
    val id: String,
    val type: String,
    val cameraName: String,
    val time: String,
    val status: String,
)

data class AlarmRecordUiState(
    val selectedTimeFilter: String,
    val selectedStatusFilter: String,
    val records: List<AlarmRecordItem>,
)

data class AlarmCenterUiState(
    val highlightedAlarm: AlarmBrief?,
    val records: List<AlarmRecordItem>,
)

data class ProfileMenuItemModel(
    val title: String,
    val accent: Boolean = false,
)

data class ProfileUiState(
    val name: String,
    val account: String,
    val venueName: String,
    val networkStatus: String,
    val tokenExpireText: String,
    val menuItems: List<ProfileMenuItemModel>,
)
