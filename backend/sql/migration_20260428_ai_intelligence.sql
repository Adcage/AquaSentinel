-- 第三阶段AI智能分析数据库迁移脚本
-- 执行日期: 2026-04-28

USE aqua_sentinel;

-- 1. alert_record表添加ai_analysis列
ALTER TABLE alert_record 
ADD COLUMN ai_analysis TEXT COMMENT 'AI智能分析结果' AFTER detection_result;

-- 2. 创建报警向量嵌入表
CREATE TABLE IF NOT EXISTS alert_embedding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    alert_id BIGINT NOT NULL COMMENT '关联alert_record.id',
    alert_uid VARCHAR(64) NOT NULL COMMENT '关联alert_record.alert_uid',
    source_text TEXT NOT NULL COMMENT '原始文本（用于向量化）',
    embedding JSON NOT NULL COMMENT '向量数据（JSON数组，1536维）',
    embedding_model VARCHAR(128) NOT NULL COMMENT '嵌入模型标识',
    similarity_search_text TEXT COMMENT '用于搜索的复合文本',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_alert_id (alert_id),
    INDEX idx_alert_uid (alert_uid),
    INDEX idx_created_at (created_at),
    CONSTRAINT fk_embedding_alert FOREIGN KEY (alert_id) REFERENCES alert_record (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报警向量嵌入表';

-- 3. 创建AI对话会话表
CREATE TABLE IF NOT EXISTS ai_chat_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(128) DEFAULT NULL COMMENT '会话标题',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_id (user_id),
    INDEX idx_updated_at (updated_at),
    CONSTRAINT fk_conversation_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话会话表';

-- 4. 创建AI对话消息表
CREATE TABLE IF NOT EXISTS ai_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：user/assistant/function',
    content TEXT NOT NULL COMMENT '消息内容',
    function_name VARCHAR(64) DEFAULT NULL COMMENT '调用的Function名称',
    function_args TEXT DEFAULT NULL COMMENT 'Function调用参数（JSON）',
    function_result TEXT DEFAULT NULL COMMENT 'Function返回结果（JSON）',
    tokens_used INT DEFAULT NULL COMMENT '使用的token数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_delete TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_created_at (created_at),
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES ai_chat_conversation (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话消息表';