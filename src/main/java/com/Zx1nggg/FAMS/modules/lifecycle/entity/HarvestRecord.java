package com.Zx1nggg.FAMS.modules.lifecycle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 出塘结算与消费者防伪溯源凭证表
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("t_harvest_record")
@Schema(name = "HarvestRecord", description = "出塘结算与消费者防伪溯源凭证表")
public class HarvestRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 大结局主线：哪个批次出塘了 (一对一关系)
     */
    @TableField("batch_no")
    @Schema(description = "大结局主线：哪个批次出塘了 (一对一关系)")
    private String batchNo;

    /**
     * 从哪个池塘捞出的
     */
    @TableField("pond_id")
    @Schema(description = "从哪个池塘捞出的")
    private Long pondId;

    /**
     * 实际出塘结算日期
     */
    @TableField("harvest_date")
    @Schema(description = "实际出塘结算日期")
    private LocalDate harvestDate;

    /**
     * 算法预测产量(kg) (供后期做算法精确度误差分析)
     */
    @TableField("predicted_weight_kg")
    @Schema(description = "算法预测产量(kg) (供后期做算法精确度误差分析)")
    private BigDecimal predictedWeightKg;

    /**
     * 实际过磅总产量(kg)
     */
    @Schema(description = "实际过磅总产量(kg)")
    @TableField("actual_total_weight_kg")
    private BigDecimal actualTotalWeightKg;

    /**
     * 最终出池抽测均重(g/尾)
     */
    @TableField("actual_avg_weight_g")
    @Schema(description = "最终出池抽测均重(g/尾)")
    private BigDecimal actualAvgWeightG;

    /**
     * 收购方/去向 (满足国家《销售记录》合规要求)
     */
    @TableField("buyer_name")
    @Schema(description = "收购方/去向 (满足国家《销售记录》合规要求)")
    private String buyerName;

    /**
     * 系统自动生成的C端溯源H5页面链接 (前端转成二维码展示)
     */
    @TableField("trace_qr_code_url")
    @Schema(description = "系统自动生成的C端溯源H5页面链接 (前端转成二维码展示)")
    private String traceQrCodeUrl;

    /**
     * 消费者扫码查询次数 (防伪被恶意盗刷的校验手段)
     */
    @TableField("trace_query_count")
    @Schema(description = "消费者扫码查询次数 (防伪被恶意盗刷的校验手段)")
    private Integer traceQueryCount;

    /**
     * 经手人
     */
    @TableField("operator_id")
    @Schema(description = "经手人")
    private Long operatorId;

    @TableField("create_time")
    private LocalDateTime createTime;
}
