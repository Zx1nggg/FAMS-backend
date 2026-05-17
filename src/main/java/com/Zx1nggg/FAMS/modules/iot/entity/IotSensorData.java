package com.Zx1nggg.FAMS.modules.iot.entity;

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
import java.time.LocalDateTime;

/**
 * <p>
 * 物联网水质传感器实时流水表
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("t_iot_sensor_data")
@Schema(name = "IotSensorData", description = "物联网水质传感器实时流水表")
public class IotSensorData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 监测的是哪口池塘
     */
    @TableField("pond_id")
    @Schema(description = "监测的是哪口池塘")
    private Long pondId;

    /**
     * 硬件设备SN码
     */
    @TableField("device_sn")
    @Schema(description = "硬件设备SN码")
    private String deviceSn;

    /**
     * 实时水温(°C)
     */
    @TableField("water_temp")
    @Schema(description = "实时水温(°C)")
    private BigDecimal waterTemp;

    /**
     * 溶氧量(mg/L)
     */
    @TableField("dissolved_oxygen")
    @Schema(description = "溶氧量(mg/L)")
    private BigDecimal dissolvedOxygen;

    /**
     * pH值
     */
    @TableField("ph_value")
    @Schema(description = "pH值")
    private BigDecimal phValue;

    /**
     * 传感器采集时间
     */
    @TableField("collect_time")
    @Schema(description = "传感器采集时间")
    private LocalDateTime collectTime;
}
