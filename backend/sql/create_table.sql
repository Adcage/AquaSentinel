-- AI防溺水系统数据库初始化脚本（DDL + 初始管理员）

CREATE DATABASE IF NOT EXISTS ai_drowning
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ai_drowning;

SET NAMES utf8mb4;

-- =========================
-- 1) 账号与鉴权
-- =========================

CREATE TABLE IF NOT EXISTS sys_user
(
    id                    BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    username              VARCHAR(64)                         NOT NULL COMMENT '登录账号',
    password_hash         VARCHAR(128)                        NOT NULL COMMENT '密码哈希',
    display_name          VARCHAR(64)                         NOT NULL COMMENT '显示名称',
    phone                 VARCHAR(20)                         NULL COMMENT '手机号',
    email                 VARCHAR(128)                        NULL COMMENT '邮箱',
    status                TINYINT      DEFAULT 1              NOT NULL COMMENT '状态:1启用,0禁用',
    failed_login_count    INT          DEFAULT 0              NOT NULL COMMENT '连续登录失败次数',
    lock_until            DATETIME                             NULL COMMENT '锁定截止时间',
    force_change_password TINYINT      DEFAULT 1              NOT NULL COMMENT '首登强制改密',
    last_login_at         DATETIME                             NULL COMMENT '最近登录时间',
    created_at            DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at            DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete             TINYINT      DEFAULT 0              NOT NULL COMMENT '逻辑删除',
    UNIQUE KEY uk_username (username),
    KEY idx_status (status),
    KEY idx_phone (phone)
) COMMENT '系统用户表' ENGINE = InnoDB
                   DEFAULT CHARSET = utf8mb4
                   COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_role
(
    id              BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    role_code       VARCHAR(32)                         NOT NULL COMMENT '角色编码',
    role_name       VARCHAR(64)                         NOT NULL COMMENT '角色名称',
    permission_json JSON                                NOT NULL COMMENT '权限集合JSON',
    status          TINYINT      DEFAULT 1              NOT NULL COMMENT '状态:1启用,0禁用',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete       TINYINT      DEFAULT 0              NOT NULL COMMENT '逻辑删除',
    UNIQUE KEY uk_role_code (role_code)
) COMMENT '系统角色表' ENGINE = InnoDB
                   DEFAULT CHARSET = utf8mb4
                   COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_user_role
(
    id         BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    user_id    BIGINT                              NOT NULL COMMENT '用户ID',
    role_id    BIGINT                              NOT NULL COMMENT '角色ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP  NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) COMMENT '用户角色关联表' ENGINE = InnoDB
                     DEFAULT CHARSET = utf8mb4
                     COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_refresh_token
(
    id                 BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    user_id            BIGINT                              NOT NULL COMMENT '用户ID',
    refresh_token_hash VARCHAR(128)                        NOT NULL COMMENT 'RefreshToken哈希',
    device_id          VARCHAR(128)                        NOT NULL COMMENT '设备标识',
    client_type        VARCHAR(32)                         NOT NULL COMMENT '客户端类型',
    client_version     VARCHAR(32)                         NULL COMMENT '客户端版本',
    ip_address         VARCHAR(64)                         NULL COMMENT '登录IP',
    expires_at         DATETIME                             NOT NULL COMMENT '过期时间',
    revoked            TINYINT      DEFAULT 0              NOT NULL COMMENT '是否吊销',
    revoked_at         DATETIME                             NULL COMMENT '吊销时间',
    revoke_reason      VARCHAR(128)                        NULL COMMENT '吊销原因',
    last_used_at       DATETIME                             NULL COMMENT '最近使用时间',
    created_at         DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    KEY idx_user_active (user_id, revoked, expires_at),
    KEY idx_device (device_id),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) COMMENT '刷新令牌会话表' ENGINE = InnoDB
                   DEFAULT CHARSET = utf8mb4
                   COLLATE = utf8mb4_unicode_ci;

-- =========================
-- 2) 场馆与设备
-- =========================

