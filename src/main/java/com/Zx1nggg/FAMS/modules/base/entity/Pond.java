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
import java.math.BigDecimal;

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
@TableName("t_pond")
@Schema(name = "Pond", description = "")
public class Pond implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属养殖场 (Where锁定)
     */
    @TableField("farm_id")
    @Schema(description = "所属养殖场 (Where锁定)")
    private Long farmId;

    /**
     * 池塘编号 (如: 1号高位池)
     */
    @TableField("pond_name")
    @Schema(description = "池塘编号 (如: 1号高位池)")
    private String pondName;

    /**
     * 面积(亩)
     */
    @TableField("area_mu")
    @Schema(description = "面积(亩)")
    private BigDecimal areaMu;
}
