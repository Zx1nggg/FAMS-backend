package com.Zx1nggg.FAMS.modules.regulator.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductionRankingVO {
    private Integer ranking;
    private Long farmId;
    private String farmName;
    private BigDecimal totalProductionKg;
    private BigDecimal totalRevenue;
    private BigDecimal netProfit;
    private Integer harvestCount;
}