CREATE TABLE IF NOT EXISTS venue
(
    id            BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    venue_code    VARCHAR(32)                         NOT NULL COMMENT '场馆编码',
    venue_name    VARCHAR(128)                        NOT NULL COMMENT '场馆名称',
    address       VARCHAR(256)                        NOT NULL COMMENT '详细地址',
    contact_name  VARCHAR(64)                         NOT NULL COMMENT '紧急联系人姓名',
    contact_phone VARCHAR(20)                         NOT NULL COMMENT '紧急联系人电话',
    timezone      VARCHAR(64) DEFAULT 'Asia/Shanghai' NOT NULL COMMENT '时区',
    status        TINYINT     DEFAULT 1               NOT NULL COMMENT '状态:1启用,0禁用',
    fence_geo_json JSON                                NULL COMMENT '场馆电子围栏GeoJSON',
    created_at    DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at    DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete     TINYINT     DEFAULT 0               NOT NULL COMMENT '逻辑删除',
    UNIQUE KEY uk_venue_code (venue_code),
    KEY idx_venue_status (status)
) COMMENT '场馆表' ENGINE = InnoDB
               DEFAULT CHARSET = utf8mb4
               COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS venue_zone
(
    id         BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    venue_id   BIGINT                              NOT NULL COMMENT '场馆ID',
    zone_code  VARCHAR(32)                         NOT NULL COMMENT '区域编码',
    zone_name  VARCHAR(128)                        NOT NULL COMMENT '区域名称',
    zone_type  VARCHAR(32) DEFAULT 'POOL'          NOT NULL COMMENT '区域类型',
    geo_json   JSON                                NOT NULL COMMENT '区域GeoJSON',
    risk_level VARCHAR(16) DEFAULT 'LOW'           NOT NULL COMMENT '风险等级',
    created_at DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete  TINYINT     DEFAULT 0               NOT NULL COMMENT '逻辑删除',
    UNIQUE KEY uk_zone_code (zone_code),
    KEY idx_zone_venue_type (venue_id, zone_type),
    CONSTRAINT fk_zone_venue FOREIGN KEY (venue_id) REFERENCES venue (id)
) COMMENT '场馆区域表' ENGINE = InnoDB
                   DEFAULT CHARSET = utf8mb4
                   COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS camera_device
(
    id                BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    venue_id          BIGINT                              NOT NULL COMMENT '场馆ID',
    zone_id           BIGINT                               NULL COMMENT '区域ID',
    camera_code       VARCHAR(32)                         NOT NULL COMMENT '摄像头编码',
    camera_name       VARCHAR(128)                        NOT NULL COMMENT '摄像头名称',
    stream_url        VARCHAR(512)                        NOT NULL COMMENT '视频流地址',
    protocol          VARCHAR(16) DEFAULT 'RTSP'          NOT NULL COMMENT '流协议',
    device_status     VARCHAR(16) DEFAULT 'OFFLINE'       NOT NULL COMMENT '设备在线状态',
    health_status     VARCHAR(16) DEFAULT 'NORMAL'        NOT NULL COMMENT '健康状态',
    enabled           TINYINT     DEFAULT 1               NOT NULL COMMENT '是否启用',
    last_heartbeat_at DATETIME                             NULL COMMENT '最近心跳时间',
    created_at        DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at        DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete         TINYINT     DEFAULT 0               NOT NULL COMMENT '逻辑删除',
    UNIQUE KEY uk_camera_code (camera_code),
    KEY idx_camera_venue_status (venue_id, device_status),
    CONSTRAINT fk_camera_venue FOREIGN KEY (venue_id) REFERENCES venue (id),
    CONSTRAINT fk_camera_zone FOREIGN KEY (zone_id) REFERENCES venue_zone (id)
) COMMENT '摄像头设备表' ENGINE = InnoDB
                   DEFAULT CHARSET = utf8mb4
                   COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS camera_maintenance_log
(
    id                  BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    camera_id           BIGINT                              NOT NULL COMMENT '摄像头ID',
    maintenance_type    VARCHAR(32)                         NOT NULL COMMENT '维护类型',
    maintenance_content VARCHAR(512)                        NOT NULL COMMENT '维护内容',
    maintained_by       VARCHAR(64)                         NOT NULL COMMENT '维护人',
    maintained_at       DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '维护时间',
    next_maintenance_at DATETIME                             NULL COMMENT '下次维护时间',
    KEY idx_camera_maintained_at (camera_id, maintained_at),
    CONSTRAINT fk_maintenance_camera FOREIGN KEY (camera_id) REFERENCES camera_device (id)
) COMMENT '设备维护记录表' ENGINE = InnoDB
                     DEFAULT CHARSET = utf8mb4
                     COLLATE = utf8mb4_unicode_ci;

