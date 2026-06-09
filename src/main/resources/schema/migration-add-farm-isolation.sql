-- ============================================================
-- 数据隔离迁移：为 supplier 和 seedling_dict 添加 user_id
-- 按用户隔离供应商和苗种字典（同一用户多场区共享）
-- ============================================================

-- 1. t_supplier 添加 user_id
ALTER TABLE `t_supplier`
  ADD COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '所属用户ID（FARMER仅可见自己的供应商）' AFTER `create_time`;

-- 2. t_seedling_dict 添加 user_id
ALTER TABLE `t_seedling_dict`
  ADD COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '所属用户ID（FARMER仅可见自己的苗种）' AFTER `min_do`;

-- 3. 历史数据：将现有数据归属到默认测试用户 (user_id=2 即 farmer)
--    这样测试账号 farmer 仍能看到已有数据
UPDATE `t_supplier` SET `user_id` = 2 WHERE `user_id` IS NULL;
UPDATE `t_seedling_dict` SET `user_id` = 2 WHERE `user_id` IS NULL;
