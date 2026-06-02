package com.Zx1nggg.FAMS.modules.base.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StockingVO {
    private Long id;
    private Long batchId;
    private String batchNo;
    private Long seedlingId;
    private String seedlingName;
    private Byte batchStatus;
    private String purchaseUnit;
    private Integer densityPerUnit;
    private Integer estimatedTotalQty;
    private Long pondId;
    private String pondName;
    private Long farmId;
    private String farmName;
    private Integer stockedUnits;
    private Integer stockedQty;
    private BigDecimal stockedWeight;
    private LocalDate stockingDate;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
