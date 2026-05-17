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
import java.time.LocalDateTime;

/**
 * <p>
 * 系统异常告警与处理记录表
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("sys_alarm_record")
@Schema(name = "AlarmRecord", description = "系统异常告警与处理记录表")
public class AlarmRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 接收告警的养殖场ID
     */
    @TableField("farm_id")
    @Schema(description = "接收告警的养殖场ID")
    private Long farmId;

    /**
     * 告警级别: 1提示, 2警告, 3严重 (如: 溶氧极低)
     */
    @TableField("alarm_level")
    @Schema(description = "告警级别: 1提示, 2警告, 3严重 (如: 溶氧极低)")
    private Byte alarmLevel;

    /**
     * 告警类型: IOT_TEMP(水温超标), IOT_DO(缺氧), BIOLOGY(突发死苗)
     */
    @TableField("alarm_type")
    @Schema(description = "告警类型: IOT_TEMP(水温超标), IOT_DO(缺氧), BIOLOGY(突发死苗)")
    private String alarmType;

    /**
     * 告警详情描述 (如: 1号池溶氧量低至2.5mg/L，请立即开启增氧设备！)
     */
    @TableField("alarm_content")
    @Schema(description = "告警详情描述 (如: 1号池溶氧量低至2.5mg/L，请立即开启增氧设备！)")
    private String alarmContent;

    /**
     * 处理状态: 0未处理, 1已处理
     */
    @TableField("is_handled")
    @Schema(description = "处理状态: 0未处理, 1已处理")
    private Byte isHandled;

    /**
     * 养殖户点击\"已知晓/已处理\"的时间
     */
    @TableField("handle_time")
    @Schema(description = "养殖户点击\"已知晓/已处理\"的时间")
    private LocalDateTime handleTime;

    /**
     * 告警发生时间
     */
    @TableField("create_time")
    @Schema(description = "告警发生时间")
    private LocalDateTime createTime;
}
