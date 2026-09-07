-- 维护窗口执行一次；保留所有历史申请，不删除重复记录。
-- 若预检查有结果，先人工核对重复的待审申请，再执行 ALTER。
SELECT phone, COUNT(*) AS pending_count FROM sys_registration_application
WHERE status=0 GROUP BY phone HAVING COUNT(*)>1;
ALTER TABLE sys_registration_application
    ADD COLUMN pending_phone VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN status=0 THEN phone ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_registration_pending_phone(pending_phone);
