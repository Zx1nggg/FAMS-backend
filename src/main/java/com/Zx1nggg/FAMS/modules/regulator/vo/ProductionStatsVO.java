package com.Zx1nggg.FAMS.modules.regulator.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductionStatsVO {
    private BigDecimal totalProductionKg;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal netProfit;
    private Integer harvestCount;
    private Integer participatingFarmCount;
    private BigDecimal avgUnitPrice;
}
