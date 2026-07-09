package com.Zx1nggg.FAMS.modules.regulator.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IotRealtimeAlertVO {
    private Long pondId;
    private String pondName;
    private Long farmId;
    private String farmName;
    private BigDecimal waterTemp;
    private BigDecimal doLevel;
    private BigDecimal phLevel;
    private String alertField;
    private BigDecimal currentValue;
    private BigDecimal thresholdMin;
    private BigDecimal thresholdMax;
    private LocalDateTime dataTime;
}
