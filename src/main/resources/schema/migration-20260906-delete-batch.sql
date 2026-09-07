-- 仅为后续农场级联删除记录批次；旧删除数据维持 NULL，不自动推断恢复范围。
ALTER TABLE t_farm ADD COLUMN delete_batch VARCHAR(36) NULL COMMENT '本次级联删除批次';
ALTER TABLE t_pond ADD COLUMN delete_batch VARCHAR(36) NULL COMMENT '随农场级联删除的批次，单独删除时为空';
