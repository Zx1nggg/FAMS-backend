package com.Zx1nggg.FAMS.modules.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * <p>
 * 
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("t_purchase_batch")
@Schema(name = "PurchaseBatch", description = "")
public class PurchaseBatch implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 确立 Where 属性: 进了哪个场
     */
    @TableField("farm_id")
    @Schema(description = "确立 Where 属性: 进了哪个场")
    private Long farmId;

    /**
     * What: 唯一批次号
     */
    @TableField("batch_no")
    @Schema(description = "What: 唯一批次号")
    private String batchNo;

    /**
     * 供应商
     */
    @TableField("supplier_id")
    @Schema(description = "供应商")
    private Long supplierId;

    /**
     * 采购包装单位 (如: 袋, 箱)
     */
    @TableField("purchase_unit")
    @Schema(description = "采购包装单位 (如: 袋, 箱)")
    private String purchaseUnit;

    /**
     * 包装件数 (如: 50袋)
     */
    @TableField("unit_qty")
    @Schema(description = "包装件数 (如: 50袋)")
    private Integer unitQty;

    /**
     * 每包装预估密度 (如: 2000尾/袋)
     */
    @TableField("density_per_unit")
    @Schema(description = "每包装预估密度 (如: 2000尾/袋)")
    private Integer densityPerUnit;

    /**
     * 系统换算总尾数 (件数 * 密度)
     */
    @TableField("estimated_total_qty")
    @Schema(description = "系统换算总尾数 (件数 * 密度)")
    private Integer estimatedTotalQty;

    /**
     * Why 状态机: 0-待检疫, 1-已检疫入库, 2-养殖中, 3-已出库结算
     */
    @TableField("batch_status")
    @Schema(description = "Why 状态机: 0-待检疫, 1-已检疫入库, 2-养殖中, 3-已出库结算")
    private Byte batchStatus;

    /**
     * 检疫证号
     */
    @Schema(description = "检疫证号")
    @TableField("quarantine_cert_no")
    private String quarantineCertNo;

    @TableField("purchase_date")
    private LocalDate purchaseDate;
}
