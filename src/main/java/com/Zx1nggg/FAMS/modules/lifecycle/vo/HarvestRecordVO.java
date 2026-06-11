package com.Zx1nggg.FAMS.modules.lifecycle.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出塘结算响应 VO
 *
 * @author Zx1nggg
 * @since 2026-06-11
 */
@Data
public class HarvestRecordVO {

    private Long id;
    private String batchNo;
    private Long pondId;
    private String pondName;
    private Long farmId;
    private String farmName;

    /** 苗种品种名称（从批次关联） */
    private String seedlingName;

    /** 出塘日期 */
    private LocalDate harvestDate;

    /** 预估产量(kg) */
    private BigDecimal predictedWeightKg;

    /** 实际过磅总重(kg) */
    private BigDecimal actualTotalWeightKg;

    /** 最终出池抽测均重(g/尾) */
    private BigDecimal actualAvgWeightG;

    /** 出塘单价(元/kg) */
    private BigDecimal unitPrice;

    /** 总收入(元) */
    private BigDecimal totalRevenue;

    /** 苗种成本(元) */
    private BigDecimal seedlingCost;

    /** 饲料成本(元) */
    private BigDecimal feedCost;

    /** 药品成本(元) */
    private BigDecimal medicineCost;

    /** 其他成本(元) */
    private BigDecimal otherCost;

    /** 总成本(元) */
    private BigDecimal totalCost;

    /** 净利润(元) */
    private BigDecimal netProfit;

    /** 结算状态: 0=未结算 1=已结算 */
    private Integer settlementStatus;

    /** 收购方 */
    private String buyerName;

    /** 溯源二维码 URL */
    private String traceQrCodeUrl;

    /** 二维码扫码次数 */
    private Integer traceQueryCount;

    /** 备注 */
    private String remark;

    /** 经手人 ID */
    private Long operatorId;

    // ---- 关联 Stocking 信息 ----
    /** 投放总尾数 */
    private Integer stockedQty;
    /** 投放日期 */
    private LocalDate stockingDate;

    // ---- 关联 PurchaseBatch 信息 ----
    /** 批次状态 */
    private Byte batchStatus;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
