package com.Zx1nggg.FAMS.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * <p>
 * 通用字典数据表
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-12
 */
@Getter
@Setter
@ToString
@TableName("sys_dict_data")
@Schema(name = "DictData", description = "通用字典数据表")
public class DictData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 字典类型 (如: sys_disease_type 病害类型)
     */
    @TableField("dict_type")
    @Schema(description = "字典类型 (如: sys_disease_type 病害类型)")
    private String dictType;

    /**
     * 字典标签 (如: 肠炎病, 白斑综合征)
     */
    @TableField("dict_label")
    @Schema(description = "字典标签 (如: 肠炎病, 白斑综合征)")
    private String dictLabel;

    /**
     * 字典键值 (如: enteritis, wss)
     */
    @TableField("dict_value")
    @Schema(description = "字典键值 (如: enteritis, wss)")
    private String dictValue;

    /**
     * 前端ElementPlus的样式属性 (如: danger, warning)
     */
    @TableField("css_class")
    @Schema(description = "前端ElementPlus的样式属性 (如: danger, warning)")
    private String cssClass;

    /**
     * 状态: 1正常, 0停用
     */
    @TableField("status")
    @Schema(description = "状态: 1正常, 0停用")
    private Byte status;
}
