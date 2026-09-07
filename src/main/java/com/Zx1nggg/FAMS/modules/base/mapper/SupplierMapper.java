package com.Zx1nggg.FAMS.modules.base.mapper;

import com.Zx1nggg.FAMS.modules.base.entity.Supplier;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 苗种供应商/培育基地档案表 Mapper 接口
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
public interface SupplierMapper extends BaseMapper<Supplier> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM t_supplier WHERE id=#{id} FOR UPDATE")
    Supplier selectForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM t_purchase_batch WHERE supplier_id=#{id}")
    long countReferences(@org.apache.ibatis.annotations.Param("id") Long id);

}
