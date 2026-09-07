package com.Zx1nggg.FAMS.modules.base.mapper;

import com.Zx1nggg.FAMS.modules.base.entity.SeedlingDict;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 苗种分类字典 Mapper 接口
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
public interface SeedlingDictMapper extends BaseMapper<SeedlingDict> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM t_seedling_dict WHERE id=#{id} FOR UPDATE")
    SeedlingDict selectForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
    @org.apache.ibatis.annotations.Select("SELECT (SELECT COUNT(*) FROM t_purchase_batch WHERE seedling_id=#{id}) + (SELECT COUNT(*) FROM t_sop_template WHERE category_id=#{id})")
    long countReferences(@org.apache.ibatis.annotations.Param("id") Long id);

}
