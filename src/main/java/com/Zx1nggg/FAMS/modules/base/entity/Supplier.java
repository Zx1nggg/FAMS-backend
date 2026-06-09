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
import java.time.LocalDateTime;

/**
 * <p>
 * 苗种供应商/培育基地档案表
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-07
 */
@Getter
@Setter
@ToString
@TableName("t_supplier")
@Schema(name = "Supplier", description = "苗种供应商/培育基地档案表")
public class Supplier implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 供应商名称 (如: 湛江海联水产种苗基地)
     */
    @TableField("supplier_name")
    @Schema(description = "供应商名称 (如: 湛江海联水产种苗基地)")
    private String supplierName;

    /**
     * 联系人
     */
    @Schema(description = "联系人")
    @TableField("contact_person")
    private String contactPerson;

    /**
     * 联系电话
     */
    @TableField("contact_phone")
    @Schema(description = "联系电话")
    private String contactPhone;

    /**
     * 水产苗种生产许可证号
     */
    @TableField("qualification_code")
    @Schema(description = "水产苗种生产许可证号")
    private String qualificationCode;

    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 所属用户ID（FARMER仅可见自己的供应商，同一用户多场区共享）
     */
    @TableField("user_id")
    @Schema(description = "所属用户ID（FARMER仅可见自己的供应商）")
    private Long userId;
}
