package com.Zx1nggg.FAMS.modules.system.mapper;

import com.Zx1nggg.FAMS.modules.system.entity.RegistrationApplication;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 入驻申请表 Mapper 接口
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-06-09
 */
public interface RegistrationApplicationMapper extends BaseMapper<RegistrationApplication> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM sys_registration_application WHERE id = #{id} FOR UPDATE")
    RegistrationApplication selectForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
}
