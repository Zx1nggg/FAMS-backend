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
 * 系统菜单与按钮权限表
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("sys_menu")
@Schema(name = "Menu", description = "系统菜单与按钮权限表")
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 父菜单ID
     */
    @TableField("parent_id")
    @Schema(description = "父菜单ID")
    private Long parentId;

    /**
     * 菜单名称
     */
    @TableField("menu_name")
    @Schema(description = "菜单名称")
    private String menuName;

    /**
     * 路由地址
     */
    @TableField("path")
    @Schema(description = "路由地址")
    private String path;

    /**
     * Vue组件路径
     */
    @TableField("component")
    @Schema(description = "Vue组件路径")
    private String component;

    /**
     * 权限标识 (如: base:pond:add)
     */
    @TableField("perms")
    @Schema(description = "权限标识 (如: base:pond:add)")
    private String perms;

    /**
     * 菜单类型: M目录, C菜单, F按钮
     */
    @TableField("menu_type")
    @Schema(description = "菜单类型: M目录, C菜单, F按钮")
    private String menuType;
}
