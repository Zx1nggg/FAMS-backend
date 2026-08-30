package com.Zx1nggg.FAMS.modules.regulator.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlertHandleDTO {
    @Min(value = 0, message = "状态码必须在 0 到 4 之间")
    @Max(value = 4, message = "状态码必须在 0 到 4 之间")
    private Byte status;

    @Size(max = 500, message = "remark must not exceed 500 characters")
    private String remark;
}
