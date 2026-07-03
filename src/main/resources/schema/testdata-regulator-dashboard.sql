-- ============================================================
-- 监管方 Dashboard 告警与 GIS 测试数据（新告警模型）
-- 前置：已执行 recreate-alarm-system.sql，且 t_farm 至少有 5 条数据
-- ============================================================

UPDATE t_farm SET longitude = 110.359, latitude = 21.271, address = '广东省湛江市遂溪县' WHERE id = 1;
UPDATE t_farm SET longitude = 113.577, latitude = 22.271, address = '广东省珠海市斗门区' WHERE id = 2;
UPDATE t_farm SET longitude = 116.682, latitude = 23.354, address = '广东省汕头市南澳县' WHERE id = 3;
UPDATE t_farm SET longitude = 111.983, latitude = 21.858, address = '广东省阳江市海陵岛' WHERE id = 4;
UPDATE t_farm SET longitude = 115.375, latitude = 22.787, address = '广东省汕尾市马宫镇' WHERE id = 5;

INSERT INTO sys_alarm_record
    (farm_id, alarm_code, title, message, source_type, severity, status,
     metric_code, trigger_value, threshold_operator, threshold_value, metric_unit,
     dedup_key, occurrence_count, first_occurred_at, last_occurred_at, created_at)
VALUES
    (1, 'IOT_DO_LOW', '溶解氧过低', '1号池溶解氧持续偏低，请立即开启增氧设备', 'IOT', 3, 0,
     'dissolved_oxygen', 1.8000, 'LT', 3.5000, 'mg/L', '1:0:IOT_DO_LOW:dissolved_oxygen', 3,
     DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR)),
    (1, 'IOT_TEMP_HIGH', '水温过高', '3号池水温升高至34.2℃', 'IOT', 2, 1,
     'water_temp', 34.2000, 'GT', 32.0000, '℃', '1:0:IOT_TEMP_HIGH:water_temp', 1,
     DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (2, 'IOT_PH_HIGH', 'pH过高', '5号池pH值为9.1，建议立即换水', 'IOT', 2, 0,
     'ph_value', 9.1000, 'GT', 9.0000, 'pH', '2:0:IOT_PH_HIGH:ph_value', 2,
     DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR)),
    (3, 'BIOLOGY_MORTALITY', '异常死亡', '7号池突发大量死苗，原因待查', 'BIOLOGY', 3, 2,
     'mortality_count', 5000.0000, 'GT', 1000.0000, '尾', '3:0:BIOLOGY_MORTALITY:mortality_count', 1,
     DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (4, 'FEED_MISSING', '投喂记录缺失', '连续2天无投喂记录，请核查', 'SYSTEM', 1, 0,
     NULL, NULL, NULL, NULL, NULL, '4:0:FEED_MISSING:none', 1,
     DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 已解决历史告警（active_dedup_key 自动为 NULL，不占用活动告警唯一键）
INSERT INTO sys_alarm_record
    (farm_id, alarm_code, title, message, source_type, severity, status,
     metric_code, trigger_value, threshold_operator, threshold_value, metric_unit,
     dedup_key, occurrence_count, first_occurred_at, last_occurred_at,
     resolved_at, resolution_remark, created_at)
VALUES
    (5, 'IOT_DO_LOW', '溶解氧过低', '2号池溶解氧曾降至3.2mg/L', 'IOT', 2, 3,
     'dissolved_oxygen', 3.2000, 'LT', 3.5000, 'mg/L', '5:0:IOT_DO_LOW:dissolved_oxygen', 2,
     DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY),
     DATE_SUB(NOW(), INTERVAL 5 DAY), '换水并开启增氧设备后恢复正常', DATE_SUB(NOW(), INTERVAL 7 DAY));

SELECT farm_id, alarm_code, title, severity, status, occurrence_count, last_occurred_at
FROM sys_alarm_record
ORDER BY last_occurred_at DESC;