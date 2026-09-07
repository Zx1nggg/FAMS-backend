package com.Zx1nggg.FAMS.modules.base.mapper;

import com.Zx1nggg.FAMS.modules.base.entity.PurchaseBatch;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
public interface PurchaseBatchMapper extends BaseMapper<PurchaseBatch> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM t_purchase_batch WHERE id = #{id} FOR UPDATE")
    PurchaseBatch selectForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);

    @org.apache.ibatis.annotations.Select("SELECT * FROM t_purchase_batch WHERE batch_no = #{batchNo} FOR UPDATE")
    PurchaseBatch selectByBatchNoForUpdate(@org.apache.ibatis.annotations.Param("batchNo") String batchNo);
}
