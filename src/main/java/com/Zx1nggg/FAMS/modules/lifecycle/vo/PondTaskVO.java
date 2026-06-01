package com.Zx1nggg.FAMS.modules.lifecycle.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PondTaskVO {
    private Long id;
    private Long pondId;
    private String pondName;
    private String batchNo;
    private String taskType;
    private String taskDesc;
    private LocalDate scheduledDate;
    private Byte status;
    private LocalDateTime finishTime;
    private Long operatorId;
    private LocalDateTime updateTime;
}
