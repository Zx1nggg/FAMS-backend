package com.Zx1nggg.FAMS.modules.regulator.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SurvivalRateVO {
    private String dimKey;
    private String dimLabel;
    private Long farmId;
    private String farmName;
    private Long seedlingId;
    private String seedlingName;
    private Long stockedQty;
    private Long estimatedHarvestQty;
    private Long deathQty;
    private BigDecimal totalHarvestWeightKg;
    private BigDecimal survivalRate;
    private BigDecimal avgWeightG;
    private Integer batchCount;
}
