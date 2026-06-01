package com.Zx1nggg.FAMS.modules.lifecycle.entity;

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
import java.time.LocalDateTime;

/**
 * <p>
 * 日常巡塘台账主表：每次巡塘一条记录
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-31
 */
@Getter
@Setter
@ToString
@TableName("t_patrol_log")
@Schema(name = "PatrolLog", description = "日常巡塘台账主表：每次巡塘一条记录")
public class PatrolLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("pond_id")
    @Schema(description = "巡塘的池塘ID")
    private Long pondId;

    @TableField("batch_no")
    @Schema(description = "关联批次号（混合养殖时指定，可选）")
    private String batchNo;

    @TableField("patrol_time")
    @Schema(description = "巡塘时间（一日多巡的核心字段）")
    private LocalDateTime patrolTime;

    @TableField("weather")
    @Schema(description = "天气（晴/阴/雨）")
    private String weather;

    @TableField("water_temp")
    @Schema(description = "水温感官估算(°C)")
    private BigDecimal waterTemp;

    @TableField("water_color")
    @Schema(description = "水色感官（翠绿/黄绿/浑浊/发红）")
    private String waterColor;

    @TableField("operator_id")
    @Schema(description = "巡塘人ID")
    private Long operatorId;

    @TableField("remark")
    @Schema(description = "巡塘综合备注")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}
