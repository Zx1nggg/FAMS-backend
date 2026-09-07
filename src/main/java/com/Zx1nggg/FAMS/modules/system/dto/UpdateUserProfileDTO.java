package com.Zx1nggg.FAMS.modules.system.dto;

import lombok.Data;

@Data
public class UpdateUserProfileDTO {
    @jakarta.validation.constraints.Size(min = 2, max = 50, message = "真实姓名长度需在2-50个字符之间")
    @jakarta.validation.constraints.Pattern(regexp = ".*\\S.*", message = "真实姓名不能为空")
    private String realName;
    @jakarta.validation.constraints.Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    @jakarta.validation.constraints.Email
    private String email;
    @jakarta.validation.constraints.Min(0)
    @jakarta.validation.constraints.Max(2)
    private Integer gender;
    private String address;
    private String username;
}
