package com.Zx1nggg.FAMS.modules.lifecycle.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PatrolLogDTO {

    @NotNull(message = "池塘不能为空")
    private Long pondId;

    private String batchNo;

    @NotNull(message = "巡塘时间不能为空")
    private LocalDateTime patrolTime;

    private String weather;

    private BigDecimal waterTemp;

    private String waterColor;

    private String remark;
}
