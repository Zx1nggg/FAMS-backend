package com.Zx1nggg.FAMS.modules.base.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeedlingDictVO {
    private Long id;
    private String categoryName;
    private Integer growthCycleDays;
    private BigDecimal allowableMortalityRate;
    private BigDecimal minTemp;
    private BigDecimal minDo;
}
