-- ============================================================
-- Farm 表增加地理信息字段，支撑监管方 GIS 地图展示
-- 依赖: t_farm 表已存在
-- 日期: 2026-07-03
-- ============================================================

ALTER TABLE t_farm
    ADD COLUMN IF NOT EXISTS longitude  DECIMAL(10, 7) COMMENT '经度',
    ADD COLUMN IF NOT EXISTS latitude   DECIMAL(10, 7) COMMENT '纬度',
    ADD COLUMN IF NOT EXISTS address    VARCHAR(255)   COMMENT '详细地址';

-- 为 GIS 查询建立索引
CREATE INDEX IF NOT EXISTS idx_farm_geo ON t_farm (longitude, latitude);
