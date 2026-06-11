package com.Zx1nggg.FAMS.security.service;

import com.Zx1nggg.FAMS.modules.system.entity.User;
import com.Zx1nggg.FAMS.modules.system.service.IUserService;
import com.Zx1nggg.FAMS.security.entity.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private IUserService userService;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        // 按手机号查询用户（登录标识符已从 username 切换为 phone）
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));

        if (user == null) {
            throw new UsernameNotFoundException("手机号未注册");
        }

        // 封装成 Spring Security 认识的 LoginUser 返回
        return new LoginUser(user);
    }
}