-- =========================
-- 3) 救生员与数据采集
-- =========================

CREATE TABLE IF NOT EXISTS lifeguard
(
    id              BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    user_id         BIGINT                              NOT NULL COMMENT '关联用户ID',
    lifeguard_code  VARCHAR(32)                         NOT NULL COMMENT '救生员编码',
    full_name       VARCHAR(64)                         NOT NULL COMMENT '姓名',
    phone           VARCHAR(20)                         NOT NULL COMMENT '手机号',
    venue_id        BIGINT                              NOT NULL COMMENT '绑定场馆',
    fence_geo_json  JSON                                NOT NULL COMMENT '电子围栏GeoJSON',
    audit_status    VARCHAR(16) DEFAULT 'PENDING'       NOT NULL COMMENT '审核状态',
    duty_status     VARCHAR(16) DEFAULT 'OFF_DUTY'      NOT NULL COMMENT '在岗状态',
    last_login_at   DATETIME                             NULL COMMENT '最近登录时间',
    created_at      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete       TINYINT     DEFAULT 0               NOT NULL COMMENT '逻辑删除',
    UNIQUE KEY uk_lifeguard_code (lifeguard_code),
    UNIQUE KEY uk_lifeguard_user_id (user_id),
    KEY idx_lifeguard_venue_status (venue_id, duty_status),
    CONSTRAINT fk_lifeguard_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_lifeguard_venue FOREIGN KEY (venue_id) REFERENCES venue (id)
) COMMENT '救生员表' ENGINE = InnoDB
                 DEFAULT CHARSET = utf8mb4
                 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lifeguard_duty_log
(
    id                BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    lifeguard_id      BIGINT                              NOT NULL COMMENT '救生员ID',
    action_type       VARCHAR(32)                         NOT NULL COMMENT '动作类型',
    leave_reason      VARCHAR(32)                          NULL COMMENT '离岗原因',
    planned_return_at DATETIME                             NULL COMMENT '预计返回时间',
    actual_return_at  DATETIME                             NULL COMMENT '实际返回时间',
    approved_by       BIGINT                               NULL COMMENT '审批管理员ID',
    created_at        DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    KEY idx_duty_lifeguard_created (lifeguard_id, created_at),
    CONSTRAINT fk_duty_lifeguard FOREIGN KEY (lifeguard_id) REFERENCES lifeguard (id),
    CONSTRAINT fk_duty_approved_user FOREIGN KEY (approved_by) REFERENCES sys_user (id)
) COMMENT '救生员上下岗日志表' ENGINE = InnoDB
                     DEFAULT CHARSET = utf8mb4
                     COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lifeguard_location_log
(
    id            BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    lifeguard_id  BIGINT                              NOT NULL COMMENT '救生员ID',
    venue_id      BIGINT                              NOT NULL COMMENT '场馆ID',
    longitude     DECIMAL(10, 6)                      NOT NULL COMMENT '经度',
    latitude      DECIMAL(10, 6)                      NOT NULL COMMENT '纬度',
    in_fence      TINYINT     DEFAULT 1               NOT NULL COMMENT '是否在围栏内',
    report_source VARCHAR(32) DEFAULT 'APP_GPS'       NOT NULL COMMENT '上报来源',
    reported_at   DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '上报时间',
    KEY idx_location_lifeguard_time (lifeguard_id, reported_at),
    KEY idx_location_venue_time (venue_id, reported_at),
    CONSTRAINT fk_location_lifeguard FOREIGN KEY (lifeguard_id) REFERENCES lifeguard (id),
    CONSTRAINT fk_location_venue FOREIGN KEY (venue_id) REFERENCES venue (id)
) COMMENT '救生员定位上报表' ENGINE = InnoDB
                     DEFAULT CHARSET = utf8mb4
                     COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS env_sensor_sample
