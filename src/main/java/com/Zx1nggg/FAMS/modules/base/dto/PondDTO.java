package com.Zx1nggg.FAMS.modules.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PondDTO {

    private Long farmId;

    @NotBlank(message = "池塘编号不能为空")
    private String pondName;

    @NotNull(message = "面积不能为空")
    private BigDecimal areaMu;
}
