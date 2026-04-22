package com.vision.swimsafe.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("lifeguards/login")
    suspend fun lifeguardLogin(@Body body: LoginRequest): ApiResponse<LoginResultVo>

    @POST("auth/logout")
    suspend fun logout(@Body body: LogoutRequest): ApiResponse<Boolean>

    @POST("alerts/list/page")
    suspend fun listAlerts(@Body body: AlertListRequest): ApiResponse<PageData<AlertRecordVo>>

    @GET("alerts/{id}")
    suspend fun getAlertById(@retrofit2.http.Path("id") id: Long): ApiResponse<AlertRecordVo>

    @POST("alerts/action")
    suspend fun submitAlertAction(@Body body: AlertActionRequest): ApiResponse<Map<String, Any>>

    @POST("lifeguards/list/page/vo")
    suspend fun listLifeguards(@Body body: LifeguardQueryRequest): ApiResponse<PageData<LifeguardVo>>

    @GET("lifeguards/location/recent")
    suspend fun recentLocations(
        @Query("lifeguardId") lifeguardId: Long,
        @Query("limit") limit: Int = 10,
    ): ApiResponse<List<LifeguardLocationLogVo>>

    @POST("lifeguards/location/report")
    suspend fun reportLocation(@Body body: LifeguardLocationReportRequest): ApiResponse<Map<String, Any>>

    @POST("cameras/list/page/vo")
    suspend fun listCameraDevices(@Body body: CameraDeviceQueryRequest): ApiResponse<PageData<CameraDeviceVo>>

    @POST("lifeguards/leave-report")
    suspend fun submitLeaveReport(@Body body: LeaveReportRequest): ApiResponse<Map<String, Any>>

    @POST("lifeguards/duty/update")
    suspend fun updateDutyStatus(@Body body: DutyStatusUpdateRequest): ApiResponse<Map<String, Any>>

    @GET("alerts/stats/today")
    suspend fun getTodayAlertStats(): ApiResponse<TodayAlertStatsVo>
}
