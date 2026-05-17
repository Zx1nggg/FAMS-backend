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
import java.time.LocalDateTime;

/**
 * <p>
 * 标准化养殖 SOP 规则模板表
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("t_sop_template")
@Schema(name = "SopTemplate", description = "标准化养殖 SOP 规则模板表")
public class SopTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 适用苗种字典ID (关联 t_seedling_dict，如: 南美白对虾)
     */
    @TableField("category_id")
    @Schema(description = "适用苗种字典ID (关联 t_seedling_dict，如: 南美白对虾)")
    private Long categoryId;

    /**
     * 养殖阶段 (如: 苗期、标粗期、育肥期)
     */
    @TableField("stage_name")
    @Schema(description = "养殖阶段 (如: 苗期、标粗期、育肥期)")
    private String stageName;

    /**
     * 时间偏移量 (定义规则：如下塘后第 15 天执行)
     */
    @TableField("day_offset")
    @Schema(description = "时间偏移量 (定义规则：如下塘后第 15 天执行)")
    private Integer dayOffset;

    /**
     * 任务类型 (枚举值: DISINFECT-消毒, TEST-抽测, WATER-换水, FEED-特殊投喂)
     */
    @TableField("task_type")
    @Schema(description = "任务类型 (枚举值: DISINFECT-消毒, TEST-抽测, WATER-换水, FEED-特殊投喂)")
    private String taskType;

    /**
     * 标准操作指南/作业要求 (如: 使用聚维酮碘全池泼洒消毒)
     */
    @TableField("task_desc")
    @Schema(description = "标准操作指南/作业要求 (如: 使用聚维酮碘全池泼洒消毒)")
    private String taskDesc;

    @TableField("create_time")
    private LocalDateTime createTime;
}
