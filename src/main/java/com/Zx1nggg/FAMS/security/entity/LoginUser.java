package com.Zx1nggg.FAMS.security.entity;

import com.Zx1nggg.FAMS.modules.system.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Spring Security 专属的用户封装类
 */
public class LoginUser implements UserDetails {

    private User user;

    public LoginUser(User user) {
        this.user = Objects.requireNonNull(user, "用户不能为空");
    }

    public User getUser() {
        return user;
    }

    // 获取用户权限列表
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 将 userType 封装为 Spring Security 的权限标识，例如: ROLE_FARMER
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getUserType()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() == 1; // 1正常，0禁用
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == 1;
    }
}