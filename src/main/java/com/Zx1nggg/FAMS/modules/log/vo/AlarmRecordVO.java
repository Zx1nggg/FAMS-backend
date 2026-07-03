package com.Zx1nggg.FAMS.modules.log.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AlarmRecordVO {
    private Long id;
    private Long farmId;
    private String farmName;
    private Long pondId;
    private String pondName;
    private Long ruleId;
    private String alarmCode;
    private String title;
    private String message;
    private String sourceType;
    private Long sourceId;
    private Byte severity;
    private Byte status;
    private String metricCode;
    private BigDecimal triggerValue;
    private String thresholdOperator;
    private BigDecimal thresholdValue;
    private BigDecimal thresholdValueHigh;
    private String metricUnit;
    private Integer occurrenceCount;
    private LocalDateTime firstOccurredAt;
    private LocalDateTime lastOccurredAt;
    private Long acknowledgedBy;
    private String acknowledgedByName;
    private LocalDateTime acknowledgedAt;
    private Long resolvedBy;
    private String resolvedByName;
    private LocalDateTime resolvedAt;
    private String resolutionRemark;
    private LocalDateTime recoveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}