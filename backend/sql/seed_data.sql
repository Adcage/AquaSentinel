-- AI防溺水系统种子数据脚本
-- 要求：每张表至少20条，数据风格真实，可重复执行

USE ai_drowning;

SET NAMES utf8mb4;

-- =========================
-- 1) sys_role (20条)
-- =========================
INSERT INTO sys_role (id, role_code, role_name, permission_json, status, is_delete)
VALUES (1, 'SUPER_ADMIN', '平台超级管理员', JSON_ARRAY('ALL:*'), 1, 0),
       (2, 'VENUE_ADMIN', '场馆管理员', JSON_ARRAY('dashboard:view', 'camera:*', 'lifeguard:*', 'alert:*', 'stats:view'), 1, 0),
       (3, 'LIFEGUARD', '救生员', JSON_ARRAY('alert:receive', 'alert:handle', 'location:report', 'duty:update'), 1, 0),
       (4, 'USER', '普通用户', JSON_ARRAY('profile:view', 'profile:update'), 1, 0),
       (5, 'REGION_SUPERVISOR', '区域督导', JSON_ARRAY('dashboard:view', 'venue:view', 'alert:list'), 1, 0),
       (6, 'RISK_ANALYST', '风险分析员', JSON_ARRAY('stats:*', 'alert:list', 'event:list'), 1, 0),
       (7, 'DEVICE_ENGINEER', '设备工程师', JSON_ARRAY('camera:list', 'camera:update', 'maintenance:*'), 1, 0),
       (8, 'SHIFT_CAPTAIN', '值班队长', JSON_ARRAY('alert:assign', 'lifeguard:list', 'duty:view'), 1, 0),
       (9, 'EMERGENCY_COORD', '应急协同员', JSON_ARRAY('alert:list', 'alert:update', 'notice:send'), 1, 0),
       (10, 'OPS_AUDITOR', '运维审计员', JSON_ARRAY('audit:list', 'audit:export'), 1, 0),
       (11, 'VENUE_VIEWER', '场馆只读账号', JSON_ARRAY('dashboard:view', 'camera:view', 'stats:view'), 1, 0),
       (12, 'TRAINING_MANAGER', '培训管理员', JSON_ARRAY('lifeguard:list', 'training:*'), 1, 0),
       (13, 'SENSOR_OPERATOR', '传感器运维', JSON_ARRAY('sensor:list', 'sensor:update'), 1, 0),
       (14, 'REPORT_OPERATOR', '报表专员', JSON_ARRAY('stats:view', 'stats:export'), 1, 0),
       (15, 'PATROL_STAFF', '巡检人员', JSON_ARRAY('camera:list', 'maintenance:add'), 1, 0),
       (16, 'SECURITY_OFFICER', '安全专员', JSON_ARRAY('alert:list', 'alert:review'), 1, 0),
       (17, 'TEST_ACCOUNT', '测试账号', JSON_ARRAY('dashboard:view'), 1, 0),
       (18, 'RESERVE_LIFEGUARD', '后备救生员', JSON_ARRAY('alert:receive', 'location:report'), 1, 0),
       (19, 'VENUE_ASSISTANT', '场馆助理', JSON_ARRAY('camera:view', 'lifeguard:view'), 1, 0),
       (20, 'DATA_EXPORTER', '数据导出员', JSON_ARRAY('stats:export', 'audit:export'), 1, 0)
ON DUPLICATE KEY UPDATE
    role_name       = VALUES(role_name),
    permission_json = VALUES(permission_json),
    status          = VALUES(status),
    is_delete       = VALUES(is_delete);

-- =========================
-- 2) sys_user (>=20条)
-- =========================
INSERT INTO sys_user (id, username, password_hash, display_name, phone, email, status, failed_login_count, lock_until,
                      force_change_password, last_login_at, is_delete)
VALUES (1000002, 'venue.admin.pudong', 'e7a217b556e3b00b410e644b324cb430', '韩锐', '13910000002', 'hanrui@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 08:10:00', 0),
       (1000003, 'venue.admin.xuhui', 'e7a217b556e3b00b410e644b324cb430', '陆谨言', '13910000003', 'lujinyan@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 08:12:00', 0),
       (1000004, 'venue.admin.jingan', 'e7a217b556e3b00b410e644b324cb430', '沈哲', '13910000004', 'shenzhe@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 08:13:00', 0),
       (1000005, 'venue.admin.minhang', 'e7a217b556e3b00b410e644b324cb430', '顾川', '13910000005', 'guchuan@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 08:15:00', 0),
       (1000006, 'lg.zhouwentao', 'e7a217b556e3b00b410e644b324cb430', '周文涛', '13910000006', 'zhouwentao@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:00:00', 0),
       (1000007, 'lg.chenyifan', 'e7a217b556e3b00b410e644b324cb430', '陈逸帆', '13910000007', 'chenyifan@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:01:00', 0),
       (1000008, 'lg.linxiaohan', 'e7a217b556e3b00b410e644b324cb430', '林晓寒', '13910000008', 'linxiaohan@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:02:00', 0),
       (1000009, 'lg.fengzilin', 'e7a217b556e3b00b410e644b324cb430', '冯子林', '13910000009', 'fengzilin@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:03:00', 0),
       (1000010, 'lg.guanlin', 'e7a217b556e3b00b410e644b324cb430', '关霖', '13910000010', 'guanlin@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:04:00', 0),
       (1000011, 'lg.heyucheng', 'e7a217b556e3b00b410e644b324cb430', '何宇澄', '13910000011', 'heyucheng@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:05:00', 0),
       (1000012, 'lg.songxinyi', 'e7a217b556e3b00b410e644b324cb430', '宋欣怡', '13910000012', 'songxinyi@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:06:00', 0),
       (1000013, 'lg.qiaoyun', 'e7a217b556e3b00b410e644b324cb430', '乔韵', '13910000013', 'qiaoyun@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:07:00', 0),
       (1000014, 'lg.duhaoran', 'e7a217b556e3b00b410e644b324cb430', '杜浩然', '13910000014', 'duhaoran@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:08:00', 0),
       (1000015, 'lg.zhaokai', 'e7a217b556e3b00b410e644b324cb430', '赵恺', '13910000015', 'zhaokai@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:09:00', 0),
       (1000016, 'lg.fangrui', 'e7a217b556e3b00b410e644b324cb430', '方睿', '13910000016', 'fangrui@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:10:00', 0),
       (1000017, 'lg.liuyiqing', 'e7a217b556e3b00b410e644b324cb430', '刘奕清', '13910000017', 'liuyiqing@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:11:00', 0),
       (1000018, 'lg.majiahao', 'e7a217b556e3b00b410e644b324cb430', '马嘉昊', '13910000018', 'majiahao@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:12:00', 0),
       (1000019, 'lg.pengyuchen', 'e7a217b556e3b00b410e644b324cb430', '彭宇辰', '13910000019', 'pengyuchen@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:13:00', 0),
       (1000020, 'lg.wangxinyi', 'e7a217b556e3b00b410e644b324cb430', '王心怡', '13910000020', 'wangxinyi@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:14:00', 0),
       (1000021, 'lg.shenjianing', 'e7a217b556e3b00b410e644b324cb430', '沈佳宁', '13910000021', 'shenjianing@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:15:00', 0),
       (1000022, 'lg.chenhao', 'e7a217b556e3b00b410e644b324cb430', '陈昊', '13910000022', 'chenhao@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:16:00', 0),
       (1000023, 'lg.yuanle', 'e7a217b556e3b00b410e644b324cb430', '袁乐', '13910000023', 'yuanle@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:17:00', 0),
       (1000024, 'lg.xuweichen', 'e7a217b556e3b00b410e644b324cb430', '徐维辰', '13910000024', 'xuweichen@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:18:00', 0),
       (1000025, 'lg.jinshuo', 'e7a217b556e3b00b410e644b324cb430', '金硕', '13910000025', 'jinshuo@swimsafe.local', 1, 0, NULL, 0, '2026-03-22 09:19:00', 0)
ON DUPLICATE KEY UPDATE
    display_name          = VALUES(display_name),
    phone                 = VALUES(phone),
    email                 = VALUES(email),
    status                = VALUES(status),
    failed_login_count    = VALUES(failed_login_count),
    lock_until            = VALUES(lock_until),
    force_change_password = VALUES(force_change_password),
    last_login_at         = VALUES(last_login_at),
    is_delete             = VALUES(is_delete);

-- =========================
-- 3) sys_user_role (25条)
-- =========================
INSERT INTO sys_user_role (id, user_id, role_id)
VALUES (2, 1000002, 2),
       (3, 1000003, 2),
       (4, 1000004, 2),
       (5, 1000005, 2),
       (6, 1000006, 3),
       (7, 1000007, 3),
       (8, 1000008, 3),
       (9, 1000009, 3),
       (10, 1000010, 3),
       (11, 1000011, 3),
       (12, 1000012, 3),
       (13, 1000013, 3),
       (14, 1000014, 3),
       (15, 1000015, 3),
       (16, 1000016, 3),
       (17, 1000017, 3),
       (18, 1000018, 3),
       (19, 1000019, 3),
       (20, 1000020, 3),
       (21, 1000021, 3),
       (22, 1000022, 3),
       (23, 1000023, 3),
       (24, 1000024, 3),
       (25, 1000025, 3),
       (26, 1000001, 1)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    role_id = VALUES(role_id);

-- =========================
-- 4) auth_refresh_token (20条)
-- =========================
INSERT INTO auth_refresh_token (id, user_id, refresh_token_hash, device_id, client_type, client_version, ip_address,
                                expires_at, revoked, revoked_at, revoke_reason, last_used_at, created_at)
