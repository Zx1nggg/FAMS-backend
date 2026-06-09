package com.Zx1nggg.FAMS.modules.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批请求参数 DTO
 */
@Data
public class ApprovalReqDTO {

    @NotNull(message = "审批状态不能为空")
    @Min(value = 1, message = "审批状态只能为1(通过)或2(拒绝)")
    @Max(value = 2, message = "审批状态只能为1(通过)或2(拒绝)")
    private Integer status;

    /**
     * 审批意见（通过时可选，拒绝时必填）
     */
    private String reviewComment;
}
