-- 来源：PLAN_监管方功能开发计划.md §3.3 / B5。仅新增表，不更改现有业务数据。
CREATE TABLE IF NOT EXISTS t_inspection_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    farm_id BIGINT NOT NULL,
    pond_id BIGINT NULL,
    inspection_date DATE NOT NULL,
    inspection_type VARCHAR(50) NOT NULL,
    inspection_item VARCHAR(200),
    result VARCHAR(20) NOT NULL,
    inspector_name VARCHAR(50),
    inspector_id BIGINT NOT NULL,
    description TEXT,
    unqualified_reason VARCHAR(500),
    attachment_urls TEXT,
    rectify_status VARCHAR(20) NOT NULL DEFAULT 'none',
    rectify_deadline DATE,
    rectify_remark VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_inspection_farm (farm_id),
    INDEX idx_inspection_date (inspection_date),
    INDEX idx_inspection_rectify (rectify_status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监管方线下抽检档案';
