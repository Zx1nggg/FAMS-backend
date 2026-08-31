package com.Zx1nggg.FAMS.modules.regulator.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SurvivalTrendVO {
    private String month;
    private BigDecimal avgSurvivalRate;
    private BigDecimal maxRate;
    private BigDecimal minRate;
    private Integer batchCount;
}
