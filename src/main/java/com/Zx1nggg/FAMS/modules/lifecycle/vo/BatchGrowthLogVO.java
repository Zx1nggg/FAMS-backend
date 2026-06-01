package com.Zx1nggg.FAMS.modules.lifecycle.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BatchGrowthLogVO {
    private Long id;
    private Long patrolLogId;
    private String batchNo;
    private Long pondId;
    private String pondName;
    private LocalDate logDate;
    private BigDecimal avgLength;
    private BigDecimal avgWeight;
    private Integer routineDeathCount;
    private Integer abnormalDeathCount;
    private String abnormalReason;
}
