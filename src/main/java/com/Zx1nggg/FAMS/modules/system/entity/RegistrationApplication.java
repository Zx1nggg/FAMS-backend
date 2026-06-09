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
import java.time.LocalDateTime;

/**
 * <p>
 * 入驻申请表
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-06-09
 */
@Getter
@Setter
@ToString
@TableName("sys_registration_application")
@Schema(name = "RegistrationApplication", description = "入驻申请表")
public class RegistrationApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 申请登录账号
     */
    @TableField("username")
    @Schema(description = "申请登录账号")
    private String username;

    /**
     * 密码(BCrypt加密)
     */
    @TableField("password")
    @Schema(description = "密码(BCrypt加密)")
    private String password;

    /**
     * 真实姓名/负责人
     */
    @TableField("real_name")
    @Schema(description = "真实姓名/负责人")
    private String realName;

    /**
     * 联系电话
     */
    @TableField("phone")
    @Schema(description = "联系电话")
    private String phone;

    /**
     * 电子邮箱
     */
    @TableField("email")
    @Schema(description = "电子邮箱")
    private String email;

    /**
     * 申请入驻的养殖场名称
     */
    @TableField("farm_name")
    @Schema(description = "申请入驻的养殖场名称")
    private String farmName;

    /**
     * 养殖场所属省份
     */
    @TableField("farm_province")
    @Schema(description = "养殖场所属省份")
    private String farmProvince;

    /**
     * 养殖场所属城市
     */
    @TableField("farm_city")
    @Schema(description = "养殖场所属城市")
    private String farmCity;

    /**
     * 养殖场详细地址
     */
    @TableField("farm_address")
    @Schema(description = "养殖场详细地址")
    private String farmAddress;

    /**
     * 入驻申请理由/补充说明
     */
    @TableField("application_reason")
    @Schema(description = "入驻申请理由/补充说明")
    private String applicationReason;

    /**
     * 审批状态: 0=待审批, 1=已通过, 2=已拒绝
     */
    @TableField("status")
    @Schema(description = "审批状态: 0=待审批, 1=已通过, 2=已拒绝")
    private Integer status;

    /**
     * 审批人ID（管理员）
     */
    @TableField("reviewer_id")
    @Schema(description = "审批人ID（管理员）")
    private Long reviewerId;

    /**
     * 审批意见/拒绝原因
     */
    @TableField("review_comment")
    @Schema(description = "审批意见/拒绝原因")
    private String reviewComment;

    /**
     * 审批时间
     */
    @TableField("reviewed_at")
    @Schema(description = "审批时间")
    private LocalDateTime reviewedAt;

    /**
     * 申请提交时间
     */
    @TableField("created_at")
    @Schema(description = "申请提交时间")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
