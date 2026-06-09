package com.Zx1nggg.FAMS.modules.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 入驻申请列表展示 VO
 */
@Data
public class RegistrationApplicationVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private String email;

    private String farmName;

    private String farmProvince;

    private String farmCity;

    private String farmAddress;

    private String applicationReason;

    /**
     * 审批状态: 0=待审批, 1=已通过, 2=已拒绝
     */
    private Integer status;

    /**
     * 审批人姓名
     */
    private String reviewerName;

    /**
     * 审批意见/拒绝原因
     */
    private String reviewComment;

    /**
     * 审批时间
     */
    private LocalDateTime reviewedAt;

    /**
     * 申请提交时间
     */
    private LocalDateTime createdAt;
}