VALUES (1, 1000001, 'rt_admin_01_hash', 'pc-ops-center-001', 'pc', '1.3.2', '10.10.1.10', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 08:20:00'),
       (2, 1000002, 'rt_va_pd_01_hash', 'pc-pudong-001', 'pc', '1.3.2', '10.10.1.21', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 08:25:00'),
       (3, 1000003, 'rt_va_xh_01_hash', 'pc-xuhui-001', 'pc', '1.3.2', '10.10.1.22', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 08:25:30'),
       (4, 1000004, 'rt_va_ja_01_hash', 'pc-jingan-001', 'pc', '1.3.2', '10.10.1.23', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 08:26:00'),
       (5, 1000005, 'rt_va_mh_01_hash', 'pc-minhang-001', 'pc', '1.3.2', '10.10.1.24', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 08:27:00'),
       (6, 1000006, 'rt_lg_06_hash', 'android-lg-06', 'android', '1.0.0', '10.10.2.6', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:00:00'),
       (7, 1000007, 'rt_lg_07_hash', 'android-lg-07', 'android', '1.0.0', '10.10.2.7', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:01:00'),
       (8, 1000008, 'rt_lg_08_hash', 'android-lg-08', 'android', '1.0.0', '10.10.2.8', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:02:00'),
       (9, 1000009, 'rt_lg_09_hash', 'android-lg-09', 'android', '1.0.0', '10.10.2.9', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:03:00'),
       (10, 1000010, 'rt_lg_10_hash', 'android-lg-10', 'android', '1.0.0', '10.10.2.10', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:04:00'),
       (11, 1000011, 'rt_lg_11_hash', 'android-lg-11', 'android', '1.0.0', '10.10.2.11', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:05:00'),
       (12, 1000012, 'rt_lg_12_hash', 'android-lg-12', 'android', '1.0.0', '10.10.2.12', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:06:00'),
       (13, 1000013, 'rt_lg_13_hash', 'android-lg-13', 'android', '1.0.0', '10.10.2.13', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:07:00'),
       (14, 1000014, 'rt_lg_14_hash', 'android-lg-14', 'android', '1.0.0', '10.10.2.14', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:08:00'),
       (15, 1000015, 'rt_lg_15_hash', 'android-lg-15', 'android', '1.0.0', '10.10.2.15', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:09:00'),
       (16, 1000016, 'rt_lg_16_hash', 'android-lg-16', 'android', '1.0.0', '10.10.2.16', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:10:00'),
       (17, 1000017, 'rt_lg_17_hash', 'android-lg-17', 'android', '1.0.0', '10.10.2.17', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:11:00'),
       (18, 1000018, 'rt_lg_18_hash', 'android-lg-18', 'android', '1.0.0', '10.10.2.18', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:12:00'),
       (19, 1000019, 'rt_lg_19_hash', 'android-lg-19', 'android', '1.0.0', '10.10.2.19', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:13:00'),
       (20, 1000020, 'rt_lg_20_hash', 'android-lg-20', 'android', '1.0.0', '10.10.2.20', '2026-04-20 23:59:59', 0, NULL, NULL, '2026-03-22 09:20:00', '2026-03-22 09:14:00')
ON DUPLICATE KEY UPDATE
    refresh_token_hash = VALUES(refresh_token_hash),
    device_id          = VALUES(device_id),
    client_type        = VALUES(client_type),
    client_version     = VALUES(client_version),
    ip_address         = VALUES(ip_address),
    expires_at         = VALUES(expires_at),
    revoked            = VALUES(revoked),
    revoked_at         = VALUES(revoked_at),
    revoke_reason      = VALUES(revoke_reason),
    last_used_at       = VALUES(last_used_at);

-- =========================
-- 5) venue (20条)
-- =========================
INSERT INTO venue (id, venue_code, venue_name, address, contact_name, contact_phone, timezone, status, fence_geo_json, is_delete)
VALUES (2001, 'VEN-SH-PD', '浦东游泳中心', '浦东新区东方路1888号', '陈昊', '13818880001', 'Asia/Shanghai', 1, NULL, 0),
       (2002, 'VEN-SH-XH', '徐汇滨江泳馆', '徐汇区龙腾大道902号', '韩锐', '13818880002', 'Asia/Shanghai', 1, NULL, 0),
       (2003, 'VEN-SH-JA', '静安体育公园泳池', '静安区共和新路1268号', '陆谨言', '13818880003', 'Asia/Shanghai', 1, NULL, 0),
       (2004, 'VEN-SH-CN', '长宁天山水上中心', '长宁区天山路1555号', '顾川', '13818880004', 'Asia/Shanghai', 1, NULL, 0),
       (2005, 'VEN-SH-YP', '杨浦复兴岛游泳馆', '杨浦区共青路110号', '沈哲', '13818880005', 'Asia/Shanghai', 1, NULL, 0),
       (2006, 'VEN-SH-PT', '普陀桃浦全民健身馆', '普陀区真南路1199号', '方睿', '13818880006', 'Asia/Shanghai', 1, NULL, 0),
       (2007, 'VEN-SH-BS', '宝山顾村水上运动馆', '宝山区陆翔路2555号', '何宇澄', '13818880007', 'Asia/Shanghai', 1, NULL, 0),
       (2008, 'VEN-SH-MH', '闵行七宝游泳馆', '闵行区七莘路2188号', '刘奕清', '13818880008', 'Asia/Shanghai', 1, NULL, 0),
       (2009, 'VEN-SH-JD', '嘉定远香湖泳训中心', '嘉定区阿克苏路399号', '马嘉昊', '13818880009', 'Asia/Shanghai', 1, NULL, 0),
       (2010, 'VEN-SH-JS', '金山滨海游泳馆', '金山区龙山路688号', '彭宇辰', '13818880010', 'Asia/Shanghai', 1, NULL, 0),
       (2011, 'VEN-SH-SJ', '松江大学城泳馆', '松江区文汇路888号', '宋欣怡', '13818880011', 'Asia/Shanghai', 1, NULL, 0),
       (2012, 'VEN-SH-QP', '青浦淀山湖水上中心', '青浦区淀山湖大道1588号', '乔韵', '13818880012', 'Asia/Shanghai', 1, NULL, 0),
       (2013, 'VEN-SH-FX', '奉贤南桥游泳馆', '奉贤区人民南路1166号', '杜浩然', '13818880013', 'Asia/Shanghai', 1, NULL, 0),
       (2014, 'VEN-SH-CM', '崇明东滩水上训练中心', '崇明区东滩大道699号', '赵恺', '13818880014', 'Asia/Shanghai', 1, NULL, 0),
       (2015, 'VEN-SH-HK', '虹口北外滩泳池', '虹口区东大名路518号', '沈佳宁', '13818880015', 'Asia/Shanghai', 1, NULL, 0),
       (2016, 'VEN-SH-HP', '黄浦世博水上馆', '黄浦区蒙自路120号', '林晓寒', '13818880016', 'Asia/Shanghai', 1, NULL, 0),
       (2017, 'VEN-SH-LG', '临港滴水湖游泳馆', '浦东新区环湖西二路99号', '冯子林', '13818880017', 'Asia/Shanghai', 1, NULL, 0),
       (2018, 'VEN-SH-NH', '南汇新城泳训中心', '浦东新区申港大道328号', '关霖', '13818880018', 'Asia/Shanghai', 1, NULL, 0),
       (2019, 'VEN-SH-SB', '世博园区综合泳馆', '浦东新区世博大道201号', '王心怡', '13818880019', 'Asia/Shanghai', 1, NULL, 0),
       (2020, 'VEN-SH-XJH', '徐家汇全民游泳馆', '徐汇区宜山路540号', '袁乐', '13818880020', 'Asia/Shanghai', 1, NULL, 0)
ON DUPLICATE KEY UPDATE
    venue_name    = VALUES(venue_name),
    address       = VALUES(address),
    contact_name  = VALUES(contact_name),
    contact_phone = VALUES(contact_phone),
    timezone      = VALUES(timezone),
    status        = VALUES(status),
    fence_geo_json = VALUES(fence_geo_json),
    is_delete     = VALUES(is_delete);

-- =========================
-- 6) venue_zone (20条)
-- =========================
INSERT INTO venue_zone (id, venue_id, zone_code, zone_name, zone_type, geo_json, risk_level, is_delete)
VALUES (3001, 2001, 'ZONE-PD-DS-E3', '深水区东侧3号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.4801,31.2251],[121.4804,31.2251],[121.4804,31.2254],[121.4801,31.2254],[121.4801,31.2251]]]}', 'HIGH', 0),
       (3002, 2002, 'ZONE-XH-DS-W2', '深水区西侧2号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.4551,31.1811],[121.4554,31.1811],[121.4554,31.1814],[121.4551,31.1814],[121.4551,31.1811]]]}', 'HIGH', 0),
       (3003, 2003, 'ZONE-JA-JS-01', '竞赛池中部泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.4621,31.2561],[121.4624,31.2561],[121.4624,31.2564],[121.4621,31.2564],[121.4621,31.2561]]]}', 'MEDIUM', 0),
       (3004, 2004, 'ZONE-CN-QS-04', '浅水区南侧4号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.3921,31.2121],[121.3924,31.2121],[121.3924,31.2124],[121.3921,31.2124],[121.3921,31.2121]]]}', 'LOW', 0),
       (3005, 2005, 'ZONE-YP-DS-01', '深水区中段1号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.5341,31.3021],[121.5344,31.3021],[121.5344,31.3024],[121.5341,31.3024],[121.5341,31.3021]]]}', 'HIGH', 0),
       (3006, 2006, 'ZONE-PT-QS-02', '教学池北侧2号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.4011,31.2891],[121.4014,31.2891],[121.4014,31.2894],[121.4011,31.2894],[121.4011,31.2891]]]}', 'MEDIUM', 0),
       (3007, 2007, 'ZONE-BS-DS-05', '深水区外圈5号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.4311,31.3611],[121.4314,31.3611],[121.4314,31.3614],[121.4311,31.3614],[121.4311,31.3611]]]}', 'HIGH', 0),
       (3008, 2008, 'ZONE-MH-DS-E8', '深水区东侧8号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.3751,31.1621],[121.3754,31.1621],[121.3754,31.1624],[121.3751,31.1624],[121.3751,31.1621]]]}', 'HIGH', 0),
       (3009, 2009, 'ZONE-JD-QS-03', '浅水区西侧3号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.2651,31.3821],[121.2654,31.3821],[121.2654,31.3824],[121.2651,31.3824],[121.2651,31.3821]]]}', 'LOW', 0),
       (3010, 2010, 'ZONE-JS-DS-02', '深水区北侧2号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.3321,30.7351],[121.3324,30.7351],[121.3324,30.7354],[121.3321,30.7354],[121.3321,30.7351]]]}', 'MEDIUM', 0),
       (3011, 2011, 'ZONE-SJ-JS-06', '竞赛池南侧6号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.2131,31.0361],[121.2134,31.0361],[121.2134,31.0364],[121.2131,31.0364],[121.2131,31.0361]]]}', 'MEDIUM', 0),
       (3012, 2012, 'ZONE-QP-DS-04', '深水区中段4号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.1131,31.1451],[121.1134,31.1451],[121.1134,31.1454],[121.1131,31.1454],[121.1131,31.1451]]]}', 'HIGH', 0),
       (3013, 2013, 'ZONE-FX-QS-01', '教学池东侧1号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.4731,30.9161],[121.4734,30.9161],[121.4734,30.9164],[121.4731,30.9164],[121.4731,30.9161]]]}', 'LOW', 0),
       (3014, 2014, 'ZONE-CM-DS-01', '训练池主泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.9321,31.5621],[121.9324,31.5621],[121.9324,31.5624],[121.9321,31.5624],[121.9321,31.5621]]]}', 'MEDIUM', 0),
       (3015, 2015, 'ZONE-HK-DS-03', '深水区西侧3号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.5051,31.2481],[121.5054,31.2481],[121.5054,31.2484],[121.5051,31.2484],[121.5051,31.2481]]]}', 'HIGH', 0),
       (3016, 2016, 'ZONE-HP-QS-02', '浅水区北侧2号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.4761,31.2011],[121.4764,31.2011],[121.4764,31.2014],[121.4761,31.2014],[121.4761,31.2011]]]}', 'LOW', 0),
       (3017, 2017, 'ZONE-LG-DS-07', '深水区外圈7号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.9341,30.9001],[121.9344,30.9001],[121.9344,30.9004],[121.9341,30.9004],[121.9341,30.9001]]]}', 'HIGH', 0),
       (3018, 2018, 'ZONE-NH-JS-02', '竞赛池东侧2号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.8221,30.8981],[121.8224,30.8981],[121.8224,30.8984],[121.8221,30.8984],[121.8221,30.8981]]]}', 'MEDIUM', 0),
       (3019, 2019, 'ZONE-SB-DS-02', '深水区中段2号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.4921,31.1901],[121.4924,31.1901],[121.4924,31.1904],[121.4921,31.1904],[121.4921,31.1901]]]}', 'HIGH', 0),
       (3020, 2020, 'ZONE-XJH-QS-05', '教学池南侧5号泳道', 'POOL', '{"type":"Polygon","coordinates":[[[121.4371,31.1931],[121.4374,31.1931],[121.4374,31.1934],[121.4371,31.1934],[121.4371,31.1931]]]}', 'LOW', 0)
ON DUPLICATE KEY UPDATE
    venue_id    = VALUES(venue_id),
    zone_name   = VALUES(zone_name),
    zone_type   = VALUES(zone_type),
    geo_json    = VALUES(geo_json),
    risk_level  = VALUES(risk_level),
    is_delete   = VALUES(is_delete);

UPDATE venue v
JOIN venue_zone z ON z.id = (
    SELECT MIN(vz.id)
    FROM venue_zone vz
    WHERE vz.venue_id = v.id AND vz.is_delete = 0
)
SET v.fence_geo_json = JSON_OBJECT(
    'type', 'FeatureCollection',
    'features', JSON_ARRAY(
        JSON_OBJECT(
            'type', 'Feature',
            'geometry', z.geo_json,
            'properties', JSON_OBJECT()
        )
    )
)
WHERE v.is_delete = 0;

-- =========================
-- 7) camera_device (20条)
-- =========================
INSERT INTO camera_device (id, venue_id, zone_id, camera_code, camera_name, stream_url, protocol, device_status,
                           health_status, enabled, last_heartbeat_at, is_delete)
VALUES (5001, 2001, 3001, 'CAM-PD-0001', '浦东深水区东侧1号机', 'rtsp://10.10.10.1/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:10', 0),
       (5002, 2002, 3002, 'CAM-XH-0002', '徐汇深水区西侧2号机', 'rtsp://10.10.10.2/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:12', 0),
       (5003, 2003, 3003, 'CAM-JA-0003', '静安竞赛池中部机位', 'rtsp://10.10.10.3/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:15', 0),
       (5004, 2004, 3004, 'CAM-CN-0004', '长宁浅水区南侧机位', 'rtsp://10.10.10.4/live/1', 'RTSP', 'ONLINE', 'WARN', 1, '2026-03-22 10:00:16', 0),
       (5005, 2005, 3005, 'CAM-YP-0005', '杨浦深水区中段机位', 'rtsp://10.10.10.5/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:17', 0),
       (5006, 2006, 3006, 'CAM-PT-0006', '普陀教学池北侧机位', 'rtsp://10.10.10.6/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:18', 0),
       (5007, 2007, 3007, 'CAM-BS-0007', '宝山深水区外圈机位', 'rtsp://10.10.10.7/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:19', 0),
       (5008, 2008, 3008, 'CAM-MH-0008', '闵行深水区东侧8号机', 'rtsp://10.10.10.8/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:20', 0),
       (5009, 2009, 3009, 'CAM-JD-0009', '嘉定浅水区西侧机位', 'rtsp://10.10.10.9/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:21', 0),
       (5010, 2010, 3010, 'CAM-JS-0010', '金山深水区北侧机位', 'rtsp://10.10.10.10/live/1', 'RTSP', 'ONLINE', 'WARN', 1, '2026-03-22 10:00:22', 0),
       (5011, 2011, 3011, 'CAM-SJ-0011', '松江竞赛池南侧机位', 'rtsp://10.10.10.11/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:23', 0),
       (5012, 2012, 3012, 'CAM-QP-0012', '青浦深水区中段机位', 'rtsp://10.10.10.12/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:24', 0),
       (5013, 2013, 3013, 'CAM-FX-0013', '奉贤教学池东侧机位', 'rtsp://10.10.10.13/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:25', 0),
       (5014, 2014, 3014, 'CAM-CM-0014', '崇明训练池主机位', 'rtsp://10.10.10.14/live/1', 'RTSP', 'OFFLINE', 'ERROR', 1, '2026-03-22 09:45:00', 0),
       (5015, 2015, 3015, 'CAM-HK-0015', '虹口深水区西侧机位', 'rtsp://10.10.10.15/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:26', 0),
       (5016, 2016, 3016, 'CAM-HP-0016', '黄浦浅水区北侧机位', 'rtsp://10.10.10.16/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:27', 0),
       (5017, 2017, 3017, 'CAM-LG-0017', '临港深水区外圈机位', 'rtsp://10.10.10.17/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:28', 0),
       (5018, 2018, 3018, 'CAM-NH-0018', '南汇竞赛池东侧机位', 'rtsp://10.10.10.18/live/1', 'RTSP', 'ONLINE', 'WARN', 1, '2026-03-22 10:00:29', 0),
       (5019, 2019, 3019, 'CAM-SB-0019', '世博深水区中段机位', 'rtsp://10.10.10.19/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:30', 0),
       (5020, 2020, 3020, 'CAM-XJH-0020', '徐家汇教学池南侧机位', 'rtsp://10.10.10.20/live/1', 'RTSP', 'ONLINE', 'NORMAL', 1, '2026-03-22 10:00:31', 0)
ON DUPLICATE KEY UPDATE
    venue_id          = VALUES(venue_id),
    zone_id           = VALUES(zone_id),
    camera_name       = VALUES(camera_name),
    stream_url        = VALUES(stream_url),
    protocol          = VALUES(protocol),
    device_status     = VALUES(device_status),
    health_status     = VALUES(health_status),
    enabled           = VALUES(enabled),
    last_heartbeat_at = VALUES(last_heartbeat_at),
    is_delete         = VALUES(is_delete);

-- =========================
-- 8) camera_maintenance_log (20条)
-- =========================
INSERT INTO camera_maintenance_log (id, camera_id, maintenance_type, maintenance_content, maintained_by, maintained_at,
                                    next_maintenance_at)
VALUES (6001, 5001, 'CHECK', '完成镜头清洁与编码器温度校验，视频帧率稳定25fps', '赵立新', '2026-03-10 09:00:00', '2026-04-10 09:00:00'),
       (6002, 5002, 'CHECK', '更换网线接头，丢包率由2.1%降至0.3%', '韩飞', '2026-03-10 09:20:00', '2026-04-10 09:20:00'),
       (6003, 5003, 'REPAIR', '修复电源模块接触不良问题', '周启明', '2026-03-10 09:40:00', '2026-04-10 09:40:00'),
       (6004, 5004, 'CHECK', '校准曝光参数，降低水面反光', '顾宁', '2026-03-10 10:00:00', '2026-04-10 10:00:00'),
       (6005, 5005, 'CHECK', '完成镜头焦距复核，目标框稳定', '赵立新', '2026-03-10 10:20:00', '2026-04-10 10:20:00'),
       (6006, 5006, 'CHECK', '网络抖动排查完成，延迟稳定120ms', '韩飞', '2026-03-10 10:40:00', '2026-04-10 10:40:00'),
       (6007, 5007, 'REPAIR', '更换老化PoE供电模块', '周启明', '2026-03-10 11:00:00', '2026-04-10 11:00:00'),
       (6008, 5008, 'CHECK', '视角重定位，覆盖深水区盲角', '顾宁', '2026-03-10 11:20:00', '2026-04-10 11:20:00'),
       (6009, 5009, 'CHECK', '固件升级至v2.4.1', '赵立新', '2026-03-10 11:40:00', '2026-04-10 11:40:00'),
       (6010, 5010, 'CHECK', '夜间降噪参数重调', '韩飞', '2026-03-10 12:00:00', '2026-04-10 12:00:00'),
       (6011, 5011, 'CHECK', '完成镜头防水圈检查', '周启明', '2026-03-10 12:20:00', '2026-04-10 12:20:00'),
       (6012, 5012, 'CHECK', '检测RTSP码流稳定，关键帧间隔2秒', '顾宁', '2026-03-10 12:40:00', '2026-04-10 12:40:00'),
       (6013, 5013, 'REPAIR', '更换损坏镜头保护罩', '赵立新', '2026-03-10 13:00:00', '2026-04-10 13:00:00'),
       (6014, 5014, 'REPAIR', '现场断电后重启失败，待主板更换', '韩飞', '2026-03-10 13:20:00', '2026-03-25 13:20:00'),
       (6015, 5015, 'CHECK', '复核夜间补光效果', '周启明', '2026-03-10 13:40:00', '2026-04-10 13:40:00'),
       (6016, 5016, 'CHECK', '视频时钟同步误差修正至50ms以内', '顾宁', '2026-03-10 14:00:00', '2026-04-10 14:00:00'),
       (6017, 5017, 'CHECK', '完成设备端温度传感器校准', '赵立新', '2026-03-10 14:20:00', '2026-04-10 14:20:00'),
       (6018, 5018, 'CHECK', '恢复异常音频通道，保持静音输出', '韩飞', '2026-03-10 14:40:00', '2026-04-10 14:40:00'),
       (6019, 5019, 'CHECK', '关键帧丢失告警阈值调整', '周启明', '2026-03-10 15:00:00', '2026-04-10 15:00:00'),
       (6020, 5020, 'CHECK', '完成图像畸变参数矫正', '顾宁', '2026-03-10 15:20:00', '2026-04-10 15:20:00')
ON DUPLICATE KEY UPDATE
    camera_id           = VALUES(camera_id),
    maintenance_type    = VALUES(maintenance_type),
    maintenance_content = VALUES(maintenance_content),
    maintained_by       = VALUES(maintained_by),
    maintained_at       = VALUES(maintained_at),
    next_maintenance_at = VALUES(next_maintenance_at);

-- =========================
-- 9) lifeguard (20条)
-- =========================
INSERT INTO lifeguard (id, user_id, lifeguard_code, full_name, phone, venue_id, fence_geo_json, audit_status,
                       duty_status, last_login_at, is_delete)
VALUES (8001, 1000006, 'LG-PD-001', '周文涛', '13910000006', 2001, '{"type":"Polygon","coordinates":[[[121.4801,31.2251],[121.4805,31.2251],[121.4805,31.2255],[121.4801,31.2255],[121.4801,31.2251]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:00:00', 0),
       (8002, 1000007, 'LG-XH-001', '陈逸帆', '13910000007', 2002, '{"type":"Polygon","coordinates":[[[121.4551,31.1811],[121.4555,31.1811],[121.4555,31.1815],[121.4551,31.1815],[121.4551,31.1811]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:01:00', 0),
       (8003, 1000008, 'LG-JA-001', '林晓寒', '13910000008', 2003, '{"type":"Polygon","coordinates":[[[121.4621,31.2561],[121.4625,31.2561],[121.4625,31.2565],[121.4621,31.2565],[121.4621,31.2561]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:02:00', 0),
       (8004, 1000009, 'LG-CN-001', '冯子林', '13910000009', 2004, '{"type":"Polygon","coordinates":[[[121.3921,31.2121],[121.3925,31.2121],[121.3925,31.2125],[121.3921,31.2125],[121.3921,31.2121]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:03:00', 0),
       (8005, 1000010, 'LG-YP-001', '关霖', '13910000010', 2005, '{"type":"Polygon","coordinates":[[[121.5341,31.3021],[121.5345,31.3021],[121.5345,31.3025],[121.5341,31.3025],[121.5341,31.3021]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:04:00', 0),
       (8006, 1000011, 'LG-PT-001', '何宇澄', '13910000011', 2006, '{"type":"Polygon","coordinates":[[[121.4011,31.2891],[121.4015,31.2891],[121.4015,31.2895],[121.4011,31.2895],[121.4011,31.2891]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:05:00', 0),
       (8007, 1000012, 'LG-BS-001', '宋欣怡', '13910000012', 2007, '{"type":"Polygon","coordinates":[[[121.4311,31.3611],[121.4315,31.3611],[121.4315,31.3615],[121.4311,31.3615],[121.4311,31.3611]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:06:00', 0),
       (8008, 1000013, 'LG-MH-001', '乔韵', '13910000013', 2008, '{"type":"Polygon","coordinates":[[[121.3751,31.1621],[121.3755,31.1621],[121.3755,31.1625],[121.3751,31.1625],[121.3751,31.1621]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:07:00', 0),
       (8009, 1000014, 'LG-JD-001', '杜浩然', '13910000014', 2009, '{"type":"Polygon","coordinates":[[[121.2651,31.3821],[121.2655,31.3821],[121.2655,31.3825],[121.2651,31.3825],[121.2651,31.3821]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:08:00', 0),
       (8010, 1000015, 'LG-JS-001', '赵恺', '13910000015', 2010, '{"type":"Polygon","coordinates":[[[121.3321,30.7351],[121.3325,30.7351],[121.3325,30.7355],[121.3321,30.7355],[121.3321,30.7351]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:09:00', 0),
       (8011, 1000016, 'LG-SJ-001', '方睿', '13910000016', 2011, '{"type":"Polygon","coordinates":[[[121.2131,31.0361],[121.2135,31.0361],[121.2135,31.0365],[121.2131,31.0365],[121.2131,31.0361]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:10:00', 0),
       (8012, 1000017, 'LG-QP-001', '刘奕清', '13910000017', 2012, '{"type":"Polygon","coordinates":[[[121.1131,31.1451],[121.1135,31.1451],[121.1135,31.1455],[121.1131,31.1455],[121.1131,31.1451]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:11:00', 0),
       (8013, 1000018, 'LG-FX-001', '马嘉昊', '13910000018', 2013, '{"type":"Polygon","coordinates":[[[121.4731,30.9161],[121.4735,30.9161],[121.4735,30.9165],[121.4731,30.9165],[121.4731,30.9161]]]}', 'APPROVED', 'OFF_DUTY', '2026-03-22 09:12:00', 0),
       (8014, 1000019, 'LG-CM-001', '彭宇辰', '13910000019', 2014, '{"type":"Polygon","coordinates":[[[121.9321,31.5621],[121.9325,31.5621],[121.9325,31.5625],[121.9321,31.5625],[121.9321,31.5621]]]}', 'APPROVED', 'OFF_DUTY', '2026-03-22 09:13:00', 0),
       (8015, 1000020, 'LG-HK-001', '王心怡', '13910000020', 2015, '{"type":"Polygon","coordinates":[[[121.5051,31.2481],[121.5055,31.2481],[121.5055,31.2485],[121.5051,31.2485],[121.5051,31.2481]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:14:00', 0),
       (8016, 1000021, 'LG-HP-001', '沈佳宁', '13910000021', 2016, '{"type":"Polygon","coordinates":[[[121.4761,31.2011],[121.4765,31.2011],[121.4765,31.2015],[121.4761,31.2015],[121.4761,31.2011]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:15:00', 0),
       (8017, 1000022, 'LG-LG-001', '陈昊', '13910000022', 2017, '{"type":"Polygon","coordinates":[[[121.9341,30.9001],[121.9345,30.9001],[121.9345,30.9005],[121.9341,30.9005],[121.9341,30.9001]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:16:00', 0),
       (8018, 1000023, 'LG-NH-001', '袁乐', '13910000023', 2018, '{"type":"Polygon","coordinates":[[[121.8221,30.8981],[121.8225,30.8981],[121.8225,30.8985],[121.8221,30.8985],[121.8221,30.8981]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:17:00', 0),
       (8019, 1000024, 'LG-SB-001', '徐维辰', '13910000024', 2019, '{"type":"Polygon","coordinates":[[[121.4921,31.1901],[121.4925,31.1901],[121.4925,31.1905],[121.4921,31.1905],[121.4921,31.1901]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:18:00', 0),
       (8020, 1000025, 'LG-XJH-001', '金硕', '13910000025', 2020, '{"type":"Polygon","coordinates":[[[121.4371,31.1931],[121.4375,31.1931],[121.4375,31.1935],[121.4371,31.1935],[121.4371,31.1931]]]}', 'APPROVED', 'ON_DUTY', '2026-03-22 09:19:00', 0)
ON DUPLICATE KEY UPDATE
    user_id        = VALUES(user_id),
    full_name      = VALUES(full_name),
    phone          = VALUES(phone),
    venue_id       = VALUES(venue_id),
    fence_geo_json = VALUES(fence_geo_json),
    audit_status   = VALUES(audit_status),
    duty_status    = VALUES(duty_status),
    last_login_at  = VALUES(last_login_at),
    is_delete      = VALUES(is_delete);

-- =========================
-- 10) lifeguard_duty_log (20条)
-- =========================
INSERT INTO lifeguard_duty_log (id, lifeguard_id, action_type, leave_reason, planned_return_at, actual_return_at,
                                approved_by, created_at)
VALUES (9001, 8001, 'ON_DUTY', NULL, NULL, NULL, 1000002, '2026-03-22 08:58:00'),
       (9002, 8002, 'ON_DUTY', NULL, NULL, NULL, 1000003, '2026-03-22 08:59:00'),
       (9003, 8003, 'ON_DUTY', NULL, NULL, NULL, 1000004, '2026-03-22 09:00:00'),
       (9004, 8004, 'ON_DUTY', NULL, NULL, NULL, 1000005, '2026-03-22 09:01:00'),
       (9005, 8005, 'ON_DUTY', NULL, NULL, NULL, 1000002, '2026-03-22 09:02:00'),
       (9006, 8006, 'ON_DUTY', NULL, NULL, NULL, 1000003, '2026-03-22 09:03:00'),
       (9007, 8007, 'ON_DUTY', NULL, NULL, NULL, 1000004, '2026-03-22 09:04:00'),
       (9008, 8008, 'ON_DUTY', NULL, NULL, NULL, 1000005, '2026-03-22 09:05:00'),
       (9009, 8009, 'ON_DUTY', NULL, NULL, NULL, 1000002, '2026-03-22 09:06:00'),
       (9010, 8010, 'ON_DUTY', NULL, NULL, NULL, 1000003, '2026-03-22 09:07:00'),
       (9011, 8011, 'ON_DUTY', NULL, NULL, NULL, 1000004, '2026-03-22 09:08:00'),
       (9012, 8012, 'ON_DUTY', NULL, NULL, NULL, 1000005, '2026-03-22 09:09:00'),
       (9013, 8013, 'LEAVE_REPORT', '换班', '2026-03-22 11:30:00', '2026-03-22 11:28:00', 1000002, '2026-03-22 11:00:00'),
       (9014, 8014, 'LEAVE_REPORT', '如厕', '2026-03-22 11:20:00', NULL, 1000003, '2026-03-22 11:05:00'),
       (9015, 8015, 'ON_DUTY', NULL, NULL, NULL, 1000004, '2026-03-22 09:10:00'),
       (9016, 8016, 'ON_DUTY', NULL, NULL, NULL, 1000005, '2026-03-22 09:11:00'),
       (9017, 8017, 'ON_DUTY', NULL, NULL, NULL, 1000002, '2026-03-22 09:12:00'),
       (9018, 8018, 'ON_DUTY', NULL, NULL, NULL, 1000003, '2026-03-22 09:13:00'),
       (9019, 8019, 'ON_DUTY', NULL, NULL, NULL, 1000004, '2026-03-22 09:14:00'),
       (9020, 8020, 'ON_DUTY', NULL, NULL, NULL, 1000005, '2026-03-22 09:15:00')
ON DUPLICATE KEY UPDATE
    lifeguard_id      = VALUES(lifeguard_id),
    action_type       = VALUES(action_type),
    leave_reason      = VALUES(leave_reason),
    planned_return_at = VALUES(planned_return_at),
    actual_return_at  = VALUES(actual_return_at),
    approved_by       = VALUES(approved_by),
    created_at        = VALUES(created_at);

-- =========================
-- 11) lifeguard_location_log (20条)
-- =========================
INSERT INTO lifeguard_location_log (id, lifeguard_id, venue_id, longitude, latitude, in_fence, report_source, reported_at)
VALUES (10001, 8001, 2001, 121.480312, 31.225341, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10002, 8002, 2002, 121.455298, 31.181265, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10003, 8003, 2003, 121.462255, 31.256332, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10004, 8004, 2004, 121.392288, 31.212244, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10005, 8005, 2005, 121.534233, 31.302289, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10006, 8006, 2006, 121.401276, 31.289198, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10007, 8007, 2007, 121.431342, 31.361287, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10008, 8008, 2008, 121.375266, 31.162312, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10009, 8009, 2009, 121.265341, 31.382265, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10010, 8010, 2010, 121.332288, 30.735299, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10011, 8011, 2011, 121.213331, 31.036278, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10012, 8012, 2012, 121.113244, 31.145321, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10013, 8013, 2013, 121.473266, 30.916279, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10014, 8014, 2014, 121.932411, 31.562218, 0, 'APP_GPS', '2026-03-22 10:00:00'),
       (10015, 8015, 2015, 121.505267, 31.248271, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10016, 8016, 2016, 121.476289, 31.201233, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10017, 8017, 2017, 121.934245, 30.900244, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10018, 8018, 2018, 121.822299, 30.898311, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10019, 8019, 2019, 121.492255, 31.190255, 1, 'APP_GPS', '2026-03-22 10:00:00'),
       (10020, 8020, 2020, 121.437244, 31.193288, 1, 'APP_GPS', '2026-03-22 10:00:00')
ON DUPLICATE KEY UPDATE
    lifeguard_id  = VALUES(lifeguard_id),
    venue_id      = VALUES(venue_id),
    longitude     = VALUES(longitude),
    latitude      = VALUES(latitude),
    in_fence      = VALUES(in_fence),
    report_source = VALUES(report_source),
    reported_at   = VALUES(reported_at);

-- =========================
-- 12) env_sensor_sample (20条)
-- =========================
INSERT INTO env_sensor_sample (id, venue_id, zone_id, sensor_code, water_temperature, humidity, quality_flag, sample_time)
VALUES (11001, 2001, 3001, 'SNS-PD-01', 27.80, 62.10, 'NORMAL', '2026-03-22 09:55:00'),
       (11002, 2002, 3002, 'SNS-XH-01', 27.60, 61.20, 'NORMAL', '2026-03-22 09:55:00'),
       (11003, 2003, 3003, 'SNS-JA-01', 27.50, 60.50, 'NORMAL', '2026-03-22 09:55:00'),
       (11004, 2004, 3004, 'SNS-CN-01', 27.20, 63.00, 'NORMAL', '2026-03-22 09:55:00'),
       (11005, 2005, 3005, 'SNS-YP-01', 27.90, 62.60, 'NORMAL', '2026-03-22 09:55:00'),
       (11006, 2006, 3006, 'SNS-PT-01', 27.10, 64.20, 'NORMAL', '2026-03-22 09:55:00'),
       (11007, 2007, 3007, 'SNS-BS-01', 28.00, 65.10, 'NORMAL', '2026-03-22 09:55:00'),
       (11008, 2008, 3008, 'SNS-MH-01', 27.70, 61.80, 'NORMAL', '2026-03-22 09:55:00'),
       (11009, 2009, 3009, 'SNS-JD-01', 27.30, 60.90, 'NORMAL', '2026-03-22 09:55:00'),
       (11010, 2010, 3010, 'SNS-JS-01', 27.40, 63.30, 'NORMAL', '2026-03-22 09:55:00'),
       (11011, 2011, 3011, 'SNS-SJ-01', 27.60, 62.40, 'NORMAL', '2026-03-22 09:55:00'),
       (11012, 2012, 3012, 'SNS-QP-01', 27.90, 64.00, 'NORMAL', '2026-03-22 09:55:00'),
       (11013, 2013, 3013, 'SNS-FX-01', 27.20, 61.10, 'NORMAL', '2026-03-22 09:55:00'),
       (11014, 2014, 3014, 'SNS-CM-01', 27.00, 66.20, 'WARN', '2026-03-22 09:55:00'),
       (11015, 2015, 3015, 'SNS-HK-01', 27.50, 62.80, 'NORMAL', '2026-03-22 09:55:00'),
       (11016, 2016, 3016, 'SNS-HP-01', 27.30, 63.00, 'NORMAL', '2026-03-22 09:55:00'),
       (11017, 2017, 3017, 'SNS-LG-01', 27.80, 65.50, 'NORMAL', '2026-03-22 09:55:00'),
       (11018, 2018, 3018, 'SNS-NH-01', 27.40, 64.80, 'NORMAL', '2026-03-22 09:55:00'),
       (11019, 2019, 3019, 'SNS-SB-01', 27.60, 62.20, 'NORMAL', '2026-03-22 09:55:00'),
       (11020, 2020, 3020, 'SNS-XJH-01', 27.10, 61.90, 'NORMAL', '2026-03-22 09:55:00')
ON DUPLICATE KEY UPDATE
    venue_id          = VALUES(venue_id),
    zone_id           = VALUES(zone_id),
    sensor_code       = VALUES(sensor_code),
    water_temperature = VALUES(water_temperature),
    humidity          = VALUES(humidity),
    quality_flag      = VALUES(quality_flag),
    sample_time       = VALUES(sample_time);

-- =========================
-- 13) ai_stream_task (20条)
-- =========================
INSERT INTO ai_stream_task (id, task_code, camera_id, stream_url, frame_interval_ms, callback_url,
                            task_status, started_at, stopped_at, last_frame_at)
VALUES (12001, 'TASK-CAM-5001-20260322', 5001, 'rtsp://10.10.10.1/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12002, 'TASK-CAM-5002-20260322', 5002, 'rtsp://10.10.10.2/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12003, 'TASK-CAM-5003-20260322', 5003, 'rtsp://10.10.10.3/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12004, 'TASK-CAM-5004-20260322', 5004, 'rtsp://10.10.10.4/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12005, 'TASK-CAM-5005-20260322', 5005, 'rtsp://10.10.10.5/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12006, 'TASK-CAM-5006-20260322', 5006, 'rtsp://10.10.10.6/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12007, 'TASK-CAM-5007-20260322', 5007, 'rtsp://10.10.10.7/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12008, 'TASK-CAM-5008-20260322', 5008, 'rtsp://10.10.10.8/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12009, 'TASK-CAM-5009-20260322', 5009, 'rtsp://10.10.10.9/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12010, 'TASK-CAM-5010-20260322', 5010, 'rtsp://10.10.10.10/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12011, 'TASK-CAM-5011-20260322', 5011, 'rtsp://10.10.10.11/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12012, 'TASK-CAM-5012-20260322', 5012, 'rtsp://10.10.10.12/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12013, 'TASK-CAM-5013-20260322', 5013, 'rtsp://10.10.10.13/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12014, 'TASK-CAM-5014-20260322', 5014, 'rtsp://10.10.10.14/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'FAILED', '2026-03-22 09:00:00', '2026-03-22 09:46:00', '2026-03-22 09:45:00'),
       (12015, 'TASK-CAM-5015-20260322', 5015, 'rtsp://10.10.10.15/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12016, 'TASK-CAM-5016-20260322', 5016, 'rtsp://10.10.10.16/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12017, 'TASK-CAM-5017-20260322', 5017, 'rtsp://10.10.10.17/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12018, 'TASK-CAM-5018-20260322', 5018, 'rtsp://10.10.10.18/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12019, 'TASK-CAM-5019-20260322', 5019, 'rtsp://10.10.10.19/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00'),
       (12020, 'TASK-CAM-5020-20260322', 5020, 'rtsp://10.10.10.20/live/1', 200, 'http://127.0.0.1:8101/api/internal/ai/events', 'RUNNING', '2026-03-22 09:00:00', NULL, '2026-03-22 10:00:00')
ON DUPLICATE KEY UPDATE
    camera_id         = VALUES(camera_id),
    stream_url        = VALUES(stream_url),
    frame_interval_ms = VALUES(frame_interval_ms),
    callback_url      = VALUES(callback_url),
    task_status       = VALUES(task_status),
    started_at        = VALUES(started_at),
    stopped_at        = VALUES(stopped_at),
    last_frame_at     = VALUES(last_frame_at);

-- =========================
-- 14) monitoring_event (20条)
-- =========================
INSERT INTO monitoring_event (id, event_uid, camera_id, task_id, event_type, risk_level, confidence, target_id,
                              pool_head_count, bbox_json, position_desc, emergency_contact_name,
                              emergency_contact_phone, incident_location, video_stream_url, event_time, ext_json)
VALUES (13001, 'EVT-20260322-0001', 5001, 12001, 'DROWING', 'HIGH', 0.9630, 'track_101', 18,
        '{"xMin":312,"yMin":124,"xMax":418,"yMax":301}', '浦东游泳中心深水区东侧', '陈昊', '13818880001', '浦东游泳中心-深水区东侧3号泳道', 'http://stream.swimsafe.local/flv/CAM-PD-0001', '2026-03-22 09:36:10', '{"poseScore":0.91,"tempScore":0.72,"durationScore":0.88}'),
       (13002, 'EVT-20260322-0002', 5002, 12002, 'OVERCROWD', 'MEDIUM', 0.8610, 'track_205', 27,
        '{"xMin":288,"yMin":110,"xMax":390,"yMax":280}', '徐汇滨江泳馆深水区西侧', '韩锐', '13818880002', '徐汇滨江泳馆-深水区西侧2号泳道', 'http://stream.swimsafe.local/flv/CAM-XH-0002', '2026-03-22 09:37:12', '{"threshold":25}'),
       (13003, 'EVT-20260322-0003', 5003, 12003, 'DROWING', 'HIGH', 0.9470, 'track_310', 14,
        '{"xMin":300,"yMin":100,"xMax":405,"yMax":290}', '静安体育公园泳池中部', '陆谨言', '13818880003', '静安体育公园泳池-竞赛池中部泳道', 'http://stream.swimsafe.local/flv/CAM-JA-0003', '2026-03-22 09:38:08', '{"poseScore":0.88,"tempScore":0.69,"durationScore":0.91}'),
       (13004, 'EVT-20260322-0004', 5004, 12004, 'OFF_POST', 'HIGH', 0.9330, 'track_402', 12,
        '{"xMin":120,"yMin":90,"xMax":210,"yMax":260}', '长宁天山水上中心浅水区南侧', '顾川', '13818880004', '长宁天山水上中心-浅水区南侧4号泳道', 'http://stream.swimsafe.local/flv/CAM-CN-0004', '2026-03-22 09:39:16', '{"offPostMinutes":2}'),
       (13005, 'EVT-20260322-0005', 5005, 12005, 'DROWING', 'HIGH', 0.9510, 'track_518', 16,
        '{"xMin":305,"yMin":121,"xMax":419,"yMax":304}', '杨浦复兴岛游泳馆深水区中段', '沈哲', '13818880005', '杨浦复兴岛游泳馆-深水区中段1号泳道', 'http://stream.swimsafe.local/flv/CAM-YP-0005', '2026-03-22 09:40:02', '{"poseScore":0.90,"tempScore":0.70,"durationScore":0.86}'),
       (13006, 'EVT-20260322-0006', 5006, 12006, 'OVERCROWD', 'MEDIUM', 0.8230, 'track_621', 24,
        '{"xMin":260,"yMin":108,"xMax":360,"yMax":278}', '普陀桃浦全民健身馆教学池北侧', '方睿', '13818880006', '普陀桃浦全民健身馆-教学池北侧2号泳道', 'http://stream.swimsafe.local/flv/CAM-PT-0006', '2026-03-22 09:41:22', '{"threshold":22}'),
       (13007, 'EVT-20260322-0007', 5007, 12007, 'DROWING', 'HIGH', 0.9440, 'track_709', 19,
        '{"xMin":301,"yMin":126,"xMax":423,"yMax":312}', '宝山顾村水上运动馆外圈', '何宇澄', '13818880007', '宝山顾村水上运动馆-深水区外圈5号泳道', 'http://stream.swimsafe.local/flv/CAM-BS-0007', '2026-03-22 09:42:35', '{"poseScore":0.89,"tempScore":0.74,"durationScore":0.85}'),
       (13008, 'EVT-20260322-0008', 5008, 12008, 'DROWING', 'HIGH', 0.9680, 'track_812', 20,
        '{"xMin":320,"yMin":118,"xMax":431,"yMax":308}', '闵行七宝游泳馆深水区东侧', '刘奕清', '13818880008', '闵行七宝游泳馆-深水区东侧8号泳道', 'http://stream.swimsafe.local/flv/CAM-MH-0008', '2026-03-22 09:43:10', '{"poseScore":0.93,"tempScore":0.71,"durationScore":0.90}'),
       (13009, 'EVT-20260322-0009', 5009, 12009, 'OFF_POST', 'MEDIUM', 0.8510, 'track_905', 11,
        '{"xMin":210,"yMin":88,"xMax":301,"yMax":256}', '嘉定远香湖泳训中心浅水区', '马嘉昊', '13818880009', '嘉定远香湖泳训中心-浅水区西侧3号泳道', 'http://stream.swimsafe.local/flv/CAM-JD-0009', '2026-03-22 09:44:44', '{"offPostMinutes":1}'),
       (13010, 'EVT-20260322-0010', 5010, 12010, 'DROWING', 'HIGH', 0.9420, 'track_1011', 15,
        '{"xMin":302,"yMin":119,"xMax":417,"yMax":296}', '金山滨海游泳馆北侧', '彭宇辰', '13818880010', '金山滨海游泳馆-深水区北侧2号泳道', 'http://stream.swimsafe.local/flv/CAM-JS-0010', '2026-03-22 09:45:12', '{"poseScore":0.87,"tempScore":0.68,"durationScore":0.92}'),
       (13011, 'EVT-20260322-0011', 5011, 12011, 'OVERCROWD', 'MEDIUM', 0.8180, 'track_1112', 26,
        '{"xMin":255,"yMin":106,"xMax":350,"yMax":274}', '松江大学城泳馆竞赛池', '宋欣怡', '13818880011', '松江大学城泳馆-竞赛池南侧6号泳道', 'http://stream.swimsafe.local/flv/CAM-SJ-0011', '2026-03-22 09:46:03', '{"threshold":24}'),
       (13012, 'EVT-20260322-0012', 5012, 12012, 'DROWING', 'HIGH', 0.9550, 'track_1214', 18,
        '{"xMin":318,"yMin":124,"xMax":428,"yMax":306}', '青浦淀山湖水上中心中段', '乔韵', '13818880012', '青浦淀山湖水上中心-深水区中段4号泳道', 'http://stream.swimsafe.local/flv/CAM-QP-0012', '2026-03-22 09:47:21', '{"poseScore":0.91,"tempScore":0.73,"durationScore":0.89}'),
       (13013, 'EVT-20260322-0013', 5013, 12013, 'OFF_POST', 'MEDIUM', 0.8340, 'track_1319', 10,
        '{"xMin":190,"yMin":82,"xMax":281,"yMax":248}', '奉贤南桥游泳馆教学池', '杜浩然', '13818880013', '奉贤南桥游泳馆-教学池东侧1号泳道', 'http://stream.swimsafe.local/flv/CAM-FX-0013', '2026-03-22 09:48:32', '{"offPostMinutes":1}'),
       (13014, 'EVT-20260322-0014', 5014, 12014, 'DEVICE_ERROR', 'HIGH', 0.9900, 'track_1401', 0,
        '{"xMin":0,"yMin":0,"xMax":0,"yMax":0}', '崇明东滩训练中心主机位', '赵恺', '13818880014', '崇明东滩水上训练中心-训练池主泳道', 'http://stream.swimsafe.local/flv/CAM-CM-0014', '2026-03-22 09:49:02', '{"error":"camera_offline"}'),
       (13015, 'EVT-20260322-0015', 5015, 12015, 'DROWING', 'HIGH', 0.9460, 'track_1518', 17,
        '{"xMin":309,"yMin":115,"xMax":416,"yMax":300}', '虹口北外滩泳池西侧', '沈佳宁', '13818880015', '虹口北外滩泳池-深水区西侧3号泳道', 'http://stream.swimsafe.local/flv/CAM-HK-0015', '2026-03-22 09:50:13', '{"poseScore":0.88,"tempScore":0.75,"durationScore":0.87}'),
       (13016, 'EVT-20260322-0016', 5016, 12016, 'OVERCROWD', 'MEDIUM', 0.8010, 'track_1612', 23,
        '{"xMin":250,"yMin":102,"xMax":343,"yMax":270}', '黄浦世博水上馆浅水区', '林晓寒', '13818880016', '黄浦世博水上馆-浅水区北侧2号泳道', 'http://stream.swimsafe.local/flv/CAM-HP-0016', '2026-03-22 09:51:33', '{"threshold":21}'),
       (13017, 'EVT-20260322-0017', 5017, 12017, 'DROWING', 'HIGH', 0.9520, 'track_1707', 16,
        '{"xMin":311,"yMin":120,"xMax":420,"yMax":305}', '临港滴水湖游泳馆外圈', '冯子林', '13818880017', '临港滴水湖游泳馆-深水区外圈7号泳道', 'http://stream.swimsafe.local/flv/CAM-LG-0017', '2026-03-22 09:52:08', '{"poseScore":0.90,"tempScore":0.70,"durationScore":0.91}'),
       (13018, 'EVT-20260322-0018', 5018, 12018, 'OFF_POST', 'MEDIUM', 0.8420, 'track_1804', 13,
        '{"xMin":202,"yMin":89,"xMax":295,"yMax":252}', '南汇新城泳训中心东侧', '关霖', '13818880018', '南汇新城泳训中心-竞赛池东侧2号泳道', 'http://stream.swimsafe.local/flv/CAM-NH-0018', '2026-03-22 09:53:19', '{"offPostMinutes":1}'),
       (13019, 'EVT-20260322-0019', 5019, 12019, 'DROWING', 'HIGH', 0.9590, 'track_1911', 18,
        '{"xMin":317,"yMin":123,"xMax":429,"yMax":309}', '世博园区综合泳馆中段', '王心怡', '13818880019', '世博园区综合泳馆-深水区中段2号泳道', 'http://stream.swimsafe.local/flv/CAM-SB-0019', '2026-03-22 09:54:41', '{"poseScore":0.92,"tempScore":0.71,"durationScore":0.90}'),
       (13020, 'EVT-20260322-0020', 5020, 12020, 'OVERCROWD', 'MEDIUM', 0.8090, 'track_2015', 22,
        '{"xMin":248,"yMin":101,"xMax":340,"yMax":268}', '徐家汇全民游泳馆教学池', '袁乐', '13818880020', '徐家汇全民游泳馆-教学池南侧5号泳道', 'http://stream.swimsafe.local/flv/CAM-XJH-0020', '2026-03-22 09:55:03', '{"threshold":20}')
ON DUPLICATE KEY UPDATE
    camera_id              = VALUES(camera_id),
    task_id                = VALUES(task_id),
    event_type             = VALUES(event_type),
    risk_level             = VALUES(risk_level),
    confidence             = VALUES(confidence),
    target_id              = VALUES(target_id),
    pool_head_count        = VALUES(pool_head_count),
    bbox_json              = VALUES(bbox_json),
    position_desc          = VALUES(position_desc),
    emergency_contact_name = VALUES(emergency_contact_name),
    emergency_contact_phone = VALUES(emergency_contact_phone),
    incident_location      = VALUES(incident_location),
    video_stream_url       = VALUES(video_stream_url),
    event_time             = VALUES(event_time),
    ext_json               = VALUES(ext_json);

-- =========================
-- 15) alert_record (20条)
-- =========================
INSERT INTO alert_record (id, alert_uid, event_id, camera_id, venue_id, lifeguard_id, alert_type, alert_status,
                           emergency_contact_name, emergency_contact_phone, incident_location, video_stream_url,
                           detection_result, pushed_to_app, pushed_to_pc, first_push_time, resolved_time, created_at)
VALUES (14001, 'ALT-20260322-0001', 13001, 5001, 2001, 8001, 'DROWING', 'PROCESSING', '陈昊', '13818880001', '浦东游泳中心-深水区东侧3号泳道', 'http://stream.swimsafe.local/flv/CAM-PD-0001', '疑似溺水，目标长时间低头漂浮', 1, 1, '2026-03-22 09:36:11', NULL, '2026-03-22 09:36:11'),
       (14002, 'ALT-20260322-0002', 13002, 5002, 2002, 8002, 'OVERCROWD', 'DONE', '韩锐', '13818880002', '徐汇滨江泳馆-深水区西侧2号泳道', 'http://stream.swimsafe.local/flv/CAM-XH-0002', '人数超过阈值', 1, 1, '2026-03-22 09:37:13', '2026-03-22 09:42:00', '2026-03-22 09:37:13'),
       (14003, 'ALT-20260322-0003', 13003, 5003, 2003, 8003, 'DROWING', 'PROCESSING', '陆谨言', '13818880003', '静安体育公园泳池-竞赛池中部泳道', 'http://stream.swimsafe.local/flv/CAM-JA-0003', '疑似溺水，持续下沉动作', 1, 1, '2026-03-22 09:38:09', NULL, '2026-03-22 09:38:09'),
       (14004, 'ALT-20260322-0004', 13004, 5004, 2004, 8004, 'OFF_POST', 'DONE', '顾川', '13818880004', '长宁天山水上中心-浅水区南侧4号泳道', 'http://stream.swimsafe.local/flv/CAM-CN-0004', '救生员离岗超过阈值', 1, 1, '2026-03-22 09:39:17', '2026-03-22 09:44:00', '2026-03-22 09:39:17'),
       (14005, 'ALT-20260322-0005', 13005, 5005, 2005, 8005, 'DROWING', 'PENDING', '沈哲', '13818880005', '杨浦复兴岛游泳馆-深水区中段1号泳道', 'http://stream.swimsafe.local/flv/CAM-YP-0005', '疑似溺水，目标无明显划水', 1, 1, '2026-03-22 09:40:03', NULL, '2026-03-22 09:40:03'),
       (14006, 'ALT-20260322-0006', 13006, 5006, 2006, 8006, 'OVERCROWD', 'DONE', '方睿', '13818880006', '普陀桃浦全民健身馆-教学池北侧2号泳道', 'http://stream.swimsafe.local/flv/CAM-PT-0006', '人数接近上限', 1, 1, '2026-03-22 09:41:23', '2026-03-22 09:43:30', '2026-03-22 09:41:23'),
       (14007, 'ALT-20260322-0007', 13007, 5007, 2007, 8007, 'DROWING', 'PROCESSING', '何宇澄', '13818880007', '宝山顾村水上运动馆-深水区外圈5号泳道', 'http://stream.swimsafe.local/flv/CAM-BS-0007', '疑似溺水，姿态异常', 1, 1, '2026-03-22 09:42:36', NULL, '2026-03-22 09:42:36'),
       (14008, 'ALT-20260322-0008', 13008, 5008, 2008, 8008, 'DROWING', 'PROCESSING', '刘奕清', '13818880008', '闵行七宝游泳馆-深水区东侧8号泳道', 'http://stream.swimsafe.local/flv/CAM-MH-0008', '疑似溺水，长时间静止', 1, 1, '2026-03-22 09:43:11', NULL, '2026-03-22 09:43:11'),
       (14009, 'ALT-20260322-0009', 13009, 5009, 2009, 8009, 'OFF_POST', 'DONE', '马嘉昊', '13818880009', '嘉定远香湖泳训中心-浅水区西侧3号泳道', 'http://stream.swimsafe.local/flv/CAM-JD-0009', '救生员短时离岗', 1, 1, '2026-03-22 09:44:45', '2026-03-22 09:47:10', '2026-03-22 09:44:45'),
       (14010, 'ALT-20260322-0010', 13010, 5010, 2010, 8010, 'DROWING', 'PENDING', '彭宇辰', '13818880010', '金山滨海游泳馆-深水区北侧2号泳道', 'http://stream.swimsafe.local/flv/CAM-JS-0010', '疑似溺水，目标轨迹不稳定', 1, 1, '2026-03-22 09:45:13', NULL, '2026-03-22 09:45:13'),
       (14011, 'ALT-20260322-0011', 13011, 5011, 2011, 8011, 'OVERCROWD', 'DONE', '宋欣怡', '13818880011', '松江大学城泳馆-竞赛池南侧6号泳道', 'http://stream.swimsafe.local/flv/CAM-SJ-0011', '人数峰值超限', 1, 1, '2026-03-22 09:46:04', '2026-03-22 09:49:00', '2026-03-22 09:46:04'),
       (14012, 'ALT-20260322-0012', 13012, 5012, 2012, 8012, 'DROWING', 'PROCESSING', '乔韵', '13818880012', '青浦淀山湖水上中心-深水区中段4号泳道', 'http://stream.swimsafe.local/flv/CAM-QP-0012', '疑似溺水，动作幅度减小', 1, 1, '2026-03-22 09:47:22', NULL, '2026-03-22 09:47:22'),
       (14013, 'ALT-20260322-0013', 13013, 5013, 2013, 8013, 'OFF_POST', 'DONE', '杜浩然', '13818880013', '奉贤南桥游泳馆-教学池东侧1号泳道', 'http://stream.swimsafe.local/flv/CAM-FX-0013', '救生员未按时回岗', 1, 1, '2026-03-22 09:48:33', '2026-03-22 09:51:00', '2026-03-22 09:48:33'),
       (14014, 'ALT-20260322-0014', 13014, 5014, 2014, 8014, 'DEVICE_ERROR', 'DONE', '赵恺', '13818880014', '崇明东滩水上训练中心-训练池主泳道', 'http://stream.swimsafe.local/flv/CAM-CM-0014', '摄像头离线', 1, 1, '2026-03-22 09:49:03', '2026-03-22 09:52:00', '2026-03-22 09:49:03'),
       (14015, 'ALT-20260322-0015', 13015, 5015, 2015, 8015, 'DROWING', 'PENDING', '沈佳宁', '13818880015', '虹口北外滩泳池-深水区西侧3号泳道', 'http://stream.swimsafe.local/flv/CAM-HK-0015', '疑似溺水，头部长时间没入水面', 1, 1, '2026-03-22 09:50:14', NULL, '2026-03-22 09:50:14'),
       (14016, 'ALT-20260322-0016', 13016, 5016, 2016, 8016, 'OVERCROWD', 'DONE', '林晓寒', '13818880016', '黄浦世博水上馆-浅水区北侧2号泳道', 'http://stream.swimsafe.local/flv/CAM-HP-0016', '人数超过可控值', 1, 1, '2026-03-22 09:51:34', '2026-03-22 09:53:20', '2026-03-22 09:51:34'),
       (14017, 'ALT-20260322-0017', 13017, 5017, 2017, 8017, 'DROWING', 'PROCESSING', '冯子林', '13818880017', '临港滴水湖游泳馆-深水区外圈7号泳道', 'http://stream.swimsafe.local/flv/CAM-LG-0017', '疑似溺水，目标停滞', 1, 1, '2026-03-22 09:52:09', NULL, '2026-03-22 09:52:09'),
       (14018, 'ALT-20260322-0018', 13018, 5018, 2018, 8018, 'OFF_POST', 'DONE', '关霖', '13818880018', '南汇新城泳训中心-竞赛池东侧2号泳道', 'http://stream.swimsafe.local/flv/CAM-NH-0018', '离岗告警', 1, 1, '2026-03-22 09:53:20', '2026-03-22 09:55:20', '2026-03-22 09:53:20'),
       (14019, 'ALT-20260322-0019', 13019, 5019, 2019, 8019, 'DROWING', 'PENDING', '王心怡', '13818880019', '世博园区综合泳馆-深水区中段2号泳道', 'http://stream.swimsafe.local/flv/CAM-SB-0019', '疑似溺水，需人工复核', 1, 1, '2026-03-22 09:54:42', NULL, '2026-03-22 09:54:42'),
       (14020, 'ALT-20260322-0020', 13020, 5020, 2020, 8020, 'OVERCROWD', 'DONE', '袁乐', '13818880020', '徐家汇全民游泳馆-教学池南侧5号泳道', 'http://stream.swimsafe.local/flv/CAM-XJH-0020', '人数拥挤', 1, 1, '2026-03-22 09:55:04', '2026-03-22 09:57:30', '2026-03-22 09:55:04')
ON DUPLICATE KEY UPDATE
    event_id               = VALUES(event_id),
    camera_id              = VALUES(camera_id),
    venue_id               = VALUES(venue_id),
    lifeguard_id           = VALUES(lifeguard_id),
    alert_type             = VALUES(alert_type),
    alert_status           = VALUES(alert_status),
    emergency_contact_name = VALUES(emergency_contact_name),
    emergency_contact_phone = VALUES(emergency_contact_phone),
    incident_location      = VALUES(incident_location),
    video_stream_url       = VALUES(video_stream_url),
    detection_result       = VALUES(detection_result),
    pushed_to_app          = VALUES(pushed_to_app),
    pushed_to_pc           = VALUES(pushed_to_pc),
    first_push_time        = VALUES(first_push_time),
    resolved_time          = VALUES(resolved_time),
    created_at             = VALUES(created_at);

-- =========================
-- 16) alert_disposal (20条)
-- =========================
INSERT INTO alert_disposal (id, alert_id, operator_user_id, operator_role, action_type, action_note, action_time)
VALUES (15001, 14001, 1000006, 'LIFEGUARD', 'CONFIRM', '已接警，前往深水区东侧', '2026-03-22 09:36:30'),
       (15002, 14002, 1000007, 'LIFEGUARD', 'DONE', '已完成人员疏导并恢复秩序', '2026-03-22 09:42:00'),
       (15003, 14003, 1000008, 'LIFEGUARD', 'CONFIRM', '已接警，现场核查中', '2026-03-22 09:38:30'),
       (15004, 14004, 1000009, 'LIFEGUARD', 'DONE', '离岗救生员已返回岗位', '2026-03-22 09:44:00'),
       (15005, 14005, 1000002, 'VENUE_ADMIN', 'ASSIGN', '指派周文涛优先处置', '2026-03-22 09:40:20'),
       (15006, 14006, 1000011, 'LIFEGUARD', 'DONE', '泳池人数恢复到安全阈值', '2026-03-22 09:43:30'),
       (15007, 14007, 1000012, 'LIFEGUARD', 'CONFIRM', '已到达警戒位置', '2026-03-22 09:42:50'),
       (15008, 14008, 1000013, 'LIFEGUARD', 'CONFIRM', '正在进行二次确认', '2026-03-22 09:43:35'),
       (15009, 14009, 1000014, 'LIFEGUARD', 'DONE', '已恢复围栏内在岗状态', '2026-03-22 09:47:10'),
       (15010, 14010, 1000015, 'LIFEGUARD', 'CONFIRM', '已接警，赶往北侧2号泳道', '2026-03-22 09:45:40'),
       (15011, 14011, 1000016, 'LIFEGUARD', 'DONE', '超员情况解除', '2026-03-22 09:49:00'),
       (15012, 14012, 1000017, 'LIFEGUARD', 'CONFIRM', '已开展救援预案流程', '2026-03-22 09:47:45'),
       (15013, 14013, 1000018, 'LIFEGUARD', 'DONE', '报备已补录，风险解除', '2026-03-22 09:51:00'),
       (15014, 14014, 1000003, 'VENUE_ADMIN', 'DONE', '设备故障工单已转运维组', '2026-03-22 09:52:00'),
       (15015, 14015, 1000020, 'LIFEGUARD', 'CONFIRM', '已到达现场进行观察', '2026-03-22 09:50:35'),
       (15016, 14016, 1000021, 'LIFEGUARD', 'DONE', '现场人员密度正常', '2026-03-22 09:53:20'),
       (15017, 14017, 1000022, 'LIFEGUARD', 'CONFIRM', '已联动另一名救生员处置', '2026-03-22 09:52:30'),
       (15018, 14018, 1000023, 'LIFEGUARD', 'DONE', '离岗人员已返回并签到', '2026-03-22 09:55:20'),
       (15019, 14019, 1000004, 'VENUE_ADMIN', 'ASSIGN', '指派徐维辰与金硕协同处置', '2026-03-22 09:55:00'),
       (15020, 14020, 1000025, 'LIFEGUARD', 'DONE', '教学池负荷已降至可控范围', '2026-03-22 09:57:30')
ON DUPLICATE KEY UPDATE
    alert_id         = VALUES(alert_id),
    operator_user_id = VALUES(operator_user_id),
    operator_role    = VALUES(operator_role),
    action_type      = VALUES(action_type),
    action_note      = VALUES(action_note),
    action_time      = VALUES(action_time);

-- =========================
-- 17) system_audit_log (20条)
-- =========================
INSERT INTO system_audit_log (id, trace_id, log_category, operator_id, operator_name, client_ip, request_uri,
                              request_method, request_body, response_code, response_message, cost_ms, created_at)
VALUES (16001, 'TRC-20260322-0001', 'LOGIN', 1000001, '平台管理员', '10.10.1.10', '/api/auth/admin/login', 'POST', '{"username":"admin"}', 0, 'ok', 46, '2026-03-22 08:20:00'),
       (16002, 'TRC-20260322-0002', 'OP', 1000002, '韩锐', '10.10.1.21', '/api/cameras/list/page', 'POST', '{"current":1}', 0, 'ok', 28, '2026-03-22 08:31:00'),
       (16003, 'TRC-20260322-0003', 'OP', 1000002, '韩锐', '10.10.1.21', '/api/monitor/tasks/start', 'POST', '{"cameraId":5001}', 0, 'ok', 65, '2026-03-22 09:00:00'),
       (16004, 'TRC-20260322-0004', 'OP', 1000003, '陆谨言', '10.10.1.22', '/api/monitor/tasks/start', 'POST', '{"cameraId":5002}', 0, 'ok', 63, '2026-03-22 09:00:01'),
       (16005, 'TRC-20260322-0005', 'AI_CALLBACK', NULL, NULL, '127.0.0.1', '/api/internal/ai/events', 'POST', '{"eventUid":"EVT-20260322-0001"}', 0, 'ok', 24, '2026-03-22 09:36:11'),
       (16006, 'TRC-20260322-0006', 'ALERT', 1000006, '周文涛', '10.10.2.6', '/api/alerts/action', 'POST', '{"alertId":14001,"actionType":"CONFIRM"}', 0, 'ok', 31, '2026-03-22 09:36:30'),
       (16007, 'TRC-20260322-0007', 'AI_CALLBACK', NULL, NULL, '127.0.0.1', '/api/internal/ai/events', 'POST', '{"eventUid":"EVT-20260322-0003"}', 0, 'ok', 23, '2026-03-22 09:38:09'),
       (16008, 'TRC-20260322-0008', 'ALERT', 1000002, '韩锐', '10.10.1.21', '/api/alerts/assign', 'POST', '{"alertId":14005,"lifeguardId":8001}', 0, 'ok', 34, '2026-03-22 09:40:20'),
       (16009, 'TRC-20260322-0009', 'AI_CALLBACK', NULL, NULL, '127.0.0.1', '/api/internal/ai/events', 'POST', '{"eventUid":"EVT-20260322-0008"}', 0, 'ok', 25, '2026-03-22 09:43:11'),
       (16010, 'TRC-20260322-0010', 'OP', 1000004, '沈哲', '10.10.1.23', '/api/lifeguards/list/page', 'POST', '{"venueId":2003}', 0, 'ok', 22, '2026-03-22 09:44:00'),
       (16011, 'TRC-20260322-0011', 'ALERT', 1000014, '杜浩然', '10.10.2.14', '/api/alerts/action', 'POST', '{"alertId":14009,"actionType":"DONE"}', 0, 'ok', 29, '2026-03-22 09:47:10'),
       (16012, 'TRC-20260322-0012', 'AI_CALLBACK', NULL, NULL, '127.0.0.1', '/api/internal/ai/events', 'POST', '{"eventUid":"EVT-20260322-0012"}', 0, 'ok', 26, '2026-03-22 09:47:22'),
       (16013, 'TRC-20260322-0013', 'ALERT', 1000003, '陆谨言', '10.10.1.22', '/api/alerts/action', 'POST', '{"alertId":14014,"actionType":"DONE"}', 0, 'ok', 33, '2026-03-22 09:52:00'),
       (16014, 'TRC-20260322-0014', 'OP', 1000005, '顾川', '10.10.1.24', '/api/stats/overview', 'GET', '{}', 0, 'ok', 19, '2026-03-22 09:52:10'),
       (16015, 'TRC-20260322-0015', 'ALERT', 1000021, '沈佳宁', '10.10.2.21', '/api/alerts/action', 'POST', '{"alertId":14016,"actionType":"DONE"}', 0, 'ok', 27, '2026-03-22 09:53:20'),
       (16016, 'TRC-20260322-0016', 'AI_CALLBACK', NULL, NULL, '127.0.0.1', '/api/internal/ai/events', 'POST', '{"eventUid":"EVT-20260322-0019"}', 0, 'ok', 25, '2026-03-22 09:54:42'),
       (16017, 'TRC-20260322-0017', 'OP', 1000002, '韩锐', '10.10.1.21', '/api/alerts/list/page', 'POST', '{"alertStatus":"PENDING"}', 0, 'ok', 30, '2026-03-22 09:55:00'),
       (16018, 'TRC-20260322-0018', 'ALERT', 1000025, '金硕', '10.10.2.25', '/api/alerts/action', 'POST', '{"alertId":14020,"actionType":"DONE"}', 0, 'ok', 32, '2026-03-22 09:57:30'),
       (16019, 'TRC-20260322-0019', 'OP', 1000001, '平台管理员', '10.10.1.10', '/api/stats/export/excel', 'POST', '{"reportType":"ALERT_SUMMARY"}', 0, 'ok', 88, '2026-03-22 10:00:00'),
       (16020, 'TRC-20260322-0020', 'OP', 1000001, '平台管理员', '10.10.1.10', '/api/system/audit/list/page', 'POST', '{"current":1,"pageSize":20}', 0, 'ok', 42, '2026-03-22 10:01:00')
ON DUPLICATE KEY UPDATE
    trace_id         = VALUES(trace_id),
    log_category     = VALUES(log_category),
    operator_id      = VALUES(operator_id),
    operator_name    = VALUES(operator_name),
    client_ip        = VALUES(client_ip),
    request_uri      = VALUES(request_uri),
    request_method   = VALUES(request_method),
    request_body     = VALUES(request_body),
    response_code    = VALUES(response_code),
    response_message = VALUES(response_message),
    cost_ms          = VALUES(cost_ms),
    created_at       = VALUES(created_at);

-- =========================
-- 18) stats_snapshot (20条, HOUR + DAY)
-- =========================
INSERT INTO stats_snapshot (id, granularity, snapshot_date, snapshot_hour, venue_id, metric_type, metric_key,
                            metric_value, dimension_json, created_at)
VALUES (17001, 'HOUR', '2026-03-22', 0, 2001, 'ALERT_COUNT', 'drowing_alerts', 1.0000, '{"alertType":"DROWING"}', '2026-03-22 01:00:00'),
       (17002, 'HOUR', '2026-03-22', 1, 2001, 'ALERT_COUNT', 'drowing_alerts', 0.0000, '{"alertType":"DROWING"}', '2026-03-22 02:00:00'),
       (17003, 'HOUR', '2026-03-22', 2, 2002, 'ALERT_COUNT', 'overcrowd_alerts', 1.0000, '{"alertType":"OVERCROWD"}', '2026-03-22 03:00:00'),
       (17004, 'HOUR', '2026-03-22', 3, 2003, 'ALERT_COUNT', 'drowing_alerts', 1.0000, '{"alertType":"DROWING"}', '2026-03-22 04:00:00'),
       (17005, 'HOUR', '2026-03-22', 4, 2004, 'ALERT_COUNT', 'off_post_alerts', 1.0000, '{"alertType":"OFF_POST"}', '2026-03-22 05:00:00'),
       (17006, 'HOUR', '2026-03-22', 5, 2005, 'ALERT_COUNT', 'drowing_alerts', 1.0000, '{"alertType":"DROWING"}', '2026-03-22 06:00:00'),
       (17007, 'HOUR', '2026-03-22', 6, 2006, 'ALERT_COUNT', 'overcrowd_alerts', 1.0000, '{"alertType":"OVERCROWD"}', '2026-03-22 07:00:00'),
       (17008, 'HOUR', '2026-03-22', 7, 2007, 'ALERT_COUNT', 'drowing_alerts', 1.0000, '{"alertType":"DROWING"}', '2026-03-22 08:00:00'),
       (17009, 'HOUR', '2026-03-22', 8, 2008, 'ALERT_COUNT', 'drowing_alerts', 1.0000, '{"alertType":"DROWING"}', '2026-03-22 09:00:00'),
       (17010, 'HOUR', '2026-03-22', 9, 2009, 'ALERT_COUNT', 'off_post_alerts', 1.0000, '{"alertType":"OFF_POST"}', '2026-03-22 10:00:00'),
       (17011, 'HOUR', '2026-03-22', 10, 2010, 'ALERT_COUNT', 'drowing_alerts', 1.0000, '{"alertType":"DROWING"}', '2026-03-22 11:00:00'),
       (17012, 'HOUR', '2026-03-22', 11, 2011, 'ALERT_COUNT', 'overcrowd_alerts', 1.0000, '{"alertType":"OVERCROWD"}', '2026-03-22 12:00:00'),
       (17013, 'HOUR', '2026-03-22', 12, 2012, 'ALERT_COUNT', 'drowing_alerts', 1.0000, '{"alertType":"DROWING"}', '2026-03-22 13:00:00'),
       (17014, 'HOUR', '2026-03-22', 13, 2013, 'ALERT_COUNT', 'off_post_alerts', 1.0000, '{"alertType":"OFF_POST"}', '2026-03-22 14:00:00'),
       (17015, 'DAY', '2026-03-20', NULL, 2001, 'ALERT_TOTAL', 'alerts', 5.0000, '{"date":"2026-03-20"}', '2026-03-21 00:10:00'),
       (17016, 'DAY', '2026-03-20', NULL, 2008, 'ALERT_TOTAL', 'alerts', 4.0000, '{"date":"2026-03-20"}', '2026-03-21 00:10:00'),
       (17017, 'DAY', '2026-03-21', NULL, 2001, 'ALERT_TOTAL', 'alerts', 6.0000, '{"date":"2026-03-21"}', '2026-03-22 00:10:00'),
       (17018, 'DAY', '2026-03-21', NULL, 2008, 'ALERT_TOTAL', 'alerts', 5.0000, '{"date":"2026-03-21"}', '2026-03-22 00:10:00'),
       (17019, 'DAY', '2026-03-22', NULL, 2001, 'ALERT_TOTAL', 'alerts', 3.0000, '{"date":"2026-03-22"}', '2026-03-23 00:10:00'),
       (17020, 'DAY', '2026-03-22', NULL, 2008, 'ALERT_TOTAL', 'alerts', 2.0000, '{"date":"2026-03-22"}', '2026-03-23 00:10:00')
ON DUPLICATE KEY UPDATE
    granularity   = VALUES(granularity),
    snapshot_date = VALUES(snapshot_date),
    snapshot_hour = VALUES(snapshot_hour),
    venue_id      = VALUES(venue_id),
    metric_type   = VALUES(metric_type),
    metric_key    = VALUES(metric_key),
    metric_value  = VALUES(metric_value),
    dimension_json = VALUES(dimension_json),
    created_at    = VALUES(created_at);