(
    id                BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    venue_id          BIGINT                              NOT NULL COMMENT '场馆ID',
    zone_id           BIGINT                               NULL COMMENT '区域ID',
    sensor_code       VARCHAR(32)                         NOT NULL COMMENT '传感器编码',
    water_temperature DECIMAL(5, 2)                       NULL COMMENT '水温',
    humidity          DECIMAL(5, 2)                       NULL COMMENT '湿度',
    quality_flag      VARCHAR(16) DEFAULT 'NORMAL'        NOT NULL COMMENT '数据质量标记',
    sample_time       DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '采样时间',
    KEY idx_sensor_zone_time (zone_id, sample_time),
    KEY idx_sensor_venue_time (venue_id, sample_time),
    CONSTRAINT fk_sensor_venue FOREIGN KEY (venue_id) REFERENCES venue (id),
    CONSTRAINT fk_sensor_zone FOREIGN KEY (zone_id) REFERENCES venue_zone (id)
) COMMENT '环境传感器采样表' ENGINE = InnoDB
                     DEFAULT CHARSET = utf8mb4
                     COLLATE = utf8mb4_unicode_ci;

-- =========================
-- 4) 监控事件与报警
-- =========================

CREATE TABLE IF NOT EXISTS ai_stream_task
(
    id                BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    task_code         VARCHAR(64)                         NOT NULL COMMENT '任务编码',
    camera_id         BIGINT                              NOT NULL COMMENT '摄像头ID',
    stream_url        VARCHAR(512)                        NOT NULL COMMENT '流地址',
    frame_interval_ms INT          DEFAULT 200            NOT NULL COMMENT '抽帧间隔毫秒',
    callback_url      VARCHAR(256)                        NOT NULL COMMENT '回调地址',
    task_status       VARCHAR(16) DEFAULT 'PENDING'       NOT NULL COMMENT '任务状态',
    started_at        DATETIME                             NULL COMMENT '启动时间',
    stopped_at        DATETIME                             NULL COMMENT '停止时间',
    last_frame_at     DATETIME                             NULL COMMENT '最近处理帧时间',
    created_at        DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at        DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_task_code (task_code),
    KEY idx_task_camera_status (camera_id, task_status),
    CONSTRAINT fk_task_camera FOREIGN KEY (camera_id) REFERENCES camera_device (id)
) COMMENT 'AI流任务表' ENGINE = InnoDB
                DEFAULT CHARSET = utf8mb4
                COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS monitoring_event
(
    id                   BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    event_uid            VARCHAR(64)                         NOT NULL COMMENT '事件唯一ID(幂等)',
    camera_id            BIGINT                              NOT NULL COMMENT '摄像头ID',
    task_id              BIGINT                               NULL COMMENT '任务ID',
    event_type           VARCHAR(32)                         NOT NULL COMMENT '事件类型',
    risk_level           VARCHAR(16) DEFAULT 'MEDIUM'        NOT NULL COMMENT '风险等级',
    confidence           DECIMAL(5, 4) DEFAULT 0.0000        NOT NULL COMMENT '置信度',
    target_id            VARCHAR(64)                          NULL COMMENT '跟踪目标ID',
    pool_head_count      INT                                  NULL COMMENT '泳池人数',
    bbox_json            JSON                                 NULL COMMENT '标注框',
    position_desc        VARCHAR(256)                         NULL COMMENT '位置描述',
    emergency_contact_name  VARCHAR(64)                       NULL COMMENT '紧急联系人',
    emergency_contact_phone VARCHAR(20)                       NULL COMMENT '紧急联系人电话',
    incident_location    VARCHAR(256)                         NULL COMMENT '事发具体位置',
    video_stream_url     VARCHAR(512)                         NULL COMMENT '视频流地址',
    event_time           DATETIME                             NOT NULL COMMENT '事件发生时间',
    ext_json             JSON                                 NULL COMMENT '扩展字段',
    created_at           DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_event_uid (event_uid),
    KEY idx_event_camera_time (camera_id, event_time),
    KEY idx_event_type_time (event_type, event_time),
    CONSTRAINT fk_event_camera FOREIGN KEY (camera_id) REFERENCES camera_device (id),
    CONSTRAINT fk_event_task FOREIGN KEY (task_id) REFERENCES ai_stream_task (id)
) COMMENT '监控事件表' ENGINE = InnoDB
                 DEFAULT CHARSET = utf8mb4
                 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS alert_record
