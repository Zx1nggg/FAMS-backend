-- ======================================================
-- 迁移：username 解除唯一约束，改为可空
-- 背景：登录标识符已切换为 phone，username 将作为
--       可选的网名/昵称字段，不再强制绑定手机号。
--       同时支持同一手机号被拒后重新提交申请。
-- ======================================================

-- 1. sys_user 表：删唯一约束，改可空
ALTER TABLE sys_user DROP INDEX username;
ALTER TABLE sys_user MODIFY COLUMN username VARCHAR(50) DEFAULT NULL COMMENT '网名/昵称（可选）';

-- 2. sys_registration_application 表：删唯一约束，改可空
ALTER TABLE sys_registration_application DROP INDEX uk_username;
ALTER TABLE sys_registration_application MODIFY COLUMN username VARCHAR(50) DEFAULT NULL COMMENT '网名/昵称（可选）';
