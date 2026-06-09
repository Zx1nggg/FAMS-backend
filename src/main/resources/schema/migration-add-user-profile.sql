-- ============================================================
-- 迁移说明：sys_user 表增加个人主页相关字段（头像、邮箱、性别、地址）
-- 执行方式：在 MySQL 中直接执行本脚本
-- 创建时间：2026-06-03
-- ============================================================

ALTER TABLE sys_user
  ADD COLUMN avatar  VARCHAR(255) DEFAULT NULL COMMENT '头像路径（相对路径，如 uploads/avatar/1_xxx.jpg）',
  ADD COLUMN email   VARCHAR(100) DEFAULT NULL COMMENT '电子邮箱',
  ADD COLUMN gender  TINYINT      DEFAULT 0  COMMENT '性别: 0未设置 1男 2女',
  ADD COLUMN address VARCHAR(200) DEFAULT NULL COMMENT '地址 / 所在地';
