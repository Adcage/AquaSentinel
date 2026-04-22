package com.vision.swimsafe.data.remote

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.vision.swimsafe.ui.model.AlarmRecordItem
import com.vision.swimsafe.ui.model.AlarmCenterUiState
import kotlinx.coroutines.flow.Flow
import com.vision.swimsafe.ui.model.AlarmDetailUiState
import com.vision.swimsafe.ui.model.AlarmRecordUiState
import com.vision.swimsafe.ui.model.HomeUiState
import com.vision.swimsafe.ui.model.LocationUiState
import com.vision.swimsafe.ui.model.ProfileMenuItemModel
import com.vision.swimsafe.ui.model.ProfileUiState

private fun fallbackHomeState(): HomeUiState = HomeUiState(
    statusTitle = "在岗中",
    venueName = "未知场馆",
    currentTime = "--",
    todayAlarmCount = 0,
    online = false,
    outOfFence = false,
    networkWarning = null,
    activeAlarm = null,
)

object RemoteAuthRepository {
    private val service: ApiService = ApiClient.service

    suspend fun login(username: String, password: String): Result<Unit> = runCatching {
        val result = service.lifeguardLogin(LoginRequest(username = username, password = password))
            .requireData("登录失败")
        AuthSession.updateFromLogin(result)
    }.recoverCatching { throw Exception(it.parseBackendMessage("登录失败")) }

    suspend fun logout() {
        val refreshToken = AuthSession.getRefreshToken()
        runCatching {
            service.logout(LogoutRequest(refreshToken = refreshToken)).requireData("退出失败")
        }
        AuthSession.clear()
    }
}

object RemoteAlarmRepository {
    private val service: ApiService = ApiClient.service

    suspend fun submitAlarmAction(
        alarmId: String,
        action: RemoteAlarmAction,
        note: String?,
    ): Result<AlarmDetailUiState> {
        val parsedId = alarmId.toLongOrNull()
            ?: return Result.failure(IllegalArgumentException("报警ID无效"))
        return runCatching {
            val currentLifeguardId = fetchCurrentLifeguard(service)?.id
            val request = RemoteMapper.buildAlertActionRequest(
                alarmId = parsedId,
                action = action,
                note = note,
                currentLifeguardId = currentLifeguardId,
            )
            service.submitAlertAction(request).requireData("提交报警处置失败")
            val latest = service.getAlertById(parsedId).requireData("刷新报警详情失败")
            RemoteMapper.toAlarmDetailUiState(latest)
        }.recoverCatching { throw Exception(it.parseBackendMessage("提交报警处置失败")) }
    }

    suspend fun getAlarmCenterUiState(): AlarmCenterUiState {
        return runCatching {
            val records = service.listAlerts(AlertListRequest(current = 1, pageSize = 30))
                .requireData("加载报警失败")
                .records
                .map(RemoteMapper::toAlarmRecordItem)
            AlarmCenterUiState(
                highlightedAlarm = records.firstOrNull()?.let {
                    com.vision.swimsafe.ui.model.AlarmBrief(
                        id = it.id,
                        type = it.type,
                        cameraName = it.cameraName,
                        locationDescription = "请立即前往现场确认",
                        emergencyContact = "暂无联系人",
                        time = it.time,
                        status = it.status,
                    )
                },
                records = records,
            )
        }.getOrElse {
            AlarmCenterUiState(highlightedAlarm = null, records = emptyList())
        }
    }

    suspend fun getAlarmDetailUiState(alarmId: String): AlarmDetailUiState? {
        val parsedId = alarmId.toLongOrNull() ?: return null
        return runCatching {
            val detail = service.getAlertById(parsedId).requireData("加载报警详情失败")
            RemoteMapper.toAlarmDetailUiState(detail)
        }.getOrNull()
    }

