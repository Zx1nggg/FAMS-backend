package com.Zx1nggg.FAMS.modules.lifecycle.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 出塘结算请求 DTO
 *
 * @author Zx1nggg
 * @since 2026-06-11
 */
@Data
public class HarvestRecordDTO {

    @NotNull(message = "批次号不能为空")
    private String batchNo;

    @NotNull(message = "池塘不能为空")
    private Long pondId;

    @NotNull(message = "出塘日期不能为空")
    private LocalDate harvestDate;

    @NotNull(message = "实际过磅总重不能为空")
    private BigDecimal actualTotalWeightKg;

    /** 最终出池抽测均重(g/尾) */
    private BigDecimal actualAvgWeightG;

    /** 算法预测产量(kg) */
    private BigDecimal predictedWeightKg;

    /** 出塘单价(元/kg) */
    private BigDecimal unitPrice;

    /** 苗种成本(元) */
    private BigDecimal seedlingCost;

    /** 饲料成本(元) */
    private BigDecimal feedCost;

    /** 药品成本(元) */
    private BigDecimal medicineCost;

    /** 其他成本(元) */
    private BigDecimal otherCost;

    /** 收购方/去向 */
    private String buyerName;

    /** 备注 */
    private String remark;

    /** 结算状态: 0=未结算 1=已结算。新增时前端显式传入1，更新时由 calculateAmounts 自动判断 */
    private Integer settlementStatus;
}
