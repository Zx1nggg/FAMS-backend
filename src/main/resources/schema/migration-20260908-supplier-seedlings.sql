-- 供应商可供应苗种目录。已有数据不自动猜测供应关系，需由监管方核准后配置。
-- 供应商和苗种均已改为监管维护的公共目录，移除旧的农户归属字段。
ALTER TABLE `t_supplier` DROP COLUMN `user_id`;
ALTER TABLE `t_seedling_dict` DROP COLUMN `user_id`;

ALTER TABLE `t_purchase_batch`
  ADD COLUMN `quarantine_reviewer_id` BIGINT NULL COMMENT '检疫审核监管人员ID' AFTER `quarantine_cert_no`,
  ADD COLUMN `quarantine_reviewed_at` DATETIME NULL COMMENT '检疫审核通过时间' AFTER `quarantine_reviewer_id`;

CREATE TABLE `t_supplier_seedling` (
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `seedling_id` BIGINT NOT NULL COMMENT '苗种公共目录ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`supplier_id`, `seedling_id`),
  KEY `idx_supplier_seedling_seedling` (`seedling_id`),
  CONSTRAINT `fk_supplier_seedling_supplier`
    FOREIGN KEY (`supplier_id`) REFERENCES `t_supplier` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_supplier_seedling_seedling`
    FOREIGN KEY (`seedling_id`) REFERENCES `t_seedling_dict` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='供应商经核准可供应的苗种品类';
