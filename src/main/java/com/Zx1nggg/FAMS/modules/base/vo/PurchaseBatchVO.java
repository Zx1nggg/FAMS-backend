package com.Zx1nggg.FAMS.modules.base.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PurchaseBatchVO {
    private Long id;
    private Long farmId;
    private String farmName;
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
    private Long quarantineReviewerId;
    private LocalDateTime quarantineReviewedAt;
    /** 单价(元/件) */
    private BigDecimal unitPrice;

    /** 总金额(元) */
    private BigDecimal totalAmount;

    private LocalDate purchaseDate;
}
