package com.Zx1nggg.FAMS.modules.system.dto;

import lombok.Data;

@Data
public class UpdateUserProfileDTO {
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
