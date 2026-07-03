package com.Zx1nggg.FAMS.modules.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 *
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("t_farm")
@Schema(name = "Farm", description = "")
public class Farm implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /**
     * 养殖场名称 (如: 顺德一区基地)
     */
    @TableField("farm_name")
    @Schema(description = "养殖场名称 (如: 顺德一区基地)")
    private String farmName;

    /**
     * 经度 (支撑监管方 GIS 地图)
     */
    @TableField("longitude")
    @Schema(description = "经度")
    private BigDecimal longitude;

    /**
     * 纬度 (支撑监管方 GIS 地图)
     */
    @TableField("latitude")
    @Schema(description = "纬度")
    private BigDecimal latitude;

    /**
     * 详细地址 (支撑监管方 GIS 地图)
     */
    @TableField("address")
    @Schema(description = "详细地址")
    private String address;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
