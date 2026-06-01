-- ============================================================
-- 迁移说明：创建日常巡塘台账主表 + 给已有表加 patrol_log_id
-- 执行方式：在 MySQL 中直接执行本脚本即可
-- 创建时间：2026-05-31
-- ============================================================

-- 1. 创建巡塘台账主表
CREATE TABLE `t_patrol_log` (
  `id`              bigint NOT NULL AUTO_INCREMENT,
  `pond_id`         bigint NOT NULL              COMMENT '巡塘的池塘ID',
  `batch_no`        varchar(64) DEFAULT NULL     COMMENT '关联批次号（混合养殖时指定，可选）',
  `patrol_time`     datetime NOT NULL            COMMENT '巡塘时间（一日多巡的核心字段）',
  `weather`         varchar(20) DEFAULT NULL     COMMENT '天气（晴/阴/雨）',
  `water_temp`      decimal(4,1) DEFAULT NULL    COMMENT '水温感官估算(°C)',
  `water_color`     varchar(30) DEFAULT NULL     COMMENT '水色感官（翠绿/黄绿/浑浊/发红）',
  `operator_id`     bigint DEFAULT NULL          COMMENT '巡塘人ID',
  `remark`          varchar(500) DEFAULT NULL    COMMENT '巡塘综合备注',
  `create_time`     datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      tinyint NOT NULL DEFAULT 0   COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_pond_date` (`pond_id`, `patrol_time`),
  KEY `idx_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日常巡塘台账主表：每次巡塘一条记录';

-- 2. 给已有表添加 patrol_log_id 外键关联
ALTER TABLE t_pond_feed_log
    ADD COLUMN patrol_log_id bigint DEFAULT NULL COMMENT '关联巡塘记录ID' AFTER id;

ALTER TABLE t_batch_growth_log
    ADD COLUMN patrol_log_id bigint DEFAULT NULL COMMENT '关联巡塘记录ID' AFTER id;

-- ============================================================
-- 验证
-- ============================================================
-- 巡塘台账 = t_patrol_log（主表） + t_pond_feed_log（投喂/换水） + t_batch_growth_log（生长/死亡）
-- 每次巡塘创建一条 patrol_log，投喂、生长、SOP打卡均可挂在 patrol_log_id 下
-- 支持一日多巡：同一天同一口塘可有多条 patrol_log
-- ============================================================
