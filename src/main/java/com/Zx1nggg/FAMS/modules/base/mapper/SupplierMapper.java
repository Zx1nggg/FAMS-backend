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

    @org.apache.ibatis.annotations.Select("SELECT seedling_id FROM t_supplier_seedling WHERE supplier_id=#{supplierId} ORDER BY seedling_id")
    java.util.List<Long> selectSeedlingIds(@org.apache.ibatis.annotations.Param("supplierId") Long supplierId);

    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM t_supplier_seedling WHERE supplier_id=#{supplierId} AND seedling_id=#{seedlingId}")
    long countSeedlingOffering(@org.apache.ibatis.annotations.Param("supplierId") Long supplierId,
                               @org.apache.ibatis.annotations.Param("seedlingId") Long seedlingId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM t_supplier_seedling WHERE supplier_id=#{supplierId}")
    void deleteSeedlingOfferings(@org.apache.ibatis.annotations.Param("supplierId") Long supplierId);

    @org.apache.ibatis.annotations.Insert("INSERT INTO t_supplier_seedling(supplier_id, seedling_id) VALUES(#{supplierId}, #{seedlingId})")
    void insertSeedlingOffering(@org.apache.ibatis.annotations.Param("supplierId") Long supplierId,
                                @org.apache.ibatis.annotations.Param("seedlingId") Long seedlingId);

}
