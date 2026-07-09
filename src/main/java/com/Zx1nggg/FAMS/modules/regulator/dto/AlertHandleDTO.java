package com.Zx1nggg.FAMS.modules.regulator.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlertHandleDTO {
    @Min(value = 0, message = "status must be between 0 and 4")
    @Max(value = 4, message = "status must be between 0 and 4")
    private Byte status;

    @Size(max = 500, message = "remark must not exceed 500 characters")
    private String remark;
}
