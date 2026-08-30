package com.Zx1nggg.FAMS.modules.system.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.system.dto.UpdateUserProfileDTO;
import com.Zx1nggg.FAMS.modules.system.entity.User;
import com.Zx1nggg.FAMS.modules.system.mapper.UserMapper;
import com.Zx1nggg.FAMS.modules.system.service.IUserService;
import com.Zx1nggg.FAMS.modules.system.vo.UserProfileVO;
import com.Zx1nggg.FAMS.security.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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
        if (dto.getUsername()!= null) user.setUsername(dto.getUsername());
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

    @Override
    public Page<UserProfileVO> pageUsers(Integer pageNum, Integer pageSize, String keyword, String userType, Byte status) {
        assertAdmin();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or()
                    .like(User::getRealName, keyword)
                    .or()
                    .like(User::getPhone, keyword));
        }
        if (userType != null && !userType.isBlank()) {
            wrapper.eq(User::getUserType, userType);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getId);

        int current = pageNum == null || pageNum <= 0 ? 1 : pageNum;
        int size = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        Page<User> page = page(new Page<>(current, size), wrapper);
        Page<UserProfileVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public void updateUserStatus(Long id, Byte status) {
        assertAdmin();
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "账号状态只能为启用或停用");
        }
        if (Objects.equals(id, SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(400, "不能停用自己的账号");
        }
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus(status);
        updateById(user);
    }

    @Override
    public void deleteUsers(List<Long> ids) {
        assertAdmin();
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(400, "请选择要删除的用户");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (ids.stream().anyMatch(id -> Objects.equals(id, currentUserId))) {
            throw new BusinessException(400, "不能删除自己的账号");
        }
        removeByIds(ids);
    }

    private void assertAdmin() {
        if (!SecurityUtils.isAdmin()) {
            throw new BusinessException(403, "仅管理员可进行账号运维");
        }
    }

    private UserProfileVO toVO(User user) {
        UserProfileVO vo = new UserProfileVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
