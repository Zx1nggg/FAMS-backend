package com.Zx1nggg.FAMS.modules.base.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PurchaseBatchDTO {

    private Long farmId;

    private String batchNo;

    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @NotNull(message = "苗种品种不能为空")
    private Long seedlingId;

    private String purchaseUnit;

    private Integer unitQty;

    private Integer densityPerUnit;

    private Integer estimatedTotalQty;

    private Byte batchStatus;

    private String quarantineCertNo;

    private LocalDate purchaseDate;
}
