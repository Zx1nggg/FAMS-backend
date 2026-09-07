package com.Zx1nggg.FAMS.modules.lifecycle.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BatchGrowthLogDTO {

    private Long patrolLogId;

    @NotNull(message = "批次号不能为空")
    private String batchNo;

    @NotNull(message = "池塘不能为空")
    private Long pondId;

    @NotNull(message = "日期不能为空")
    private LocalDate logDate;

    @jakarta.validation.constraints.PositiveOrZero
    private BigDecimal avgLength;

    @jakarta.validation.constraints.PositiveOrZero
    private BigDecimal avgWeight;

    @jakarta.validation.constraints.PositiveOrZero
    private Integer routineDeathCount;

    @jakarta.validation.constraints.PositiveOrZero
    private Integer abnormalDeathCount;

    private String abnormalReason;
}
