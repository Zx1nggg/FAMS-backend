-- ============================================================
-- 迁移说明：为 Farm 和 Pond 启用 MyBatis-Plus 逻辑删除
-- 执行方式：在 MySQL 中直接执行本脚本即可
-- 创建时间：2026-05-23
-- ============================================================

-- 给养殖场表添加逻辑删除字段
ALTER TABLE t_farm
    ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-已删除';

-- 给池塘表添加逻辑删除字段
ALTER TABLE t_pond
    ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-已删除';

-- ============================================================
-- 验证
-- ============================================================
-- 执行后，删除 Farm 时 MyBatis-Plus 会自动转换为 UPDATE is_deleted=1，
-- 查询时自动过滤 is_deleted=1 的记录。
-- 误删可通过 PUT /base/farm/restore/{ids} 恢复。
