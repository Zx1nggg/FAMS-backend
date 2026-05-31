-- ============================================================
-- 迁移说明：为采购批次表添加苗种字典关联字段
-- 执行方式：在 MySQL 中直接执行本脚本即可
-- 创建时间：2026-05-31
-- ============================================================

-- 给采购批次表添加苗种品种字段
ALTER TABLE t_purchase_batch
    ADD COLUMN seedling_id bigint NOT NULL COMMENT '苗种品种，关联t_seedling_dict.id' AFTER supplier_id;

-- 添加外键约束（可选，若不需要可注释掉）
-- ALTER TABLE t_purchase_batch
--     ADD CONSTRAINT fk_purchase_batch_seedling
--     FOREIGN KEY (seedling_id) REFERENCES t_seedling_dict(id);

-- ============================================================
-- 验证
-- ============================================================
-- 执行后，采购批次与苗种字典建立关联：
--   t_purchase_batch.seedling_id → t_seedling_dict.id
-- 查询时可关联获取品种名称、养殖周期、死亡率阈值等信息。
