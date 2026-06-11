package com.Zx1nggg.FAMS.modules.lifecycle.mapper;

import com.Zx1nggg.FAMS.modules.lifecycle.entity.HarvestRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 出塘结算与消费者防伪溯源凭证表 Mapper 接口
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
public interface HarvestRecordMapper extends BaseMapper<HarvestRecord> {

    /**
     * 物理删除指定批次号的软删除记录（绕过 @TableLogic 自动过滤）
     * 用于解决：软删除记录仍占用 batch_no 唯一键，导致重新出塘结算时插入冲突
     */
    @Delete("DELETE FROM t_harvest_record WHERE batch_no = #{batchNo} AND is_deleted = 1")
    int physicalDeleteSoftDeletedByBatchNo(@Param("batchNo") String batchNo);
}
