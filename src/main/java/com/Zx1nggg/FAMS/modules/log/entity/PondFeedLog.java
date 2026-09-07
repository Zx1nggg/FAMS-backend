package com.Zx1nggg.FAMS.modules.log.entity;

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
import java.time.LocalDate;

/**
 * <p>
 * 池塘环境与投喂作业日志(无视批次)
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("t_pond_feed_log")
@Schema(name = "PondFeedLog", description = "池塘环境与投喂作业日志(无视批次)")
public class PondFeedLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联巡塘记录ID
     */
    @TableField(value = "patrol_log_id", updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    @Schema(description = "关联巡塘记录ID")
    private Long patrolLogId;

    /**
     * 针对哪个池塘
     */
    @TableField("pond_id")
    @Schema(description = "针对哪个池塘")
    private Long pondId;

    /**
     * 操作日期
     */
    @TableField("log_date")
    @Schema(description = "操作日期")
    private LocalDate logDate;

    /**
     * 饲料品牌
     */
    @TableField(value = "feed_brand", updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    @Schema(description = "饲料品牌")
    private String feedBrand;

    /**
     * 投饵量(kg)
     */
    @TableField(value = "feed_amount", updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    @Schema(description = "投饵量(kg)")
    private BigDecimal feedAmount;

    /**
     * 换水状态 (如: 换水30%)
     */
    @TableField(value = "water_change_status", updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    @Schema(description = "换水状态 (如: 换水30%)")
    private String waterChangeStatus;

    // ==================== 饲料成本 ====================

    /**
     * 饲料单价(元/kg)
     */
    @TableField(value = "feed_unit_price", updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    @Schema(description = "饲料单价(元/kg)")
    private BigDecimal feedUnitPrice;

    /**
     * 本次投喂金额(元) = feedAmount × feedUnitPrice，自动计算
     */
    @TableField(value = "feed_total_amount", updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    @Schema(description = "本次投喂金额(元) = feedAmount × feedUnitPrice")
    private BigDecimal feedTotalAmount;

    // ==================== 药品记录 ====================

    /**
     * 药品名称
     */
    @TableField(value = "medicine_name", updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    @Schema(description = "药品名称")
    private String medicineName;

    /**
     * 用量
     */
    @TableField(value = "medicine_dosage", updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    @Schema(description = "用量")
    private BigDecimal medicineDosage;

    /**
     * 用量单位 (ml/g/袋)
     */
    @TableField(value = "medicine_unit", updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    @Schema(description = "用量单位 (ml/g/袋)")
    private String medicineUnit;

    /**
     * 本次药费(元)
     */
    @TableField(value = "medicine_amount", updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    @Schema(description = "本次药费(元)")
    private BigDecimal medicineAmount;
}
