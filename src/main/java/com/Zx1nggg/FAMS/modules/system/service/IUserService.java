package com.Zx1nggg.FAMS.modules.system.service;

import com.Zx1nggg.FAMS.modules.system.dto.UpdateUserProfileDTO;
import com.Zx1nggg.FAMS.modules.system.entity.User;
import com.Zx1nggg.FAMS.modules.system.vo.UserProfileVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IUserService extends IService<User> {

    UserProfileVO getProfile(Long userId);

    UserProfileVO updateProfile(Long userId, UpdateUserProfileDTO dto);

    void updateAvatar(Long userId, String avatarPath);

    Page<UserProfileVO> pageUsers(Integer pageNum, Integer pageSize, String keyword, String userType, Byte status);

    void updateUserStatus(Long id, Byte status);

    void deleteUsers(List<Long> ids);
}
