package com.Zx1nggg.FAMS.modules.system.dto;

import lombok.Data;

@Data
public class UpdateUserProfileDTO {
    private String realName;
    private String phone;
    private String email;
    private Integer gender;
    private String address;
    private String username;
}
