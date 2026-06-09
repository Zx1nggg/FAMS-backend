package com.Zx1nggg.FAMS.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 入驻申请请求参数 DTO
 */
@Data
public class RegistrationReqDTO {

    @NotBlank(message = "登录账号不能为空")
    @Size(min = 3, max = 50, message = "账号长度需在3-50个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度需在6-100个字符之间")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    private String realName;

    private String phone;

    private String email;

    @NotBlank(message = "养殖场名称不能为空")
    @Size(max = 100, message = "养殖场名称长度不能超过100个字符")
    private String farmName;

    private String farmProvince;

    private String farmCity;

    private String farmAddress;

    private String applicationReason;
}
