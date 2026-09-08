package com.Zx1nggg.FAMS.modules.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SupplierDTO {

    @NotBlank(message = "供应商名称不能为空")
    private String supplierName;

    private String contactPerson;

    private String contactPhone;

    private String qualificationCode;

    /** 该供应商经核准可供应的苗种品类。 */
    @NotEmpty(message = "请至少选择一个可供应苗种")
    private List<Long> seedlingIds;
}
