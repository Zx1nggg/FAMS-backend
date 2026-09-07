package com.Zx1nggg.FAMS.modules.system.mapper;

import com.Zx1nggg.FAMS.modules.system.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 系统用户信息表 Mapper 接口
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
public interface UserMapper extends BaseMapper<User> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM sys_user WHERE id=#{id} FOR UPDATE")
    User selectForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
    @org.apache.ibatis.annotations.Select("SELECT (SELECT COUNT(*) FROM t_farm WHERE user_id=#{id}) + (SELECT COUNT(*) FROM t_seedling_dict WHERE user_id=#{id})")
    long countOwnedData(@org.apache.ibatis.annotations.Param("id") Long id);

}
