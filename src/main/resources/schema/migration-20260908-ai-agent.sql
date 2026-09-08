-- AI Agent MVP：会话归属及最小化审计。可安全重复执行建表部分。
CREATE TABLE IF NOT EXISTS ai_conversation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_key CHAR(36) NOT NULL COMMENT '对外暴露的随机会话标识',
    user_id BIGINT NOT NULL COMMENT '会话所有者',
    farm_id BIGINT NULL COMMENT '农户会话创建时选中的养殖场',
    user_role VARCHAR(20) NOT NULL COMMENT '创建时角色快照',
    title VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1进行中、0关闭',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_conversation_key (conversation_key),
    KEY idx_ai_conversation_user_time (user_id, updated_at),
    KEY idx_ai_conversation_farm_time (farm_id, updated_at),
    CONSTRAINT chk_ai_conversation_status CHECK (status IN (0,1)),
    CONSTRAINT chk_ai_conversation_role CHECK (user_role IN ('ADMIN','REGULATOR','FARMER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI会话归属与隔离';

CREATE TABLE IF NOT EXISTS ai_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    message_role VARCHAR(20) NOT NULL COMMENT 'user/assistant/error',
    content MEDIUMTEXT NOT NULL,
    model VARCHAR(100) NULL,
    provider_response_id VARCHAR(100) NULL,
    tool_calls_json JSON NULL COMMENT '仅记录工具名、调用ID和状态，不记录工具原始结果',
    input_tokens INT NULL,
    output_tokens INT NULL,
    total_tokens INT NULL,
    latency_ms BIGINT NULL,
    error_code VARCHAR(50) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_message_conversation_time (conversation_id, id),
    KEY idx_ai_message_model_time (model, created_at),
    CONSTRAINT fk_ai_message_conversation FOREIGN KEY (conversation_id)
        REFERENCES ai_conversation(id) ON DELETE CASCADE,
    CONSTRAINT chk_ai_message_role CHECK (message_role IN ('user','assistant','error'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI消息、用量与工具调用审计';
