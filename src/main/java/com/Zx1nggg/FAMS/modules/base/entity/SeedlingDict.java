package com.Zx1nggg.FAMS.modules.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 苗种分类字典
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("t_seedling_dict")
@Schema(name = "SeedlingDict", description = "苗种分类字典")
public class SeedlingDict implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 品种名称 (如: 南美白对虾)
     */
    @TableField("category_name")
    @Schema(description = "品种名称 (如: 南美白对虾)")
    private String categoryName;

    /**
     * 标准养殖周期(天)
     */
    @TableField("growth_cycle_days")
    @Schema(description = "标准养殖周期(天)")
    private Integer growthCycleDays;

    /**
     * 自然容许死亡率(%) - 超过此值视为异常
     */
    @TableField("allowable_mortality_rate")
    @Schema(description = "自然容许死亡率(%) - 超过此值视为异常")
    private BigDecimal allowableMortalityRate;

    /**
     * 最低水温
     */
    @TableField("min_temp")
    @Schema(description = "最低水温")
    private BigDecimal minTemp;

    /**
     * 最低溶氧量(mg/L)
     */
    @TableField("min_do")
    @Schema(description = "最低溶氧量(mg/L)")
    private BigDecimal minDo;
}
