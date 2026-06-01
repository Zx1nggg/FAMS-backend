package com.Zx1nggg.FAMS.modules.lifecycle.dto;

import lombok.Data;

@Data
public class PondTaskDTO {

    private Long pondId;

    private String batchNo;

    private String taskType;

    private String taskDesc;

    private java.time.LocalDate scheduledDate;

    private Byte status;
}
