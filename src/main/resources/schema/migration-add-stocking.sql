-- ============================================================
-- 迁移说明：创建投放登记表，建立批次与池塘的多对多关联
-- 执行方式：在 MySQL 中直接执行本脚本即可
-- 创建时间：2026-05-31
-- ============================================================

CREATE TABLE `t_stocking` (
  `id`              bigint NOT NULL AUTO_INCREMENT,
  `batch_id`        bigint NOT NULL              COMMENT '批次ID (FK → t_purchase_batch.id)',
  `pond_id`         bigint NOT NULL              COMMENT '池塘ID (FK → t_pond.id)',
  `stocked_units`   int NOT NULL                 COMMENT '投放件数（袋/箱数）',
  `stocked_qty`     int NOT NULL                 COMMENT '系统换算尾数 = stocked_units × 批次.density_per_unit',
  `stocked_weight`  decimal(10,2) DEFAULT NULL   COMMENT '投放总重(kg)，可选',
  `stocking_date`   date NOT NULL                COMMENT '投放日期',
  `remark`          varchar(255) DEFAULT NULL    COMMENT '备注',
  `create_time`     datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      tinyint NOT NULL DEFAULT 0   COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_pond_id` (`pond_id`),
  KEY `idx_batch_id` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投放登记表：批次与池塘的多对多关联';

-- ============================================================
-- 验证
-- ============================================================
-- 执行后，批次与池塘建立多对多投放关系：
--   t_stocking.batch_id → t_purchase_batch.id
--   t_stocking.pond_id  → t_pond.id
-- 支持拆分养殖（一批投多塘）与混合养殖（一塘收多批）
-- 支持分期投放：同一批次可多次投进同一池塘（每次一条记录）
-- stocked_qty 由系统根据 stocked_units × density_per_unit 自动换算
-- 件数总量由 Service 层校验（Σstocked_units ≤ batch.unit_qty），不在数据库加唯一约束
-- ============================================================
