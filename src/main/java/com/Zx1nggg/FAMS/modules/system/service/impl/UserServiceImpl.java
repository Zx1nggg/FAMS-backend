package com.Zx1nggg.FAMS.modules.system.service.impl;

import com.Zx1nggg.FAMS.modules.system.dto.UpdateUserProfileDTO;
import com.Zx1nggg.FAMS.modules.system.entity.User;
import com.Zx1nggg.FAMS.modules.system.mapper.UserMapper;
import com.Zx1nggg.FAMS.modules.system.service.IUserService;
import com.Zx1nggg.FAMS.modules.system.vo.UserProfileVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = getById(userId);
        if (user == null) return null;
        return toVO(user);
    }

    @Override
    public UserProfileVO updateProfile(Long userId, UpdateUserProfileDTO dto) {
        User user = getById(userId);
        if (user == null) return null;
        if (dto.getRealName() != null) user.setRealName(dto.getRealName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getGender() != null) user.setGender(dto.getGender());
        if (dto.getAddress() != null) user.setAddress(dto.getAddress());
        updateById(user);
        return toVO(user);
    }

    @Override
    public void updateAvatar(Long userId, String avatarPath) {
        User user = getById(userId);
        if (user != null) {
            user.setAvatar(avatarPath);
            updateById(user);
        }
    }

    private UserProfileVO toVO(User user) {
        UserProfileVO vo = new UserProfileVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
