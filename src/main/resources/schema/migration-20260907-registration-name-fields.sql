-- 入驻申请改为填写展示昵称，真实姓名在审核通过并登录后补充。
-- 可重复执行；兼容曾经把 username 改成可空的数据库。

SET @drop_user_username_index = (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `sys_user` DROP INDEX `username`',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND index_name = 'username'
);
PREPARE registration_name_stmt FROM @drop_user_username_index;
EXECUTE registration_name_stmt;
DEALLOCATE PREPARE registration_name_stmt;

SET @drop_application_username_index = (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `sys_registration_application` DROP INDEX `uk_username`',
    'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_registration_application'
    AND index_name = 'uk_username'
);
PREPARE registration_name_stmt FROM @drop_application_username_index;
EXECUTE registration_name_stmt;
DEALLOCATE PREPARE registration_name_stmt;

UPDATE sys_user
SET username = CONCAT('用户', id)
WHERE username IS NULL OR TRIM(username) = '';

UPDATE sys_registration_application
SET username = CONCAT('用户', id)
WHERE username IS NULL OR TRIM(username) = '';

ALTER TABLE sys_user
  MODIFY COLUMN username VARCHAR(50) NOT NULL COMMENT '网名/昵称';

ALTER TABLE sys_registration_application
  MODIFY COLUMN username VARCHAR(50) NOT NULL COMMENT '前端展示昵称',
  MODIFY COLUMN real_name VARCHAR(50) DEFAULT NULL COMMENT '历史申请实名（新申请登录后补充）';
