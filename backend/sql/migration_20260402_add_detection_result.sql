-- ===========================================
-- 数据库迁移脚本
-- 用途：为报警记录表添加算法识别结果字段
-- 日期：2026-04-02
-- ===========================================

-- 兼容性说明：部分 MySQL 8.x 环境不支持 ADD COLUMN IF NOT EXISTS 语法
-- 采用 information_schema 检测后再执行 ALTER，保证脚本可重复执行
SET @db_name = DATABASE();
SET @exists_cnt = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @db_name
    AND table_name = 'alert_record'
    AND column_name = 'detection_result'
);

SET @ddl_sql = IF(
  @exists_cnt = 0,
  'ALTER TABLE alert_record ADD COLUMN detection_result VARCHAR(512) NULL COMMENT ''算法识别结果/检测摘要'' AFTER video_stream_url',
  'SELECT ''detection_result already exists'''
);

PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
