package com.Zx1nggg.FAMS.modules.system.service;

import com.Zx1nggg.FAMS.modules.system.dto.UpdateUserProfileDTO;
import com.Zx1nggg.FAMS.modules.system.entity.User;
import com.Zx1nggg.FAMS.modules.system.vo.UserProfileVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IUserService extends IService<User> {

    UserProfileVO getProfile(Long userId);

    UserProfileVO updateProfile(Long userId, UpdateUserProfileDTO dto);

    void updateAvatar(Long userId, String avatarPath);
}
