package com.Zx1nggg.FAMS.modules.base.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FarmDTO {

    @NotBlank(message = "养殖场名称不能为空")
    private String farmName;

    private Long userId;
}
