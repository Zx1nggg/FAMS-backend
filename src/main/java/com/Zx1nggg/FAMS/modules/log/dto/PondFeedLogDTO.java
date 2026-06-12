package com.Zx1nggg.FAMS.modules.log.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Data
public class PondFeedLogDTO {

    private Long patrolLogId;

    @NotNull(message = "池塘不能为空")
    private Long pondId;

    @NotNull(message = "操作日期不能为空")
    private LocalDate logDate;

    private String feedBrand;

    private BigDecimal feedAmount;

    private String waterChangeStatus;

    // ==================== 饲料成本 ====================

    private BigDecimal feedUnitPrice;

    // ==================== 药品记录 ====================

    private String medicineName;

    private BigDecimal medicineDosage;

    private String medicineUnit;

    private BigDecimal medicineAmount;

    /**
     * 至少填一项：饲料品牌+用量、或药品名称+药费、或两者都填
     */
    public boolean hasAnyContent() {
        return (feedBrand != null && feedAmount != null)
                || (medicineName != null && medicineAmount != null);
    }
}
