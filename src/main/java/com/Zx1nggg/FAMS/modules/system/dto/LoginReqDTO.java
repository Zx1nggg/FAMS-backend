package com.Zx1nggg.FAMS.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数 DTO
 */
@Data
public class LoginReqDTO {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    // 如果前端传了记住我，也可以在这里接收
    private Boolean rememberMe;
}