(
    id                     BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    alert_uid              VARCHAR(64)                         NOT NULL COMMENT '报警唯一ID',
    event_id               BIGINT                              NOT NULL COMMENT '事件ID',
    camera_id              BIGINT                              NOT NULL COMMENT '摄像头ID',
    venue_id               BIGINT                              NOT NULL COMMENT '场馆ID',
    lifeguard_id           BIGINT                               NULL COMMENT '处理救生员ID',
    alert_type             VARCHAR(32) DEFAULT 'DROWING'       NOT NULL COMMENT '报警类型',
    alert_status           VARCHAR(16) DEFAULT 'PENDING'       NOT NULL COMMENT '报警状态',
    emergency_contact_name VARCHAR(64)                          NULL COMMENT '紧急联系人姓名',
    emergency_contact_phone VARCHAR(20)                         NULL COMMENT '紧急联系人电话',
    incident_location      VARCHAR(256)                         NULL COMMENT '事发具体位置',
    video_stream_url       VARCHAR(512)                         NULL COMMENT '视频流地址',
    detection_result       VARCHAR(512)                         NULL COMMENT '算法识别结果/检测摘要',
    pushed_to_app          TINYINT     DEFAULT 0               NOT NULL COMMENT '是否推送到App',
    pushed_to_pc           TINYINT     DEFAULT 0               NOT NULL COMMENT '是否推送到PC',
    first_push_time        DATETIME                             NULL COMMENT '首次推送时间',
    resolved_time          DATETIME                             NULL COMMENT '处理完成时间',
    created_at             DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at             DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_alert_uid (alert_uid),
    KEY idx_alert_status_time (alert_status, created_at),
    CONSTRAINT fk_alert_event FOREIGN KEY (event_id) REFERENCES monitoring_event (id),
    CONSTRAINT fk_alert_camera FOREIGN KEY (camera_id) REFERENCES camera_device (id),
    CONSTRAINT fk_alert_venue FOREIGN KEY (venue_id) REFERENCES venue (id),
    CONSTRAINT fk_alert_lifeguard FOREIGN KEY (lifeguard_id) REFERENCES lifeguard (id)
) COMMENT '报警记录表' ENGINE = InnoDB
                 DEFAULT CHARSET = utf8mb4
                 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS system_notice_config
(
    id                          BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    off_duty_threshold_sec      INT          DEFAULT 60            NOT NULL COMMENT '脱岗告警阈值(秒)',
    device_offline_threshold_sec INT         DEFAULT 180           NOT NULL COMMENT '设备离线阈值(秒)',
    drowning_alert_threshold_sec INT         DEFAULT 3             NOT NULL COMMENT '溺水持续判定阈值(秒)',
    created_at                  DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at                  DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '系统通知配置表(全局)' ENGINE = InnoDB
                    DEFAULT CHARSET = utf8mb4
                    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS alert_disposal
(
    id               BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    alert_id         BIGINT                              NOT NULL COMMENT '报警ID',
    operator_user_id BIGINT                              NOT NULL COMMENT '操作人用户ID',
    operator_role    VARCHAR(32)                         NOT NULL COMMENT '操作人角色',
    action_type      VARCHAR(32)                         NOT NULL COMMENT '动作类型',
    action_note      VARCHAR(512)                         NULL COMMENT '处理备注',
    action_time      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '处理时间',
    KEY idx_disposal_alert_time (alert_id, action_time),
    CONSTRAINT fk_disposal_alert FOREIGN KEY (alert_id) REFERENCES alert_record (id),
    CONSTRAINT fk_disposal_user FOREIGN KEY (operator_user_id) REFERENCES sys_user (id)
) COMMENT '报警处置表' ENGINE = InnoDB
                   DEFAULT CHARSET = utf8mb4
                   COLLATE = utf8mb4_unicode_ci;

-- =========================
-- 5) 审计与统计
-- =========================

CREATE TABLE IF NOT EXISTS system_audit_log
(
    id               BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    trace_id         VARCHAR(64)                         NOT NULL COMMENT '链路ID',
    log_category     VARCHAR(32) DEFAULT 'OP'            NOT NULL COMMENT '日志分类',
    operator_id      BIGINT                               NULL COMMENT '操作人ID',
    operator_name    VARCHAR(64)                          NULL COMMENT '操作人名称',
    client_ip        VARCHAR(64)                          NULL COMMENT '客户端IP',
    request_uri      VARCHAR(256)                         NULL COMMENT '请求URI',
    request_method   VARCHAR(16)                          NULL COMMENT '请求方法',
    request_body     LONGTEXT                             NULL COMMENT '请求体快照',
    response_code    INT                                  NULL COMMENT '响应业务码',
    response_message VARCHAR(256)                         NULL COMMENT '响应消息',
    cost_ms          INT                                  NULL COMMENT '耗时毫秒',
    created_at       DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    KEY idx_audit_category_time (log_category, created_at),
    KEY idx_audit_operator_time (operator_id, created_at)
) COMMENT '系统审计日志表' ENGINE = InnoDB
                     DEFAULT CHARSET = utf8mb4
                     COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stats_snapshot
