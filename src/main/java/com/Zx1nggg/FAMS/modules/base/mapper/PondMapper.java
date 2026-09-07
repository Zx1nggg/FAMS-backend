package com.Zx1nggg.FAMS.modules.base.mapper;

import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
public interface PondMapper extends BaseMapper<Pond> {
    @org.apache.ibatis.annotations.Select("SELECT (SELECT COUNT(*) FROM t_stocking WHERE pond_id=#{id}) + (SELECT COUNT(*) FROM t_patrol_log WHERE pond_id=#{id}) + (SELECT COUNT(*) FROM t_batch_growth_log WHERE pond_id=#{id}) + (SELECT COUNT(*) FROM t_pond_feed_log WHERE pond_id=#{id}) + (SELECT COUNT(*) FROM t_harvest_record WHERE pond_id=#{id}) + (SELECT COUNT(*) FROM t_iot_sensor_data WHERE pond_id=#{id})")
    long countHistory(@org.apache.ibatis.annotations.Param("id") Long id);
    @org.apache.ibatis.annotations.Select("SELECT * FROM t_pond WHERE id=#{id} AND is_deleted=0 FOR UPDATE")
    Pond selectForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
    @org.apache.ibatis.annotations.Update("UPDATE t_pond SET is_deleted=0, delete_batch=NULL WHERE farm_id=#{farmId} AND is_deleted=1 AND delete_batch=#{deleteBatch}")
    int restoreCascadeDeleted(@org.apache.ibatis.annotations.Param("farmId") Long farmId,
                             @org.apache.ibatis.annotations.Param("deleteBatch") String deleteBatch);
}
