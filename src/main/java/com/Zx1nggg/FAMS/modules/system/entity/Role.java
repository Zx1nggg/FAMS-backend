package com.Zx1nggg.FAMS.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * <p>
 * 角色信息表
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("sys_role")
@Schema(name = "Role", description = "角色信息表")
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 角色名称 (如: 养殖户, 监管人员)
     */
    @TableField("role_name")
    @Schema(description = "角色名称 (如: 养殖户, 监管人员)")
    private String roleName;

    /**
     * 角色权限字符串 (如: role_farmer, role_regulator)
     */
    @TableField("role_key")
    @Schema(description = "角色权限字符串 (如: role_farmer, role_regulator)")
    private String roleKey;
}
