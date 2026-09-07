package com.Zx1nggg.FAMS.modules.base.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class FarmDTO {

    @NotBlank(message = "养殖场名称不能为空")
    private String farmName;

    private Long userId;

    @jakarta.validation.constraints.DecimalMin("-180")
    @jakarta.validation.constraints.DecimalMax("180")
    private BigDecimal longitude;

    @jakarta.validation.constraints.DecimalMin("-90")
    @jakarta.validation.constraints.DecimalMax("90")
    private BigDecimal latitude;

    private String address;
}
