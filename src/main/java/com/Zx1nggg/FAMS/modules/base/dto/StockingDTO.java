package com.Zx1nggg.FAMS.modules.base.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StockingDTO {

    @NotNull(message = "批次不能为空")
    private Long batchId;

    @NotNull(message = "池塘不能为空")
    private Long pondId;

    @NotNull(message = "投放件数不能为空")
    private Integer stockedUnits;

    private BigDecimal stockedWeight;

    @NotNull(message = "投放日期不能为空")
    private LocalDate stockingDate;

    private String remark;
}
