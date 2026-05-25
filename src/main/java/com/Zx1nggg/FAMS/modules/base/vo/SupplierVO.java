package com.Zx1nggg.FAMS.modules.base.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SupplierVO {
    private Long id;
    private String supplierName;
    private String contactPerson;
    private String contactPhone;
    private String qualificationCode;
    private LocalDateTime createTime;
}