    suspend fun getAlarmRecordUiState(): AlarmRecordUiState {
        return runCatching {
            val records = service.listAlerts(AlertListRequest(current = 1, pageSize = 100))
                .requireData("加载报警记录失败")
                .records
                .map(RemoteMapper::toAlarmRecordItem)
            AlarmRecordUiState(
                selectedTimeFilter = "今日",
                selectedStatusFilter = "全部",
                records = records,
            )
        }.getOrElse {
            AlarmRecordUiState("今日", "全部", emptyList())
        }
    }

    fun getAlarmCenterPagingData(): Flow<PagingData<AlarmRecordItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5,
            ),
            pagingSourceFactory = { AlarmPagingSource(service) }
        ).flow
    }

    fun getAlarmRecordPagingData(
        alertStatus: String? = null,
        keyword: String? = null,
    ): Flow<PagingData<AlarmRecordItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5,
            ),
            pagingSourceFactory = { AlarmPagingSource(service, alertStatus, keyword) }
        ).flow
    }
}

object RemoteHomeRepository {
    private val service: ApiService = ApiClient.service

    fun shouldShowDialogOnHomeLoad(activeAlarmId: String?): Boolean {
        return false
    }

    suspend fun getHomeUiState(): HomeUiState {
        return runCatching {
            val todayStats = service.getTodayAlertStats()
                .requireData("加载今日报警统计失败")
            val pageData = service.listAlerts(AlertListRequest(current = 1, pageSize = 20))
                .requireData("加载首页数据失败")
            val currentLifeguard = fetchCurrentLifeguard(service)
            val dutyStatus = currentLifeguard?.dutyStatus
            HomeUiState(
                statusTitle = RemoteMapper.dutyStatusToText(dutyStatus),
                venueName = "场馆ID ${currentLifeguard?.venueId ?: "--"}",
                currentTime = RemoteMapper.prettyTime(pageData.records.firstOrNull()?.createdAt),
                todayAlarmCount = todayStats.count?.toInt() ?: 0,
                online = true,
                outOfFence = "OUT_OF_FENCE".equals(dutyStatus, ignoreCase = true),
                networkWarning = null,
                activeAlarm = null,
                isOffDuty = "OFF_DUTY".equals(dutyStatus, ignoreCase = true) || "LEAVE".equals(dutyStatus, ignoreCase = true),
            )
        }.getOrElse { fallbackHomeState() }
    }

    suspend fun submitLeaveReport(leaveReason: String, plannedReturnMinutes: Int? = null): Result<Unit> {
        return runCatching {
            val currentLifeguard = fetchCurrentLifeguard(service)
                ?: throw IllegalStateException("未找到救生员档案")
            val lifeguardId = currentLifeguard.id
                ?: throw IllegalStateException("救生员ID缺失")
            val plannedReturnAt = plannedReturnMinutes?.let {
                java.time.Instant.now().plusSeconds(it * 60L).toString()
            }
            val request = LeaveReportRequest(
                lifeguardId = lifeguardId,
                leaveReason = leaveReason,
                plannedReturnAt = plannedReturnAt,
            )
            service.submitLeaveReport(request).requireData("提交离岗报备失败")
            Unit
        }.recoverCatching { throw Exception(it.parseBackendMessage("提交离岗报备失败")) }
    }

    suspend fun returnToDuty(): Result<Unit> {
        return runCatching {
            val currentLifeguard = fetchCurrentLifeguard(service)
                ?: throw IllegalStateException("未找到救生员档案")
            val lifeguardId = currentLifeguard.id
                ?: throw IllegalStateException("救生员ID缺失")
            val request = DutyStatusUpdateRequest(
                lifeguardId = lifeguardId,
                dutyStatus = "ON_DUTY",
            )
            service.updateDutyStatus(request).requireData("回岗失败")
            Unit
        }.recoverCatching { throw Exception(it.parseBackendMessage("回岗失败")) }
    }
}

object RemoteCameraRepository {
    private val service: ApiService = ApiClient.service

