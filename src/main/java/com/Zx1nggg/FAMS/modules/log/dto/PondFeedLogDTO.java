package com.Zx1nggg.FAMS.modules.log.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PondFeedLogDTO {

    private Long patrolLogId;

    @NotNull(message = "池塘不能为空")
    private Long pondId;

    @NotNull(message = "操作日期不能为空")
    private LocalDate logDate;

    private String feedBrand;

    private BigDecimal feedAmount;

    private String waterChangeStatus;
}
