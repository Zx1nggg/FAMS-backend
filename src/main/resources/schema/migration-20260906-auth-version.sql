-- 部署新认证代码前执行一次。现有令牌按版本 0 兼容，首次安全变更后立即失效。
ALTER TABLE sys_user ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0 COMMENT '账号认证版本，安全状态变化时递增';