    suspend fun getCameraList(): List<CameraDeviceVo> {
        return runCatching {
            val pageData = service.listCameraDevices(CameraDeviceQueryRequest(current = 1, pageSize = 100))
                .requireData("加载摄像头列表失败")
            pageData.records
        }.getOrElse { emptyList() }
    }
}

object RemoteLocationRepository {
    private val service: ApiService = ApiClient.service

    suspend fun reportCurrentLocation(latitude: Double, longitude: Double): Result<Unit> {
        return runCatching {
            val currentLifeguard = fetchCurrentLifeguard(service)
                ?: throw IllegalStateException("未找到救生员档案")
            val request = LifeguardLocationReportRequest(
                lifeguardId = currentLifeguard.id ?: throw IllegalStateException("救生员ID缺失"),
                venueId = currentLifeguard.venueId,
                latitude = latitude,
                longitude = longitude,
            )
            service.reportLocation(request).requireData("定位上报失败")
            Unit
        }.recoverCatching { throw Exception(it.parseBackendMessage("定位上报失败")) }
    }

    suspend fun getLocationUiState(): LocationUiState {
        return runCatching {
            val currentLifeguard = fetchCurrentLifeguard(service)
            if (currentLifeguard?.id == null) {
                return@runCatching LocationUiState(
                    reportStatus = "未登录",
                    lastReportTime = "--",
                    signalStrength = "未知",
                    coordinateText = "--",
                    outOfFence = false,
                    records = emptyList(),
                )
            }
            val records = service.recentLocations(currentLifeguard.id, 10)
                .requireData("加载定位记录失败")
                .map(RemoteMapper::toLocationRecord)
            val latest = records.firstOrNull()
            LocationUiState(
                reportStatus = "上报中",
                lastReportTime = latest?.time ?: "--",
                signalStrength = "正常",
                coordinateText = latest?.coordinateText ?: "--",
                outOfFence = "OUT_OF_FENCE".equals(currentLifeguard.dutyStatus, ignoreCase = true),
                records = records,
            )
        }.getOrElse {
            LocationUiState(
                reportStatus = "异常",
                lastReportTime = "--",
                signalStrength = "弱",
                coordinateText = "--",
                outOfFence = false,
                records = emptyList(),
            )
        }
    }
}

object RemoteProfileRepository {
    private val service: ApiService = ApiClient.service

    suspend fun getProfileUiState(): ProfileUiState {
        return runCatching {
            val user = AuthSession.getUserInfo()
            val lifeguard = fetchCurrentLifeguard(service)
            ProfileUiState(
                name = user?.displayName ?: lifeguard?.fullName ?: "未登录",
                account = user?.username ?: "--",
                venueName = "场馆ID ${lifeguard?.venueId ?: "--"}",
                networkStatus = "网络可用",
                tokenExpireText = if (AuthSession.getAccessToken().isNullOrBlank()) "未登录" else "登录状态有效",
                menuItems = listOf(
                    ProfileMenuItemModel("个人信息"),
                    ProfileMenuItemModel("修改密码"),
                    ProfileMenuItemModel("关于系统"),
                    ProfileMenuItemModel("退出登录", accent = true),
                ),
            )
        }.getOrElse {
            ProfileUiState(
                name = "未登录",
                account = "--",
                venueName = "--",
                networkStatus = "网络不可用",
                tokenExpireText = "登录状态未知",
                menuItems = listOf(ProfileMenuItemModel("退出登录", accent = true)),
            )
        }
    }
}

private suspend fun fetchCurrentLifeguard(service: ApiService): LifeguardVo? {
    val userId = AuthSession.getUserInfo()?.id ?: return null
    return runCatching {
        service.listLifeguards(LifeguardQueryRequest(current = 1, pageSize = 1, userId = userId))
            .requireData("加载救生员档案失败")
            .records
            .firstOrNull()
    }.getOrNull()
}
