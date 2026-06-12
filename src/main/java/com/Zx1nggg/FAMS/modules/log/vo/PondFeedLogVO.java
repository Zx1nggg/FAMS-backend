package com.Zx1nggg.FAMS.modules.log.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PondFeedLogVO {
    private Long id;
    private Long patrolLogId;
    private Long pondId;
    private String pondName;
    private String batchNo;
    private LocalDate logDate;
    private String feedBrand;
    private BigDecimal feedAmount;
    private String waterChangeStatus;

    // ==================== 饲料成本 ====================

    private BigDecimal feedUnitPrice;

    private BigDecimal feedTotalAmount;

    // ==================== 药品记录 ====================

    private String medicineName;

    private BigDecimal medicineDosage;

    private String medicineUnit;

    private BigDecimal medicineAmount;
}
