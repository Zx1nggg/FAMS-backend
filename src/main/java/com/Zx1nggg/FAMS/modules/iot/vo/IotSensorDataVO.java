package com.Zx1nggg.FAMS.modules.iot.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "IoT 传感器数据视图")
@JsonIgnoreProperties(ignoreUnknown = true)
public class IotSensorDataVO {

    @Schema(description = "传感器数据ID")
    private Long id;

    @Schema(description = "池塘ID")
    private Long pondId;

    @Schema(description = "池塘名称")
    private String pondName;

    @Schema(description = "设备SN码")
    private String deviceSn;

    @Schema(description = "实时水温(°C)")
    private BigDecimal waterTemp;

    @Schema(description = "溶氧量(mg/L)")
    private BigDecimal dissolvedOxygen;

    @Schema(description = "pH值")
    private BigDecimal phValue;

    @Schema(description = "采集时间")
    private LocalDateTime collectTime;
}
