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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 基于SOP引擎自动生成的池塘每日待办任务表
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("t_pond_task")
@Schema(name = "PondTask", description = "基于SOP引擎自动生成的池塘每日待办任务表")
public class PondTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务关联的具体池塘ID
     */
    @TableField("pond_id")
    @Schema(description = "任务关联的具体池塘ID")
    private Long pondId;

    /**
     * 任务关联的具体批次号 (防混养干扰)
     */
    @TableField("batch_no")
    @Schema(description = "任务关联的具体批次号 (防混养干扰)")
    private String batchNo;

    /**
     * 任务类型 (继承自模板)
     */
    @TableField("task_type")
    @Schema(description = "任务类型 (继承自模板)")
    private String taskType;

    /**
     * 任务说明 (继承自模板)
     */
    @TableField("task_desc")
    @Schema(description = "任务说明 (继承自模板)")
    private String taskDesc;

    /**
     * 计划执行日期 (核心！由系统根据: 下塘日期 + day_offset 自动算出来)
     */
    @TableField("scheduled_date")
    @Schema(description = "计划执行日期 (核心！由系统根据: 下塘日期 + day_offset 自动算出来)")
    private LocalDate scheduledDate;

    /**
     * 任务状态机: 0-待执行, 1-已打卡完成, 2-已逾期未做 (红色警告)
     */
    @TableField("status")
    @Schema(description = "任务状态机: 0-待执行, 1-已打卡完成, 2-已逾期未做 (红色警告)")
    private Byte status;

    /**
     * 养殖户实际点击\"打卡\"的时间
     */
    @TableField("finish_time")
    @Schema(description = "养殖户实际点击\"打卡\"的时间")
    private LocalDateTime finishTime;

    /**
     * 执行此任务的养殖户ID
     */
    @TableField("operator_id")
    @Schema(description = "执行此任务的养殖户ID")
    private Long operatorId;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
