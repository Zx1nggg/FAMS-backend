-- ============================================================
-- FAMS 告警系统全量重建脚本
-- MySQL 8.0+
-- 警告：会永久删除旧告警、旧规则和旧处理流水数据。
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS sys_alarm_action_log;
DROP TABLE IF EXISTS sys_alarm_record;
DROP TABLE IF EXISTS sys_alarm_rule;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. 告警规则表：统一维护 IoT 阈值及复杂业务规则
CREATE TABLE sys_alarm_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rule_code VARCHAR(80) NOT NULL COMMENT '规则唯一编码',
    rule_name VARCHAR(150) NOT NULL COMMENT '规则名称',
    source_type VARCHAR(30) NOT NULL COMMENT 'IOT/BIOLOGY/QUARANTINE/TRANSPORT/SYSTEM',
    alarm_code VARCHAR(50) NOT NULL COMMENT '触发后的告警编码',
    metric_code VARCHAR(50) NULL COMMENT '监测指标编码，复杂业务规则可为空',
    threshold_operator VARCHAR(8) NULL COMMENT 'LT/LE/GT/GE/EQ/BETWEEN',
    threshold_value DECIMAL(14,4) NULL COMMENT '阈值或区间下限',
    threshold_value_high DECIMAL(14,4) NULL COMMENT '区间上限',
    metric_unit VARCHAR(20) NULL COMMENT '指标单位',
    severity TINYINT NOT NULL COMMENT '1提示、2警告、3严重',
    scope_type VARCHAR(20) NOT NULL DEFAULT 'GLOBAL' COMMENT 'GLOBAL/FARM/SPECIES',
    farm_id BIGINT NULL COMMENT '指定养殖场',
    seedling_id BIGINT NULL COMMENT '指定苗种品种',
    cooldown_minutes INT NOT NULL DEFAULT 30 COMMENT '重复触发冷却分钟数',
    rule_config JSON NULL COMMENT '检疫、调运等复杂规则配置',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '0停用、1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_alarm_rule_code (rule_code),
    KEY idx_alarm_rule_enabled_source (enabled, source_type),
    KEY idx_alarm_rule_scope (scope_type, farm_id, seedling_id),
    CONSTRAINT chk_alarm_rule_source CHECK (source_type IN ('IOT','BIOLOGY','QUARANTINE','TRANSPORT','SYSTEM')),
    CONSTRAINT chk_alarm_rule_severity CHECK (severity IN (1,2,3)),
    CONSTRAINT chk_alarm_rule_scope CHECK (scope_type IN ('GLOBAL','FARM','SPECIES')),
    CONSTRAINT chk_alarm_rule_operator CHECK (
        threshold_operator IS NULL OR threshold_operator IN ('LT','LE','GT','GE','EQ','BETWEEN')
    ),
    CONSTRAINT chk_alarm_rule_enabled CHECK (enabled IN (0,1)),
    CONSTRAINT chk_alarm_rule_cooldown CHECK (cooldown_minutes >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警触发规则';

-- 2. 告警事件表：一行代表一个完整事件；持续异常更新同一行
CREATE TABLE sys_alarm_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    farm_id BIGINT NOT NULL COMMENT '所属养殖场ID',
    pond_id BIGINT NULL COMMENT '所属池塘ID，养殖场级告警可为空',
    rule_id BIGINT NULL COMMENT '触发规则ID',
    alarm_code VARCHAR(50) NOT NULL COMMENT '告警编码',
    title VARCHAR(150) NOT NULL COMMENT '告警标题',
    message VARCHAR(500) NOT NULL COMMENT '告警详细描述',
    source_type VARCHAR(30) NOT NULL COMMENT 'IOT/BIOLOGY/QUARANTINE/TRANSPORT/SYSTEM',
    source_id BIGINT NULL COMMENT '传感器数据或业务记录ID',
    severity TINYINT NOT NULL COMMENT '1提示、2警告、3严重',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待确认、1已确认、2处理中、3已解决、4已关闭',
    metric_code VARCHAR(50) NULL COMMENT '触发指标编码',
    trigger_value DECIMAL(14,4) NULL COMMENT '最近一次触发实际值',
    threshold_operator VARCHAR(8) NULL COMMENT 'LT/LE/GT/GE/EQ/BETWEEN',
    threshold_value DECIMAL(14,4) NULL COMMENT '触发阈值或区间下限',
    threshold_value_high DECIMAL(14,4) NULL COMMENT '区间上限',
    metric_unit VARCHAR(20) NULL COMMENT '指标单位',
    dedup_key VARCHAR(160) NOT NULL COMMENT '如 farm:pond:alarmCode:metric',
    active_dedup_key VARCHAR(160) GENERATED ALWAYS AS (
        CASE WHEN status IN (0,1,2) THEN dedup_key ELSE NULL END
    ) STORED COMMENT '活动告警唯一键',
    occurrence_count INT NOT NULL DEFAULT 1 COMMENT '累计触发次数',
    first_occurred_at DATETIME NOT NULL COMMENT '首次触发时间',
    last_occurred_at DATETIME NOT NULL COMMENT '最近触发时间',
    acknowledged_by BIGINT NULL COMMENT '确认人ID',
    acknowledged_at DATETIME NULL COMMENT '确认时间',
    resolved_by BIGINT NULL COMMENT '解决人ID',
    resolved_at DATETIME NULL COMMENT '解决时间',
    resolution_remark VARCHAR(500) NULL COMMENT '解决说明',
    recovered_at DATETIME NULL COMMENT '监测指标自动恢复时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_alarm_active_dedup (active_dedup_key),
    KEY idx_alarm_farm_status_severity_time (farm_id, status, severity, last_occurred_at),
    KEY idx_alarm_pond_status_time (pond_id, status, last_occurred_at),
    KEY idx_alarm_code_status_time (alarm_code, status, last_occurred_at),
    KEY idx_alarm_source (source_type, source_id),
    KEY idx_alarm_rule_time (rule_id, last_occurred_at),
    CONSTRAINT fk_alarm_record_rule FOREIGN KEY (rule_id) REFERENCES sys_alarm_rule(id) ON DELETE SET NULL,
    CONSTRAINT chk_alarm_source CHECK (source_type IN ('IOT','BIOLOGY','QUARANTINE','TRANSPORT','SYSTEM')),
    CONSTRAINT chk_alarm_severity CHECK (severity IN (1,2,3)),
    CONSTRAINT chk_alarm_status CHECK (status IN (0,1,2,3,4)),
    CONSTRAINT chk_alarm_occurrence CHECK (occurrence_count >= 1),
    CONSTRAINT chk_alarm_operator CHECK (
        threshold_operator IS NULL OR threshold_operator IN ('LT','LE','GT','GE','EQ','BETWEEN')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警事件主表';

-- 3. 告警处理流水表：记录完整审计轨迹
CREATE TABLE sys_alarm_action_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    alarm_id BIGINT NOT NULL COMMENT '告警事件ID',
    action_type VARCHAR(30) NOT NULL COMMENT 'ACKNOWLEDGE/START_PROCESS/RESOLVE/CLOSE/REOPEN/AUTO_RECOVER',
    from_status TINYINT NULL COMMENT '变更前状态',
    to_status TINYINT NOT NULL COMMENT '变更后状态',
    operator_id BIGINT NULL COMMENT '操作人ID，系统操作可为空',
    action_remark VARCHAR(500) NULL COMMENT '操作或处理说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_alarm_action_alarm_time (alarm_id, created_at),
    KEY idx_alarm_action_operator_time (operator_id, created_at),
    CONSTRAINT fk_alarm_action_record FOREIGN KEY (alarm_id) REFERENCES sys_alarm_record(id) ON DELETE CASCADE,
    CONSTRAINT chk_alarm_action_from_status CHECK (from_status IS NULL OR from_status IN (0,1,2,3,4)),
    CONSTRAINT chk_alarm_action_to_status CHECK (to_status IN (0,1,2,3,4)),
    CONSTRAINT chk_alarm_action_type CHECK (
        action_type IN ('ACKNOWLEDGE','START_PROCESS','RESOLVE','CLOSE','REOPEN','AUTO_RECOVER')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警处理审计流水';

-- 4. 初始化当前 IoT 默认规则
INSERT INTO sys_alarm_rule
    (rule_code, rule_name, source_type, alarm_code, metric_code,
     threshold_operator, threshold_value, metric_unit, severity, cooldown_minutes)
VALUES
    ('IOT_DO_LOW_DEFAULT','溶解氧过低','IOT','IOT_DO_LOW','dissolved_oxygen','LT',3.5000,'mg/L',3,30),
    ('IOT_TEMP_HIGH_DEFAULT','水温过高','IOT','IOT_TEMP_HIGH','water_temp','GT',35.0000,'℃',2,30),
    ('IOT_PH_LOW_DEFAULT','pH过低','IOT','IOT_PH_LOW','ph_value','LT',6.5000,'pH',2,30),
    ('IOT_PH_HIGH_DEFAULT','pH过高','IOT','IOT_PH_HIGH','ph_value','GT',9.0000,'pH',2,30);

-- 5. 执行结果检查
SHOW CREATE TABLE sys_alarm_rule;
SHOW CREATE TABLE sys_alarm_record;
SHOW CREATE TABLE sys_alarm_action_log;

SELECT id, rule_code, alarm_code, metric_code, threshold_operator,
       threshold_value, metric_unit, severity, enabled
FROM sys_alarm_rule
ORDER BY id;