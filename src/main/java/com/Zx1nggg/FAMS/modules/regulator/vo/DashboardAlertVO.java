package com.Zx1nggg.FAMS.modules.regulator.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DashboardAlertVO {
    private Long alarmId;
    private Long farmId;
    private String farmName;
    private Long pondId;
    private String alarmCode;
    private String title;
    private String message;
    private Byte severity;
    private Byte status;
    private Integer occurrenceCount;
    private LocalDateTime lastOccurredAt;
}