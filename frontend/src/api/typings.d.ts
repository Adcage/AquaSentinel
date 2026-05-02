declare namespace API {
  type actionsParams = {
    id: number;
  };

  type addCameraMaintenanceLogByCameraParams = {
    cameraId: number;
  };

  type AdminLoginRequest = {
    username?: string;
    password?: string;
    captchaId?: string;
    captchaCode?: string;
    deviceId?: string;
    clientType?: string;
    clientVersion?: string;
  };

  type AiStreamTask = {
    id?: number;
    task_code?: string;
    camera_id?: number;
    stream_url?: string;
    model_version?: string;
    frame_interval_ms?: number;
    callback_url?: string;
    task_status?: string;
    started_at?: string;
    stopped_at?: string;
    last_frame_at?: string;
    created_at?: string;
    updated_at?: string;
  };

  type AiStreamTaskAddRequest = {
    taskCode?: string;
    cameraId?: number;
    streamUrl?: string;
    modelVersion?: string;
    frameIntervalMs?: number;
    callbackUrl?: string;
    taskStatus?: string;
  };

  type AiStreamTaskEditRequest = {
    id?: number;
    streamUrl?: string;
    modelVersion?: string;
    frameIntervalMs?: number;
    callbackUrl?: string;
    taskStatus?: string;
  };

  type AiStreamTaskQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    taskCode?: string;
    cameraId?: number;
    taskStatus?: string;
    modelVersion?: string;
  };

  type AiStreamTaskUpdateRequest = {
    id?: number;
    taskCode?: string;
    cameraId?: number;
    streamUrl?: string;
    modelVersion?: string;
    frameIntervalMs?: number;
    callbackUrl?: string;
    taskStatus?: string;
  };

  type AiStreamTaskVO = {
    id?: number;
    taskCode?: string;
    cameraId?: number;
    streamUrl?: string;
    modelVersion?: string;
    frameIntervalMs?: number;
    callbackUrl?: string;
    taskStatus?: string;
    startedAt?: string;
    stoppedAt?: string;
    lastFrameAt?: string;
    createdAt?: string;
    updatedAt?: string;
  };

  type AlertActionRequest = {
    alertId?: number;
    actionType?: string;
    actionNote?: string;
    assigneeLifeguardId?: number;
  };

  type AlertBatchActionRequest = {
    alertIds?: number[];
    actionType?: string;
    actionNote?: string;
    assigneeLifeguardId?: number;
  };

  type AlertDisposal = {
    id?: number;
    alert_id?: number;
    operator_user_id?: number;
    operator_role?: string;
    action_type?: string;
    action_note?: string;
    action_time?: string;
  };

  type AlertDisposalAddRequest = {
    alertId?: number;
    operatorUserId?: number;
    operatorRole?: string;
    actionType?: string;
    actionNote?: string;
    actionTime?: string;
  };

  type AlertDisposalEditRequest = {
    id?: number;
    actionType?: string;
    actionNote?: string;
    actionTime?: string;
  };

  type AlertDisposalQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    alertId?: number;
    operatorUserId?: number;
    operatorRole?: string;
    actionType?: string;
    startActionTime?: string;
    endActionTime?: string;
  };

  type AlertDisposalUpdateRequest = {
    id?: number;
    alertId?: number;
    operatorUserId?: number;
    operatorRole?: string;
    actionType?: string;
    actionNote?: string;
    actionTime?: string;
  };

  type AlertDisposalVO = {
    id?: number;
    alertId?: number;
    operatorUserId?: number;
    operatorRole?: string;
    actionType?: string;
    actionNote?: string;
    actionTime?: string;
  };

  type AlertRecord = {
    id?: number;
    alert_uid?: string;
    event_id?: number;
    camera_id?: number;
    venue_id?: number;
    lifeguard_id?: number;
    alert_type?: string;
    alert_status?: string;
    emergency_contact_name?: string;
    emergency_contact_phone?: string;
    incident_location?: string;
    video_stream_url?: string;
    pushed_to_app?: number;
    pushed_to_pc?: number;
    first_push_time?: string;
    resolved_time?: string;
    created_at?: string;
    updated_at?: string;
  };

  type AlertRecordAddRequest = {
    alertUid?: string;
    eventId?: number;
    cameraId?: number;
    venueId?: number;
    lifeguardId?: number;
    alertType?: string;
    alertStatus?: string;
    emergencyContactName?: string;
    emergencyContactPhone?: string;
    incidentLocation?: string;
    videoStreamUrl?: string;
  };

  type AlertRecordEditRequest = {
    id?: number;
    lifeguardId?: number;
    alertStatus?: string;
    emergencyContactName?: string;
    emergencyContactPhone?: string;
    incidentLocation?: string;
    videoStreamUrl?: string;
  };

  type AlertRecordQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    alertUid?: string;
    eventId?: number;
    cameraId?: number;
    venueId?: number;
    lifeguardId?: number;
    alertType?: string;
    alertStatus?: string;
    startCreatedAt?: string;
    endCreatedAt?: string;
    startTime?: string;
    endTime?: string;
    keyword?: string;
  };

  type AlertRecordUpdateRequest = {
    id?: number;
    alertUid?: string;
    eventId?: number;
    cameraId?: number;
    venueId?: number;
    lifeguardId?: number;
    alertType?: string;
    alertStatus?: string;
    emergencyContactName?: string;
    emergencyContactPhone?: string;
    incidentLocation?: string;
    videoStreamUrl?: string;
    pushedToApp?: number;
    pushedToPc?: number;
    firstPushTime?: string;
    resolvedTime?: string;
  };

  type AlertRecordVO = {
    id?: number;
    alertUid?: string;
    eventId?: number;
    cameraId?: number;
    venueId?: number;
    lifeguardId?: number;
    alertType?: string;
    alertStatus?: string;
    emergencyContactName?: string;
    emergencyContactPhone?: string;
    incidentLocation?: string;
    videoStreamUrl?: string;
    pushedToApp?: number;
    pushedToPc?: number;
    firstPushTime?: string;
    resolvedTime?: string;
    createdAt?: string;
    updatedAt?: string;
  };

  type assignParams = {
    id: number;
  };

  type auditLifeguardByPathParams = {
    id: number;
  };

  type BaseResponseAiStreamTask = {
    code?: number;
    data?: AiStreamTask;
    message?: string;
    requestId?: string;
  };

  type BaseResponseAiStreamTaskVO = {
    code?: number;
    data?: AiStreamTaskVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseAlertDisposal = {
    code?: number;
    data?: AlertDisposal;
    message?: string;
    requestId?: string;
  };

  type BaseResponseAlertDisposalVO = {
    code?: number;
    data?: AlertDisposalVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseAlertRecord = {
    code?: number;
    data?: AlertRecord;
    message?: string;
    requestId?: string;
  };

  type BaseResponseAlertRecordVO = {
    code?: number;
    data?: AlertRecordVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseBoolean = {
    code?: number;
    data?: boolean;
    message?: string;
    requestId?: string;
  };

  type BaseResponseBatchOperateResultVO = {
    code?: number;
    data?: BatchOperateResultVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseCameraDevice = {
    code?: number;
    data?: CameraDevice;
    message?: string;
    requestId?: string;
  };

  type BaseResponseCameraDeviceVO = {
    code?: number;
    data?: CameraDeviceVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseCameraMaintenanceLog = {
    code?: number;
    data?: CameraMaintenanceLog;
    message?: string;
    requestId?: string;
  };

  type BaseResponseCameraMaintenanceLogVO = {
    code?: number;
    data?: CameraMaintenanceLogVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseCaptchaVO = {
    code?: number;
    data?: CaptchaVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseLifeguard = {
    code?: number;
    data?: Lifeguard;
    message?: string;
    requestId?: string;
  };

  type BaseResponseLifeguardDutyLog = {
    code?: number;
    data?: LifeguardDutyLog;
    message?: string;
    requestId?: string;
  };

  type BaseResponseLifeguardDutyLogVO = {
    code?: number;
    data?: LifeguardDutyLogVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseLifeguardLocationLog = {
    code?: number;
    data?: LifeguardLocationLog;
    message?: string;
    requestId?: string;
  };

  type BaseResponseLifeguardLocationLogVO = {
    code?: number;
    data?: LifeguardLocationLogVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseLifeguardVO = {
    code?: number;
    data?: LifeguardVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseListAiStreamTask = {
    code?: number;
    data?: AiStreamTask[];
    message?: string;
    requestId?: string;
  };

  type BaseResponseListAiStreamTaskVO = {
    code?: number;
    data?: AiStreamTaskVO[];
    message?: string;
    requestId?: string;
  };

  type BaseResponseListAlertDisposal = {
    code?: number;
    data?: AlertDisposal[];
    message?: string;
    requestId?: string;
  };

  type BaseResponseListAlertDisposalVO = {
    code?: number;
    data?: AlertDisposalVO[];
    message?: string;
    requestId?: string;
  };

  type BaseResponseListAlertRecord = {
    code?: number;
    data?: AlertRecord[];
    message?: string;
    requestId?: string;
  };

  type BaseResponseListAlertRecordVO = {
    code?: number;
    data?: AlertRecordVO[];
    message?: string;
    requestId?: string;
  };

  type BaseResponseListLifeguardLocationLogVO = {
    code?: number;
    data?: LifeguardLocationLogVO[];
    message?: string;
    requestId?: string;
  };

  type BaseResponseListMapStringObject = {
    code?: number;
    data?: Record<string, any>[];
    message?: string;
    requestId?: string;
  };

  type BaseResponseListMonitoringEvent = {
    code?: number;
    data?: MonitoringEvent[];
    message?: string;
    requestId?: string;
  };

  type BaseResponseListMonitoringEventVO = {
    code?: number;
    data?: MonitoringEventVO[];
    message?: string;
    requestId?: string;
  };

  type BaseResponseListStatsSnapshot = {
    code?: number;
    data?: StatsSnapshot[];
    message?: string;
    requestId?: string;
  };

  type BaseResponseListStatsSnapshotVO = {
    code?: number;
    data?: StatsSnapshotVO[];
    message?: string;
    requestId?: string;
  };

  type BaseResponseListSystemAuditLog = {
    code?: number;
    data?: SystemAuditLog[];
    message?: string;
    requestId?: string;
  };

  type BaseResponseListSystemAuditLogVO = {
    code?: number;
    data?: SystemAuditLogVO[];
    message?: string;
    requestId?: string;
  };

  type BaseResponseLoginResultVO = {
    code?: number;
    data?: LoginResultVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseLong = {
    code?: number;
    data?: number;
    message?: string;
    requestId?: string;
  };

  type BaseResponseMapStringObject = {
    code?: number;
    data?: Record<string, any>;
    message?: string;
    requestId?: string;
  };

  type BaseResponseMonitoringEvent = {
    code?: number;
    data?: MonitoringEvent;
    message?: string;
    requestId?: string;
  };

  type BaseResponseMonitoringEventVO = {
    code?: number;
    data?: MonitoringEventVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageAiStreamTask = {
    code?: number;
    data?: PageAiStreamTask;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageAiStreamTaskVO = {
    code?: number;
    data?: PageAiStreamTaskVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageAlertDisposal = {
    code?: number;
    data?: PageAlertDisposal;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageAlertDisposalVO = {
    code?: number;
    data?: PageAlertDisposalVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageAlertRecord = {
    code?: number;
    data?: PageAlertRecord;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageAlertRecordVO = {
    code?: number;
    data?: PageAlertRecordVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageCameraDevice = {
    code?: number;
    data?: PageCameraDevice;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageCameraDeviceVO = {
    code?: number;
    data?: PageCameraDeviceVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageCameraMaintenanceLog = {
    code?: number;
    data?: PageCameraMaintenanceLog;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageCameraMaintenanceLogVO = {
    code?: number;
    data?: PageCameraMaintenanceLogVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageLifeguard = {
    code?: number;
    data?: PageLifeguard;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageLifeguardDutyLog = {
    code?: number;
    data?: PageLifeguardDutyLog;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageLifeguardDutyLogVO = {
    code?: number;
    data?: PageLifeguardDutyLogVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageLifeguardLocationLog = {
    code?: number;
    data?: PageLifeguardLocationLog;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageLifeguardLocationLogVO = {
    code?: number;
    data?: PageLifeguardLocationLogVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageLifeguardVO = {
    code?: number;
    data?: PageLifeguardVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageMonitoringEvent = {
    code?: number;
    data?: PageMonitoringEvent;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageMonitoringEventVO = {
    code?: number;
    data?: PageMonitoringEventVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageRoleVO = {
    code?: number;
    data?: PageRoleVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageStatsSnapshot = {
    code?: number;
    data?: PageStatsSnapshot;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageStatsSnapshotVO = {
    code?: number;
    data?: PageStatsSnapshotVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageSysRole = {
    code?: number;
    data?: PageSysRole;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageSystemAuditLog = {
    code?: number;
    data?: PageSystemAuditLog;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageSystemAuditLogVO = {
    code?: number;
    data?: PageSystemAuditLogVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageSysUser = {
    code?: number;
    data?: PageSysUser;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageUserVO = {
    code?: number;
    data?: PageUserVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageVenue = {
    code?: number;
    data?: PageVenue;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageVenueVO = {
    code?: number;
    data?: PageVenueVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageVenueZone = {
    code?: number;
    data?: PageVenueZone;
    message?: string;
    requestId?: string;
  };

  type BaseResponsePageVenueZoneVO = {
    code?: number;
    data?: PageVenueZoneVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseRoleVO = {
    code?: number;
    data?: RoleVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseStatsSnapshot = {
    code?: number;
    data?: StatsSnapshot;
    message?: string;
    requestId?: string;
  };

  type BaseResponseStatsSnapshotVO = {
    code?: number;
    data?: StatsSnapshotVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseSysRole = {
    code?: number;
    data?: SysRole;
    message?: string;
    requestId?: string;
  };

  type BaseResponseSystemAuditLog = {
    code?: number;
    data?: SystemAuditLog;
    message?: string;
    requestId?: string;
  };

  type BaseResponseSystemAuditLogVO = {
    code?: number;
    data?: SystemAuditLogVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseSysUser = {
    code?: number;
    data?: SysUser;
    message?: string;
    requestId?: string;
  };

  type BaseResponseUserVO = {
    code?: number;
    data?: UserVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseVenue = {
    code?: number;
    data?: Venue;
    message?: string;
    requestId?: string;
  };

  type BaseResponseVenueVO = {
    code?: number;
    data?: VenueVO;
    message?: string;
    requestId?: string;
  };

  type BaseResponseVenueZone = {
    code?: number;
    data?: VenueZone;
    message?: string;
    requestId?: string;
  };

  type BaseResponseVenueZoneVO = {
    code?: number;
    data?: VenueZoneVO;
    message?: string;
    requestId?: string;
  };

  type CameraDevice = {
    id?: number;
    venue_id?: number;
    zone_id?: number;
    camera_code?: string;
    camera_name?: string;
    stream_url?: string;
    protocol?: string;
    device_status?: string;
    health_status?: string;
    enabled?: number;
    last_heartbeat_at?: string;
    created_at?: string;
    updated_at?: string;
    is_delete?: number;
  };

  type CameraDeviceAddRequest = {
    venueId?: number;
    zoneId?: number;
    cameraCode?: string;
    cameraName?: string;
    streamUrl?: string;
    protocol?: string;
    deviceStatus?: string;
    healthStatus?: string;
    enabled?: number;
    lastHeartbeatAt?: string;
  };

  type CameraDeviceBatchDisableRequest = {
    cameraIds?: number[];
  };

  type CameraDeviceEditRequest = {
    id?: number;
    zoneId?: number;
    cameraName?: string;
    streamUrl?: string;
    protocol?: string;
    deviceStatus?: string;
    healthStatus?: string;
    enabled?: number;
    lastHeartbeatAt?: string;
  };

  type CameraDeviceQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    venueId?: number;
    zoneId?: number;
    cameraCode?: string;
    cameraName?: string;
    streamUrl?: string;
    protocol?: string;
    deviceStatus?: string;
    healthStatus?: string;
    enabled?: number;
  };

  type CameraDeviceUpdateRequest = {
    id?: number;
    venueId?: number;
    zoneId?: number;
    cameraCode?: string;
    cameraName?: string;
    streamUrl?: string;
    protocol?: string;
    deviceStatus?: string;
    healthStatus?: string;
    enabled?: number;
    lastHeartbeatAt?: string;
  };

  type CameraDeviceVO = {
    id?: number;
    venueId?: number;
    zoneId?: number;
    cameraCode?: string;
    cameraName?: string;
    streamUrl?: string;
    protocol?: string;
    deviceStatus?: string;
    healthStatus?: string;
    enabled?: number;
    lastHeartbeatAt?: string;
    createdAt?: string;
    updatedAt?: string;
  };

  type BatchOperateResultVO = {
    successIds?: number[];
    failed?: FailedItem[];
    successCount?: number;
    failedCount?: number;
  };

  type FailedItem = {
    id?: number;
    reason?: string;
  };

  type CameraMaintenanceLog = {
    id?: number;
    camera_id?: number;
    maintenance_type?: string;
    maintenance_content?: string;
    maintained_by?: string;
    maintained_at?: string;
    next_maintenance_at?: string;
    is_delete?: number;
  };

  type CameraMaintenanceLogAddRequest = {
    cameraId?: number;
    maintenanceType?: string;
    maintenanceContent?: string;
    maintainedBy?: string;
    maintainedAt?: string;
    nextMaintenanceAt?: string;
  };

  type CameraMaintenanceLogEditRequest = {
    id?: number;
    maintenanceType?: string;
    maintenanceContent?: string;
    maintainedBy?: string;
    maintainedAt?: string;
    nextMaintenanceAt?: string;
  };

  type CameraMaintenanceLogQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    cameraId?: number;
    maintenanceType?: string;
    maintenanceContent?: string;
    maintainedBy?: string;
    startMaintainedAt?: string;
    endMaintainedAt?: string;
  };

  type CameraMaintenanceLogUpdateRequest = {
    id?: number;
    cameraId?: number;
    maintenanceType?: string;
    maintenanceContent?: string;
    maintainedBy?: string;
    maintainedAt?: string;
    nextMaintenanceAt?: string;
  };

  type CameraMaintenanceLogVO = {
    id?: number;
    cameraId?: number;
    maintenanceType?: string;
    maintenanceContent?: string;
    maintainedBy?: string;
    maintainedAt?: string;
    nextMaintenanceAt?: string;
  };

  type CaptchaVO = {
    captchaId?: string;
    captchaImageBase64?: string;
    expireAt?: number;
  };

  type DataAnalysisReportQueryRequest = {
    venueId?: number;
    type?: string;
    startTime?: number;
    endTime?: number;
  };

  type DataPreprocessQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    taskId?: number;
    venueId?: number;
    status?: string;
    startTime?: number;
    endTime?: number;
  };

  type DeleteRequest = {
    id?: number;
  };

  type getAiStreamTaskByCodeParams = {
    taskCode: string;
  };

  type getAiStreamTaskByIdParams = {
    id: number;
  };

  type getAiStreamTaskVOByIdParams = {
    id: number;
  };

  type getAlertByIdParams = {
    id: number;
  };

  type getAlertDisposalByIdParams = {
    id: number;
  };

  type getAlertDisposalVOByIdParams = {
    id: number;
  };

  type getAlertRecordByIdParams = {
    id: number;
  };

  type getAlertRecordVOByIdParams = {
    id: number;
  };

  type getAnnotatedStreamParams = {
    cameraId: number;
  };

  type getCameraDeviceByIdParams = {
    id: number;
  };

  type getCameraDeviceVOByIdParams = {
    id: number;
  };

  type getCameraMaintenanceLogByIdParams = {
    id: number;
  };

  type getCameraMaintenanceLogVOByIdParams = {
    id: number;
  };

  type getCollectStatusParams = {
    cameraId?: number;
    venueId?: number;
  };

  type getLifeguardByIdParams = {
    id: number;
  };

  type getLifeguardDutyLogByIdParams = {
    id: number;
  };

  type getLifeguardDutyLogVOByIdParams = {
    id: number;
  };

  type getLifeguardLocationLogByIdParams = {
    id: number;
  };

  type getLifeguardLocationLogVOByIdParams = {
    id: number;
  };

  type getLifeguardVOByIdParams = {
    id: number;
  };

  type getMonitoringEventByIdParams = {
    id: number;
  };

  type getMonitoringEventVOByIdParams = {
    id: number;
  };

  type getOverviewParams = {
    venueId?: number;
    date?: string;
  };

  type getRoleByIdParams = {
    id: number;
  };

  type getRoleVOByIdParams = {
    id: number;
  };

  type getStatsSnapshotByIdParams = {
    id: number;
  };

  type getStatsSnapshotVOByIdParams = {
    id: number;
  };

  type getSystemAuditLogByIdParams = {
    id: number;
  };

  type getSystemAuditLogVOByIdParams = {
    id: number;
  };

  type getTaskByCodeParams = {
    taskCode: string;
  };

  type getTaskByPathParams = {
    taskCode: string;
  };

  type getUserByIdParams = {
    id: number;
  };

  type getUserVOByIdParams = {
    id: number;
  };

  type getVenueByIdParams = {
    id: number;
  };

  type getVenueVOByIdParams = {
    id: number;
  };

  type getVenueZoneByIdParams = {
    id: number;
  };

  type getVenueZoneVOByIdParams = {
    id: number;
  };

  type Lifeguard = {
    id?: number;
    user_id?: number;
    lifeguard_code?: string;
    full_name?: string;
    phone?: string;
    venue_id?: number;
    fence_geo_json?: Record<string, any>;
    audit_status?: string;
    duty_status?: string;
    last_login_at?: string;
    created_at?: string;
    updated_at?: string;
    is_delete?: number;
  };

  type LifeguardAddRequest = {
    userId?: number;
    username?: string;
    password?: string;
    email?: string;
    lifeguardCode?: string;
    fullName?: string;
    phone?: string;
    venueId?: number;
    fenceGeoJson?: Record<string, any>;
    auditStatus?: string;
    dutyStatus?: string;
  };

  type LifeguardAuditRequest = {
    lifeguardId?: number;
    auditStatus?: string;
    approvedBy?: number;
  };

  type LifeguardDutyLog = {
    id?: number;
    lifeguard_id?: number;
    action_type?: string;
    leave_reason?: string;
    planned_return_at?: string;
    actual_return_at?: string;
    approved_by?: number;
    created_at?: string;
  };

  type LifeguardDutyLogAddRequest = {
    lifeguardId?: number;
    actionType?: string;
    leaveReason?: string;
    plannedReturnAt?: string;
    actualReturnAt?: string;
    approvedBy?: number;
  };

  type LifeguardDutyLogEditRequest = {
    id?: number;
    actionType?: string;
    leaveReason?: string;
    plannedReturnAt?: string;
    actualReturnAt?: string;
    approvedBy?: number;
  };

  type LifeguardDutyLogQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    lifeguardId?: number;
    actionType?: string;
    approvedBy?: number;
    plannedReturnAt?: string;
    actualReturnAt?: string;
  };

  type LifeguardDutyLogUpdateRequest = {
    id?: number;
    lifeguardId?: number;
    actionType?: string;
    leaveReason?: string;
    plannedReturnAt?: string;
    actualReturnAt?: string;
    approvedBy?: number;
  };

  type LifeguardDutyLogVO = {
    id?: number;
    lifeguardId?: number;
    actionType?: string;
    leaveReason?: string;
    plannedReturnAt?: string;
    actualReturnAt?: string;
    approvedBy?: number;
    createdAt?: string;
  };

  type LifeguardDutyUpdateRequest = {
    lifeguardId?: number;
    dutyStatus?: string;
    operatorId?: number;
  };

  type LifeguardEditRequest = {
    id?: number;
    fullName?: string;
    phone?: string;
    venueId?: number;
    fenceGeoJson?: Record<string, any>;
    auditStatus?: string;
    dutyStatus?: string;
  };

  type LifeguardLeaveReportRequest = {
    lifeguardId?: number;
    leaveReason?: string;
    plannedReturnAt?: string;
  };

  type LifeguardLocationLog = {
    id?: number;
    lifeguard_id?: number;
    venue_id?: number;
    longitude?: number;
    latitude?: number;
    in_fence?: number;
    report_source?: string;
    reported_at?: string;
  };

  type LifeguardLocationLogAddRequest = {
    lifeguardId?: number;
    venueId?: number;
    longitude?: number;
    latitude?: number;
    inFence?: number;
    reportSource?: string;
    reportedAt?: string;
  };

  type LifeguardLocationLogEditRequest = {
    id?: number;
    longitude?: number;
    latitude?: number;
    inFence?: number;
    reportSource?: string;
    reportedAt?: string;
  };

  type LifeguardLocationLogQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    lifeguardId?: number;
    venueId?: number;
    inFence?: number;
    reportSource?: string;
  };

  type LifeguardLocationLogUpdateRequest = {
    id?: number;
    lifeguardId?: number;
    venueId?: number;
    longitude?: number;
    latitude?: number;
    inFence?: number;
    reportSource?: string;
    reportedAt?: string;
  };

  type LifeguardLocationLogVO = {
    id?: number;
    lifeguardId?: number;
    venueId?: number;
    longitude?: number;
    latitude?: number;
    inFence?: number;
    reportSource?: string;
    reportedAt?: string;
  };

  type LifeguardLocationReportRequest = {
    lifeguardId?: number;
    venueId?: number;
    longitude?: number;
    latitude?: number;
    inFence?: number;
    reportSource?: string;
    reportedAt?: string;
  };

  type LifeguardOffPostCheckRequest = {
    lifeguardId?: number;
    leaveReason?: string;
    plannedReturnAt?: string;
  };

  type LifeguardQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    userId?: number;
    lifeguardCode?: string;
    fullName?: string;
    phone?: string;
    venueId?: number;
    auditStatus?: string;
    dutyStatus?: string;
  };

  type LifeguardUpdateRequest = {
    id?: number;
    userId?: number;
    lifeguardCode?: string;
    fullName?: string;
    phone?: string;
    venueId?: number;
    fenceGeoJson?: Record<string, any>;
    auditStatus?: string;
    dutyStatus?: string;
  };

  type LifeguardVO = {
    id?: number;
    userId?: number;
    lifeguardCode?: string;
    fullName?: string;
    phone?: string;
    venueId?: number;
    fenceGeoJson?: Record<string, any>;
    auditStatus?: string;
    dutyStatus?: string;
    lastLoginAt?: string;
    createdAt?: string;
    updatedAt?: string;
  };

  type listByCameraParams = {
    cameraId: number;
    current?: number;
    pageSize?: number;
  };

  type listEventsParams = {
    cameraId?: number;
    eventType?: string;
    startTime?: number;
    endTime?: number;
    limit?: number;
  };

  type LoginRequest = {
    username?: string;
    password?: string;
    deviceId?: string;
    clientType?: string;
    clientVersion?: string;
  };

  type LoginResultVO = {
    accessToken?: string;
    refreshToken?: string;
    expiresIn?: number;
    forceChangePassword?: number;
    user?: UserInfo;
  };

  type LogoutRequest = {
    deviceId?: string;
    refreshToken?: string;
  };

  type MonitoringEvent = {
    id?: number;
    event_uid?: string;
    camera_id?: number;
    task_id?: number;
    event_type?: string;
    risk_level?: string;
    confidence?: number;
    target_id?: string;
    pool_head_count?: number;
    bbox_json?: Record<string, any>;
    position_desc?: string;
    emergency_contact_name?: string;
    emergency_contact_phone?: string;
    incident_location?: string;
    video_stream_url?: string;
    event_time?: string;
    ext_json?: Record<string, any>;
    created_at?: string;
  };

  type MonitoringEventAddRequest = {
    eventUid?: string;
    cameraId?: number;
    taskId?: number;
    eventType?: string;
    riskLevel?: string;
    confidence?: number;
    targetId?: string;
    poolHeadCount?: number;
    bboxJson?: Record<string, any>;
    positionDesc?: string;
    emergencyContactName?: string;
    emergencyContactPhone?: string;
    incidentLocation?: string;
    videoStreamUrl?: string;
    eventTime?: string;
    extJson?: Record<string, any>;
  };

  type MonitoringEventEditRequest = {
    id?: number;
    riskLevel?: string;
    confidence?: number;
    poolHeadCount?: number;
    positionDesc?: string;
    emergencyContactName?: string;
    emergencyContactPhone?: string;
    incidentLocation?: string;
    videoStreamUrl?: string;
    eventTime?: string;
    extJson?: Record<string, any>;
  };

  type MonitoringEventQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    eventUid?: string;
    cameraId?: number;
    taskId?: number;
    eventType?: string;
    riskLevel?: string;
    startEventTime?: string;
    endEventTime?: string;
  };

  type MonitoringEventUpdateRequest = {
    id?: number;
    eventUid?: string;
    cameraId?: number;
    taskId?: number;
    eventType?: string;
    riskLevel?: string;
    confidence?: number;
    targetId?: string;
    poolHeadCount?: number;
    bboxJson?: Record<string, any>;
    positionDesc?: string;
    emergencyContactName?: string;
    emergencyContactPhone?: string;
    incidentLocation?: string;
    videoStreamUrl?: string;
    eventTime?: string;
    extJson?: Record<string, any>;
  };

  type MonitoringEventVO = {
    id?: number;
    eventUid?: string;
    cameraId?: number;
    taskId?: number;
    eventType?: string;
    riskLevel?: string;
    confidence?: number;
    targetId?: string;
    poolHeadCount?: number;
    bboxJson?: Record<string, any>;
    positionDesc?: string;
    emergencyContactName?: string;
    emergencyContactPhone?: string;
    incidentLocation?: string;
    videoStreamUrl?: string;
    eventTime?: string;
    extJson?: Record<string, any>;
    createdAt?: string;
  };

  type MonitorTaskControlRequest = {
    taskCode?: string;
  };

  type MonitorTaskModelSwitchRequest = {
    taskCode?: string;
    modelVersion?: string;
  };

  type OrderItem = {
    column?: string;
    asc?: boolean;
  };

  type PageAiStreamTask = {
    records?: AiStreamTask[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageAiStreamTask;
    searchCount?: PageAiStreamTask;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageAiStreamTaskVO = {
    records?: AiStreamTaskVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageAiStreamTaskVO;
    searchCount?: PageAiStreamTaskVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageAlertDisposal = {
    records?: AlertDisposal[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageAlertDisposal;
    searchCount?: PageAlertDisposal;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageAlertDisposalVO = {
    records?: AlertDisposalVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageAlertDisposalVO;
    searchCount?: PageAlertDisposalVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageAlertRecord = {
    records?: AlertRecord[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageAlertRecord;
    searchCount?: PageAlertRecord;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageAlertRecordVO = {
    records?: AlertRecordVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageAlertRecordVO;
    searchCount?: PageAlertRecordVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageCameraDevice = {
    records?: CameraDevice[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageCameraDevice;
    searchCount?: PageCameraDevice;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageCameraDeviceVO = {
    records?: CameraDeviceVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageCameraDeviceVO;
    searchCount?: PageCameraDeviceVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageCameraMaintenanceLog = {
    records?: CameraMaintenanceLog[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageCameraMaintenanceLog;
    searchCount?: PageCameraMaintenanceLog;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageCameraMaintenanceLogVO = {
    records?: CameraMaintenanceLogVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageCameraMaintenanceLogVO;
    searchCount?: PageCameraMaintenanceLogVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageLifeguard = {
    records?: Lifeguard[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageLifeguard;
    searchCount?: PageLifeguard;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageLifeguardDutyLog = {
    records?: LifeguardDutyLog[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageLifeguardDutyLog;
    searchCount?: PageLifeguardDutyLog;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageLifeguardDutyLogVO = {
    records?: LifeguardDutyLogVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageLifeguardDutyLogVO;
    searchCount?: PageLifeguardDutyLogVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageLifeguardLocationLog = {
    records?: LifeguardLocationLog[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageLifeguardLocationLog;
    searchCount?: PageLifeguardLocationLog;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageLifeguardLocationLogVO = {
    records?: LifeguardLocationLogVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageLifeguardLocationLogVO;
    searchCount?: PageLifeguardLocationLogVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageLifeguardVO = {
    records?: LifeguardVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageLifeguardVO;
    searchCount?: PageLifeguardVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageMonitoringEvent = {
    records?: MonitoringEvent[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageMonitoringEvent;
    searchCount?: PageMonitoringEvent;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageMonitoringEventVO = {
    records?: MonitoringEventVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageMonitoringEventVO;
    searchCount?: PageMonitoringEventVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageRoleVO = {
    records?: RoleVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageRoleVO;
    searchCount?: PageRoleVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageStatsSnapshot = {
    records?: StatsSnapshot[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageStatsSnapshot;
    searchCount?: PageStatsSnapshot;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageStatsSnapshotVO = {
    records?: StatsSnapshotVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageStatsSnapshotVO;
    searchCount?: PageStatsSnapshotVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageSysRole = {
    records?: SysRole[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageSysRole;
    searchCount?: PageSysRole;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageSystemAuditLog = {
    records?: SystemAuditLog[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageSystemAuditLog;
    searchCount?: PageSystemAuditLog;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageSystemAuditLogVO = {
    records?: SystemAuditLogVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageSystemAuditLogVO;
    searchCount?: PageSystemAuditLogVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageSysUser = {
    records?: SysUser[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageSysUser;
    searchCount?: PageSysUser;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageUserVO = {
    records?: UserVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageUserVO;
    searchCount?: PageUserVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageVenue = {
    records?: Venue[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageVenue;
    searchCount?: PageVenue;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageVenueVO = {
    records?: VenueVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageVenueVO;
    searchCount?: PageVenueVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageVenueZone = {
    records?: VenueZone[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageVenueZone;
    searchCount?: PageVenueZone;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageVenueZoneVO = {
    records?: VenueZoneVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: PageVenueZoneVO;
    searchCount?: PageVenueZoneVO;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type rankingParams = {
    startDate?: string;
    endDate?: string;
    limit?: number;
  };

  type recentLocationsParams = {
    lifeguardId: number;
    limit?: number;
  };

  type RefreshTokenRequest = {
    refreshToken?: string;
    deviceId?: string;
  };

  type RegisterRequest = {
    displayName?: string;
    username?: string;
    password?: string;
    roleCode?: string;
    captchaId?: string;
    captchaCode?: string;
  };

  type reportLocationByPathParams = {
    id: number;
  };

  type RoleAddRequest = {
    roleCode?: string;
    roleName?: string;
    permissions?: string[];
    status?: number;
  };

  type RolePermissionUpdateRequest = {
    roleCode?: string;
    permissions?: string[];
  };

  type RoleQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    roleCode?: string;
    roleName?: string;
    status?: number;
  };

  type RoleUpdateRequest = {
    id?: number;
    roleCode?: string;
    roleName?: string;
    permissions?: string[];
    status?: number;
  };

  type RoleVO = {
    id?: number;
    roleCode?: string;
    roleName?: string;
    permissionJson?: Record<string, any>;
    status?: number;
    createdAt?: string;
    updatedAt?: string;
  };

  type StartMonitorTaskRequest = {
    cameraId?: number;
    taskCode?: string;
    frameIntervalMs?: number;
    modelVersion?: string;
    callbackUrl?: string;
  };

  type StatsExportRequest = {
    venueId?: number;
    metricType?: string;
    startDate?: string;
    endDate?: string;
  };

  type StatsSnapshot = {
    id?: number;
    granularity?: string;
    snapshot_date?: string;
    snapshot_hour?: number;
    venue_id?: number;
    metric_type?: string;
    metric_key?: string;
    metric_value?: number;
    dimension_json?: Record<string, any>;
    created_at?: string;
  };

  type StatsSnapshotAddRequest = {
    granularity?: string;
    snapshotDate?: string;
    snapshotHour?: number;
    venueId?: number;
    metricType?: string;
    metricKey?: string;
    metricValue?: number;
    dimensionJson?: Record<string, any>;
  };

  type StatsSnapshotEditRequest = {
    id?: number;
    metricValue?: number;
    dimensionJson?: Record<string, any>;
  };

  type StatsSnapshotQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    granularity?: string;
    snapshotDate?: string;
    snapshotHour?: number;
    venueId?: number;
    metricType?: string;
    metricKey?: string;
  };

  type StatsSnapshotUpdateRequest = {
    id?: number;
    granularity?: string;
    snapshotDate?: string;
    snapshotHour?: number;
    venueId?: number;
    metricType?: string;
    metricKey?: string;
    metricValue?: number;
    dimensionJson?: Record<string, any>;
  };

  type StatsSnapshotVO = {
    id?: number;
    granularity?: string;
    snapshotDate?: string;
    snapshotHour?: number;
    venueId?: number;
    metricType?: string;
    metricKey?: string;
    metricValue?: number;
    dimensionJson?: Record<string, any>;
    createdAt?: string;
  };

  type StatsTrendRequest = {
    venueId?: number;
    metricType?: string;
    metricKey?: string;
    granularity?: string;
    startDate?: string;
    endDate?: string;
  };

  type submitLeaveReportByPathParams = {
    id: number;
  };

  type SysRole = {
    id?: number;
    role_code?: string;
    role_name?: string;
    permission_json?: Record<string, any>;
    status?: number;
    created_at?: string;
    updated_at?: string;
    is_delete?: number;
  };

  type SystemAuditLog = {
    id?: number;
    trace_id?: string;
    log_category?: string;
    operator_id?: number;
    operator_name?: string;
    client_ip?: string;
    request_uri?: string;
    request_method?: string;
    request_body?: string;
    response_code?: number;
    response_message?: string;
    cost_ms?: number;
    created_at?: string;
  };

  type SystemAuditLogAddRequest = {
    traceId?: string;
    logCategory?: string;
    operatorId?: number;
    operatorName?: string;
    clientIp?: string;
    requestUri?: string;
    requestMethod?: string;
    requestBody?: string;
    responseCode?: number;
    responseMessage?: string;
    costMs?: number;
  };

  type SystemAuditLogEditRequest = {
    id?: number;
    responseMessage?: string;
    responseCode?: number;
    costMs?: number;
  };

  type SystemAuditLogQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    traceId?: string;
    logCategory?: string;
    operatorId?: number;
    operatorName?: string;
    requestUri?: string;
    responseCode?: number;
    startCreatedAt?: string;
    endCreatedAt?: string;
  };

  type SystemAuditLogUpdateRequest = {
    id?: number;
    traceId?: string;
    logCategory?: string;
    operatorId?: number;
    operatorName?: string;
    clientIp?: string;
    requestUri?: string;
    requestMethod?: string;
    requestBody?: string;
    responseCode?: number;
    responseMessage?: string;
    costMs?: number;
  };

  type SystemAuditLogVO = {
    id?: number;
    traceId?: string;
    logCategory?: string;
    operatorId?: number;
    operatorName?: string;
    clientIp?: string;
    requestUri?: string;
    requestMethod?: string;
    requestBody?: string;
    responseCode?: number;
    responseMessage?: string;
    costMs?: number;
    createdAt?: string;
  };

  type SysUser = {
    id?: number;
    username?: string;
    password_hash?: string;
    display_name?: string;
    phone?: string;
    email?: string;
    status?: number;
    failed_login_count?: number;
    lock_until?: string;
    force_change_password?: number;
    last_login_at?: string;
    created_at?: string;
    updated_at?: string;
    is_delete?: number;
  };

  type updateDutyStatusByPathParams = {
    id: number;
  };

  type UserAddRequest = {
    username?: string;
    password?: string;
    displayName?: string;
    phone?: string;
    email?: string;
    status?: number;
    forceChangePassword?: number;
    roleCodes?: string[];
  };

  type UserAssignRoleRequest = {
    userId?: number;
    roleCodes?: string[];
  };

  type UserInfo = {
    id?: number;
    username?: string;
    displayName?: string;
    roles?: string[];
    permissions?: string[];
  };

  type UserQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    username?: string;
    displayName?: string;
    phone?: string;
    status?: number;
    roleCode?: string;
  };

  type UserUpdateMyProfileRequest = {
    id?: number;
    displayName?: string;
    phone?: string;
    email?: string;
    oldPassword?: string;
    newPassword?: string;
  };

  type UserUpdateRequest = {
    id?: number;
    username?: string;
    password?: string;
    displayName?: string;
    phone?: string;
    email?: string;
    status?: number;
    forceChangePassword?: number;
    roleCodes?: string[];
  };

  type UserVO = {
    id?: number;
    username?: string;
    displayName?: string;
    phone?: string;
    email?: string;
    status?: number;
    forceChangePassword?: number;
    lastLoginAt?: string;
    createdAt?: string;
    updatedAt?: string;
    roleCodes?: string[];
  };

  type Venue = {
    id?: number;
    venue_code?: string;
    venue_name?: string;
    address?: string;
    contact_name?: string;
    contact_phone?: string;
    timezone?: string;
    status?: number;
    fence_geo_json?: Record<string, any>;
    created_at?: string;
    updated_at?: string;
    is_delete?: number;
  };

  type VenueAddRequest = {
    venueCode?: string;
    venueName?: string;
    address?: string;
    contactName?: string;
    contactPhone?: string;
    timezone?: string;
    status?: number;
    fenceGeoJson?: Record<string, any>;
  };

  type VenueEditRequest = {
    id?: number;
    venueName?: string;
    address?: string;
    contactName?: string;
    contactPhone?: string;
    timezone?: string;
    status?: number;
    fenceGeoJson?: Record<string, any>;
  };

  type VenueFenceBoundsRequest = {
    current?: number;
    pageSize?: number;
    minLng?: number;
    maxLng?: number;
    minLat?: number;
    maxLat?: number;
    status?: number;
  };

  type VenueQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    venueCode?: string;
    venueName?: string;
    status?: number;
    contactName?: string;
  };

  type VenueUpdateRequest = {
    id?: number;
    venueCode?: string;
    venueName?: string;
    address?: string;
    contactName?: string;
    contactPhone?: string;
    timezone?: string;
    status?: number;
    fenceGeoJson?: Record<string, any>;
  };

  type VenueVO = {
    id?: number;
    venueCode?: string;
    venueName?: string;
    address?: string;
    contactName?: string;
    contactPhone?: string;
    timezone?: string;
    status?: number;
    fenceGeoJson?: Record<string, any>;
    createdAt?: string;
    updatedAt?: string;
  };

  type VenueZone = {
    id?: number;
    venue_id?: number;
    zone_code?: string;
    zone_name?: string;
    zone_type?: string;
    geo_json?: Record<string, any>;
    risk_level?: string;
    created_at?: string;
    updated_at?: string;
    is_delete?: number;
  };

  type VenueZoneAddRequest = {
    venueId?: number;
    zoneCode?: string;
    zoneName?: string;
    zoneType?: string;
    geoJson?: string;
    riskLevel?: string;
  };

  type VenueZoneEditRequest = {
    id?: number;
    zoneName?: string;
    zoneType?: string;
    geoJson?: string;
    riskLevel?: string;
  };

  type VenueZoneQueryRequest = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    venueId?: number;
    zoneCode?: string;
    zoneName?: string;
    zoneType?: string;
    riskLevel?: string;
  };

  type VenueZoneUpdateRequest = {
    id?: number;
    venueId?: number;
    zoneCode?: string;
    zoneName?: string;
    zoneType?: string;
    geoJson?: string;
    riskLevel?: string;
  };

  type VenueZoneVO = {
    id?: number;
    venueId?: number;
    zoneCode?: string;
    zoneName?: string;
    zoneType?: string;
    geoJson?: Record<string, any>;
    riskLevel?: string;
    createdAt?: string;
    updatedAt?: string;
  };
}
