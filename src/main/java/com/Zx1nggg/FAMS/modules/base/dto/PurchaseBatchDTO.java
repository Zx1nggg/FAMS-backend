package com.Zx1nggg.FAMS.modules.base.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PurchaseBatchDTO {

    private Long farmId;

    private String batchNo;

    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @NotNull(message = "苗种品种不能为空")
    private Long seedlingId;

    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 20)
    private String purchaseUnit;

    @NotNull
    @jakarta.validation.constraints.Positive
    private Integer unitQty;

    @NotNull
    @jakarta.validation.constraints.Positive
    private Integer densityPerUnit;

    private Integer estimatedTotalQty;

    private Byte batchStatus;

    private String quarantineCertNo;

    /** 单价(元/件) */
    @jakarta.validation.constraints.PositiveOrZero
    private BigDecimal unitPrice;

    @NotNull
    private LocalDate purchaseDate;
}
