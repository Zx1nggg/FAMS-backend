package com.Zx1nggg.FAMS.modules.lifecycle.entity;

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
 * 批次生物生长与死亡抽测记录
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("t_batch_growth_log")
@Schema(name = "BatchGrowthLog", description = "批次生物生长与死亡抽测记录")
public class BatchGrowthLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联巡塘记录ID
     */
    @TableField("patrol_log_id")
    @Schema(description = "关联巡塘记录ID")
    private Long patrolLogId;

    /**
     * 针对哪个批次
     */
    @TableField("batch_no")
    @Schema(description = "针对哪个批次")
    private String batchNo;

    /**
     * 发生在哪口池塘
     */
    @TableField("pond_id")
    @Schema(description = "发生在哪口池塘")
    private Long pondId;

    @TableField("log_date")
    private LocalDate logDate;

    /**
     * 抽测均长(cm)
     */
    @TableField("avg_length")
    @Schema(description = "抽测均长(cm)")
    private BigDecimal avgLength;

    /**
     * 抽测均重(g)
     */
    @TableField("avg_weight")
    @Schema(description = "抽测均重(g)")
    private BigDecimal avgWeight;

    /**
     * 日常合理损耗(尾) - 算入正常死亡率
     */
    @TableField("routine_death_count")
    @Schema(description = "日常合理损耗(尾) - 算入正常死亡率")
    private Integer routineDeathCount;

    /**
     * 异常突发死亡数(尾) - 必须填原因
     */
    @TableField("abnormal_death_count")
    @Schema(description = "异常突发死亡数(尾) - 必须填原因")
    private Integer abnormalDeathCount;

    /**
     * 异常死亡原因 (如: 用药不当, 停电缺氧)
     */
    @TableField("abnormal_reason")
    @Schema(description = "异常死亡原因 (如: 用药不当, 停电缺氧)")
    private String abnormalReason;
}
