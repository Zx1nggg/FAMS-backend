package com.Zx1nggg.FAMS.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 入驻申请请求参数 DTO
 */
@Data
public class RegistrationReqDTO {

    @NotBlank(message = "手机号不能为空")
    @Size(min = 11, max = 11, message = "请输入正确的11位手机号码")
    @jakarta.validation.constraints.Pattern(regexp = "1[3-9]\\d{9}")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 72, message = "密码长度需在6-72个字符之间")
    @lombok.ToString.Exclude
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 20, message = "昵称长度需在2-20个字符之间")
    private String username;

    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 50, message = "真实姓名长度需在2-50个字符之间")
    private String realName;

    @jakarta.validation.constraints.Email
    @Size(max = 100)
    private String email;

    @NotBlank(message = "养殖场名称不能为空")
    @Size(max = 100, message = "养殖场名称长度不能超过100个字符")
    private String farmName;

    @Size(max = 50)
    private String farmProvince;

    @Size(max = 50)
    private String farmCity;

    @Size(max = 200)
    private String farmAddress;

    @Size(max = 1000)
    private String applicationReason;
}
