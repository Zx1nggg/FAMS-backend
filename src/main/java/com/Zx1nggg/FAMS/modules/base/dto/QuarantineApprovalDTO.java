package com.Zx1nggg.FAMS.modules.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 监管方对待检疫采购批次签发检疫合格证明。 */
@Data
public class QuarantineApprovalDTO {

    @NotBlank(message = "检疫合格证号不能为空")
    @Size(max = 100, message = "检疫合格证号不能超过100个字符")
    private String quarantineCertNo;
}
