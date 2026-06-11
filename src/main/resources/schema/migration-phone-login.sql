-- ======================================================
-- 迁移：phone 作为登录标识符
-- 1. 给 sys_user.phone 加唯一索引
-- 2. 给已有测试账号补手机号
-- ======================================================

ALTER TABLE sys_user ADD UNIQUE INDEX idx_phone (phone);

UPDATE sys_user SET phone = '13800000001' WHERE username = 'admin';
UPDATE sys_user SET phone = '13800000002' WHERE username = 'farmer';
UPDATE sys_user SET phone = '13800000003' WHERE username = 'leader';
