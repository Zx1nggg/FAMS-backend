-- ============================================================
-- 成本字段迁移脚本
-- 为采购批次和投喂日志补充成本属性，出塘结算自动汇总参考值
-- ============================================================

-- 1. t_purchase_batch：采购批次加单价和总金额
ALTER TABLE t_purchase_batch
    ADD COLUMN unit_price    DECIMAL(10, 2) DEFAULT NULL COMMENT '单价(元/件)' AFTER density_per_unit,
    ADD COLUMN total_amount  DECIMAL(10, 2) DEFAULT NULL COMMENT '总金额(元) = unitQty × unitPrice' AFTER unit_price;

-- 2. t_pond_feed_log：扩充为饲料+药品一体日志
ALTER TABLE t_pond_feed_log
    -- 饲料成本
    ADD COLUMN feed_unit_price     DECIMAL(10, 2) DEFAULT NULL COMMENT '饲料单价(元/kg)' AFTER water_change_status,
    ADD COLUMN feed_total_amount   DECIMAL(10, 2) DEFAULT NULL COMMENT '本次投喂金额(元) = feedAmount × feedUnitPrice' AFTER feed_unit_price,
    -- 药品记录
    ADD COLUMN medicine_name       VARCHAR(100)   DEFAULT NULL COMMENT '药品名称' AFTER feed_total_amount,
    ADD COLUMN medicine_dosage     DECIMAL(10, 2) DEFAULT NULL COMMENT '用量' AFTER medicine_name,
    ADD COLUMN medicine_unit       VARCHAR(20)    DEFAULT NULL COMMENT '用量单位(ml/g/袋)' AFTER medicine_dosage,
    ADD COLUMN medicine_amount     DECIMAL(10, 2) DEFAULT NULL COMMENT '本次药费(元)' AFTER medicine_unit;
