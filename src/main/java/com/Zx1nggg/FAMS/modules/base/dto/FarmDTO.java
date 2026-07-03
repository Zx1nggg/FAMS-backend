package com.Zx1nggg.FAMS.modules.base.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class FarmDTO {

    @NotBlank(message = "养殖场名称不能为空")
    private String farmName;

    private Long userId;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String address;
}
