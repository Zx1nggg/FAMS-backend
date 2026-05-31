package com.Zx1nggg.FAMS.modules.base.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PurchaseBatchVO {
    private Long id;
    private Long farmId;
    private String batchNo;
    private Long supplierId;
    private String supplierName;
    private Long seedlingId;
    private String seedlingName;
    private String purchaseUnit;
    private Integer unitQty;
    private Integer densityPerUnit;
    private Integer estimatedTotalQty;
    private Byte batchStatus;
    private String quarantineCertNo;
    private LocalDate purchaseDate;
}