(
    id            BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    granularity   VARCHAR(8) DEFAULT 'HOUR'            NOT NULL COMMENT '粒度:HOUR/DAY',
    snapshot_date DATE                                 NOT NULL COMMENT '快照日期',
    snapshot_hour TINYINT                               NULL COMMENT '小时(0-23,DAT粒度可空)',
    venue_id      BIGINT                                NULL COMMENT '场馆ID',
    metric_type   VARCHAR(32)                           NOT NULL COMMENT '指标类型',
    metric_key    VARCHAR(64)                           NOT NULL COMMENT '指标键',
    metric_value  DECIMAL(18, 4) DEFAULT 0.0000        NOT NULL COMMENT '指标值',
    dimension_json JSON                                  NULL COMMENT '维度JSON',
    created_at    DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_snapshot_key (granularity, snapshot_date, snapshot_hour, venue_id, metric_type, metric_key),
    KEY idx_snapshot_venue_date (venue_id, snapshot_date),
    CONSTRAINT fk_snapshot_venue FOREIGN KEY (venue_id) REFERENCES venue (id)
) COMMENT '统计快照表' ENGINE = InnoDB
                 DEFAULT CHARSET = utf8mb4
                 COLLATE = utf8mb4_unicode_ci;

-- =========================
-- 6) 初始管理员账号（按要求放在建表脚本）
-- 账号: admin
-- 密码: 123456
-- 哈希: md5('springboot' + '123456') = a384380c440fb620eb080df5cbfcd0f0
-- =========================

INSERT INTO system_notice_config (id, off_duty_threshold_sec, device_offline_threshold_sec, drowning_alert_threshold_sec)
VALUES (1, 60, 180, 3)
ON DUPLICATE KEY UPDATE
    off_duty_threshold_sec = VALUES(off_duty_threshold_sec),
    device_offline_threshold_sec = VALUES(device_offline_threshold_sec),
    drowning_alert_threshold_sec = VALUES(drowning_alert_threshold_sec);

INSERT INTO sys_role (id, role_code, role_name, permission_json, status, is_delete)
VALUES (1, 'SUPER_ADMIN', '平台超级管理员', JSON_ARRAY('ALL:*'), 1, 0),
       (2, 'VENUE_ADMIN', '场馆管理员', JSON_ARRAY('dashboard:view', 'camera:*', 'lifeguard:*', 'alert:*', 'stats:view'), 1, 0),
       (3, 'LIFEGUARD', '救生员', JSON_ARRAY('alert:receive', 'alert:handle', 'location:report', 'duty:update'), 1, 0),
       (4, 'USER', '普通用户', JSON_ARRAY('profile:view', 'profile:update'), 1, 0)
ON DUPLICATE KEY UPDATE
    role_name       = VALUES(role_name),
    permission_json = VALUES(permission_json),
    status          = VALUES(status),
    is_delete       = VALUES(is_delete);

INSERT INTO sys_user (id, username, password_hash, display_name, phone, email, status, failed_login_count, lock_until,
                      force_change_password, last_login_at, is_delete)
VALUES (1000001, 'admin', 'a384380c440fb620eb080df5cbfcd0f0', '平台管理员', '13800000001', 'admin@swimsafe.local', 1,
        0, NULL, 1, NULL, 0)
ON DUPLICATE KEY UPDATE
    password_hash         = VALUES(password_hash),
    display_name          = VALUES(display_name),
    phone                 = VALUES(phone),
    email                 = VALUES(email),
    status                = VALUES(status),
    failed_login_count    = VALUES(failed_login_count),
    lock_until            = VALUES(lock_until),
    force_change_password = VALUES(force_change_password),
    is_delete             = VALUES(is_delete);

INSERT INTO sys_user_role (id, user_id, role_id)
VALUES (1, 1000001, 1)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    role_id = VALUES(role_id);
