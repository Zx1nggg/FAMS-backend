package com.Zx1nggg.FAMS.modules.base.mapper;

import com.Zx1nggg.FAMS.modules.base.entity.Farm;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
public interface FarmMapper extends BaseMapper<Farm> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM t_farm WHERE id = #{id} FOR UPDATE")
    Farm selectIncludingDeletedForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);

    @org.apache.ibatis.annotations.Update("UPDATE t_farm SET is_deleted=0, delete_batch=NULL WHERE id=#{id} AND is_deleted=1")
    int restoreDeleted(@org.apache.ibatis.annotations.Param("id") Long id);
}
