package com.Zx1nggg.FAMS.modules.regulator.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TraceBatchVO {
    private Long id;
    private Long farmId;
    private String farmName;
    private String batchNo;
    private String seedlingName;
    private String supplierName;
    private Integer estimatedTotalQty;
    private Byte batchStatus;
    private LocalDate purchaseDate;
}
