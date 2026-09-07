-- 按池塘结算：同一批次可在多个池塘分别出塘，保留所有软删除历史。
-- 前置：已执行原 harvest-settlement 增量。先备份；在维护窗口执行。
-- 不自动修正历史金额或批次状态；见 PROJECT_AUDIT.md 的人工核对项。
-- 以下查询必须无结果，否则先由业务人员核对，禁止直接删除重复记录。
SELECT batch_no, pond_id, COUNT(*) AS active_count
FROM t_harvest_record WHERE is_deleted = 0
GROUP BY batch_no, pond_id HAVING COUNT(*) > 1;
SELECT id FROM t_harvest_record WHERE batch_no IS NULL OR pond_id IS NULL;

DELIMITER $$
CREATE PROCEDURE fams_migrate_pond_harvest()
BEGIN
    IF EXISTS (SELECT 1 FROM t_harvest_record WHERE batch_no IS NULL OR pond_id IS NULL)
       OR EXISTS (SELECT 1 FROM t_harvest_record WHERE is_deleted = 0
                  GROUP BY batch_no, pond_id HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Resolve invalid harvest associations before migration';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
                   AND table_name = 't_harvest_record' AND column_name = 'active_record') THEN
        ALTER TABLE t_harvest_record ADD COLUMN active_record TINYINT
            GENERATED ALWAYS AS (CASE WHEN is_deleted = 0 THEN 1 ELSE NULL END) STORED;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
                   AND table_name = 't_harvest_record' AND index_name = 'uk_harvest_batch_pond_active') THEN
        ALTER TABLE t_harvest_record ADD UNIQUE KEY uk_harvest_batch_pond_active (batch_no, pond_id, active_record);
    END IF;
    -- 历史 dump 中该唯一索引名为 batch_no；先建立新约束，再删除旧约束。
    IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
                AND table_name = 't_harvest_record' AND index_name = 'batch_no' AND non_unique = 0) THEN
        ALTER TABLE t_harvest_record DROP INDEX batch_no;
    END IF;
END$$
DELIMITER ;
CALL fams_migrate_pond_harvest();
DROP PROCEDURE fams_migrate_pond_harvest;

-- 人工验收：找出旧逻辑提前关闭、仍有池塘未出塘的批次，核对后再决定状态修复。
SELECT b.id, b.batch_no, b.batch_status
FROM t_purchase_batch b WHERE b.batch_status = 3 AND EXISTS (
    SELECT 1 FROM t_stocking s WHERE s.batch_id = b.id AND s.is_deleted = 0
      AND NOT EXISTS (SELECT 1 FROM t_harvest_record h
        WHERE h.batch_no = b.batch_no AND h.pond_id = s.pond_id AND h.is_deleted = 0)
);
