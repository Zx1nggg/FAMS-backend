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
 * 系统用户信息表
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("sys_user")
@Schema(name = "User", description = "系统用户信息表")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 登录账号
     */
    @TableField("username")
    @Schema(description = "登录账号")
    private String username;

    /**
     * 密码(BCrypt加密)
     */
    @TableField("password")
    @Schema(description = "密码(BCrypt加密)")
    private String password;

    /**
     * 真实姓名/负责人姓名
     */
    @TableField("real_name")
    @Schema(description = "真实姓名/负责人姓名")
    private String realName;

    /**
     * 联系电话
     */
    @TableField("phone")
    @Schema(description = "联系电话")
    private String phone;

    /**
     * 用户类型: ADMIN(管理员), REGULATOR(监管方), FARMER(养殖户)
     */
    @TableField("user_type")
    @Schema(description = "用户类型: ADMIN(管理员), REGULATOR(监管方), FARMER(养殖户)")
    private String userType;

    /**
     * 所属养殖场ID (如果是养殖户，则关联t_farm；管理员和监管方为空)
     */
    @TableField("farm_id")
    @Schema(description = "所属养殖场ID (如果是养殖户，则关联t_farm；管理员和监管方为空)")
    private Long farmId;

    /**
     * 帐号状态: 1正常, 0停用
     */
    @TableField("status")
    @Schema(description = "帐号状态: 1正常, 0停用")
    private Byte status;
}
