-- ============================================================
-- 入驻申请 + 审批制 数据库迁移
-- 创建 sys_registration_application 表
-- ============================================================

CREATE TABLE IF NOT EXISTS `sys_registration_application` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
  `username`           VARCHAR(50)   NOT NULL                   COMMENT '申请登录账号',
  `password`           VARCHAR(100)  NOT NULL                   COMMENT 'BCrypt加密密码',
  `real_name`          VARCHAR(50)   NOT NULL                   COMMENT '真实姓名/负责人',
  `phone`              VARCHAR(20)   DEFAULT NULL               COMMENT '联系电话',
  `email`              VARCHAR(100)  DEFAULT NULL               COMMENT '电子邮箱',
  `farm_name`          VARCHAR(100)  NOT NULL                   COMMENT '申请入驻的养殖场名称',
  `farm_province`      VARCHAR(50)   DEFAULT NULL               COMMENT '养殖场所属省份',
  `farm_city`          VARCHAR(50)   DEFAULT NULL               COMMENT '养殖场所属城市',
  `farm_address`       VARCHAR(200)  DEFAULT NULL               COMMENT '养殖场详细地址',
  `application_reason` TEXT          DEFAULT NULL               COMMENT '入驻申请理由/补充说明',
  `status`             TINYINT       DEFAULT 0                  COMMENT '审批状态: 0=待审批, 1=已通过, 2=已拒绝',
  `reviewer_id`        BIGINT        DEFAULT NULL               COMMENT '审批人ID（管理员）',
  `review_comment`     VARCHAR(500)  DEFAULT NULL               COMMENT '审批意见/拒绝原因',
  `reviewed_at`        DATETIME      DEFAULT NULL               COMMENT '审批时间',
  `created_at`         DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '申请提交时间',
  `updated_at`         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入驻申请表';
