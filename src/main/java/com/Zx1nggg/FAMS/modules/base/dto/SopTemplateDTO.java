package com.Zx1nggg.FAMS.modules.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SopTemplateDTO {

    @NotNull(message = "适用苗种不能为空")
    private Long categoryId;

    @NotBlank(message = "养殖阶段不能为空")
    private String stageName;

    @NotNull(message = "时间偏移量不能为空")
    private Integer dayOffset;

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    @NotBlank(message = "操作指南不能为空")
    private String taskDesc;
}
