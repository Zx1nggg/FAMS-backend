package com.Zx1nggg.FAMS.modules.log.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PondFeedLogVO {
    private Long id;
    private Long patrolLogId;
    private Long pondId;
    private String pondName;
    private String batchNo;
    private LocalDate logDate;
    private String feedBrand;
    private BigDecimal feedAmount;
    private String waterChangeStatus;
}
