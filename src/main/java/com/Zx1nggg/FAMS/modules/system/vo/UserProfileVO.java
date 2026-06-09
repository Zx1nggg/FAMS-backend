package com.Zx1nggg.FAMS.modules.system.vo;

import lombok.Data;

@Data
public class UserProfileVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Integer gender;
    private String address;
    private String avatar;
    private String userType;
    private Byte status;
}
