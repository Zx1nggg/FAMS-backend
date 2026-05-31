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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 投放登记表：批次与池塘的多对多关联
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-31
 */
@Getter
@Setter
@ToString
@TableName("t_stocking")
@Schema(name = "Stocking", description = "投放登记表：批次与池塘的多对多关联")
public class Stocking implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 批次ID
     */
    @TableField("batch_id")
    @Schema(description = "批次ID")
    private Long batchId;

    /**
     * 池塘ID
     */
    @TableField("pond_id")
    @Schema(description = "池塘ID")
    private Long pondId;

    /**
     * 投放件数（袋/箱数）
     */
    @TableField("stocked_units")
    @Schema(description = "投放件数（袋/箱数）")
    private Integer stockedUnits;

    /**
     * 系统换算尾数 = stocked_units × 批次.density_per_unit
     */
    @TableField("stocked_qty")
    @Schema(description = "系统换算尾数 = stocked_units × 批次.density_per_unit")
    private Integer stockedQty;

    /**
     * 投放总重(kg)，可选
     */
    @TableField("stocked_weight")
    @Schema(description = "投放总重(kg)，可选")
    private BigDecimal stockedWeight;

    /**
     * 投放日期
     */
    @TableField("stocking_date")
    @Schema(description = "投放日期")
    private LocalDate stockingDate;

    /**
     * 备注
     */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
