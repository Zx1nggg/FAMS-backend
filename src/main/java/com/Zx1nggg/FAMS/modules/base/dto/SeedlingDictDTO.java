package com.Zx1nggg.FAMS.modules.base.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeedlingDictDTO {

    @NotBlank(message = "品种名称不能为空")
    private String categoryName;

    private Integer growthCycleDays;

    private BigDecimal allowableMortalityRate;

    private BigDecimal minTemp;

    private BigDecimal minDo;
}
