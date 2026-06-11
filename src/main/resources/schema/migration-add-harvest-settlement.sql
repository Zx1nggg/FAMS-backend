-- ============================================================
-- 迁移说明：为出塘结算模块扩展 t_harvest_record 表
-- 新增字段：单价、收入、成本明细、净利润、结算状态、软删除等
-- 执行方式：在 MySQL 中直接执行本脚本即可
-- 创建时间：2026-06-11
-- ============================================================

ALTER TABLE t_harvest_record
    ADD COLUMN unit_price         DECIMAL(10,2) DEFAULT NULL   COMMENT '出塘单价(元/kg)',
    ADD COLUMN total_revenue      DECIMAL(12,2) DEFAULT NULL   COMMENT '总收入(元) = actual_total_weight_kg × unit_price',
    ADD COLUMN seedling_cost      DECIMAL(12,2) DEFAULT NULL   COMMENT '苗种成本(元)',
    ADD COLUMN feed_cost          DECIMAL(12,2) DEFAULT NULL   COMMENT '饲料成本(元)',
    ADD COLUMN medicine_cost      DECIMAL(12,2) DEFAULT NULL   COMMENT '药品成本(元)',
    ADD COLUMN other_cost         DECIMAL(12,2) DEFAULT NULL   COMMENT '其他成本(元)',
    ADD COLUMN total_cost         DECIMAL(12,2) DEFAULT NULL   COMMENT '总成本(元) = 四项成本之和',
    ADD COLUMN net_profit         DECIMAL(12,2) DEFAULT NULL   COMMENT '净利润(元) = total_revenue - total_cost',
    ADD COLUMN settlement_status  TINYINT     DEFAULT 0        COMMENT '结算状态: 0=未结算 1=已结算',
    ADD COLUMN remark             VARCHAR(500) DEFAULT NULL    COMMENT '备注',
    ADD COLUMN is_deleted         TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常 1=已删除',
    ADD COLUMN create_time        DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    ADD COLUMN update_time        DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- ============================================================
-- 验证
-- ============================================================
-- 执行后，t_harvest_record 具备完整的出塘结算能力：
--   收入侧：unit_price → total_revenue（自动计算）
--   成本侧：seedling_cost + feed_cost + medicine_cost + other_cost → total_cost（自动汇算）
--   利润侧：net_profit = total_revenue - total_cost（自动计算）
--   状态机：settlement_status 标识是否已完成结算
--   软删除：is_deleted 配合 MyBatis-Plus @TableLogic，物理数据不丢失
--   一批次只能有一条未删除的出塘记录（Service 层保证）
--   删除出塘记录时自动恢复 PurchaseBatch.batch_status 3→2，允许重新出塘
-- ============================================================
