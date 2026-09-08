package com.Zx1nggg.FAMS.modules.base.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SupplierVO {
    private Long id;
    private String supplierName;
    private String contactPerson;
    private String contactPhone;
    private String qualificationCode;
    private LocalDateTime createTime;
    private List<Long> seedlingIds;
    private List<String> seedlingNames;
}
