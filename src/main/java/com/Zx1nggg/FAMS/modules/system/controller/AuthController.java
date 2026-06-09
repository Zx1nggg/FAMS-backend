package com.Zx1nggg.FAMS.modules.system.controller;


import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.system.entity.RegistrationApplication;
import com.Zx1nggg.FAMS.modules.system.mapper.RegistrationApplicationMapper;
import com.Zx1nggg.FAMS.security.entity.LoginUser;
import com.Zx1nggg.FAMS.security.service.TokenBlacklistService;
import com.Zx1nggg.FAMS.security.service.UserFarmCacheService;
import com.Zx1nggg.FAMS.security.util.JwtUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private UserFarmCacheService userFarmCacheService;

    @Autowired
    private RegistrationApplicationMapper registrationApplicationMapper;

    /**
     * 从请求中提取 Token 的通用方法（兼容 Cookie 和 Header）
     */
    private String extractToken(HttpServletRequest request) {
        // 1. 从 Authorization Header 中获取
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // 2. 从 Cookie 中获取
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("aqua_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @PostMapping("/login")
    // 必须加上 HttpServletResponse 参数，用来往浏览器写 Cookie
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginForm, HttpServletResponse response) {
        String username = loginForm.get("username");
        String password = loginForm.get("password");

        // 1. 自动进行身份验证 (底层调 UserDetailsService 和 PasswordEncoder)
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (Exception e) {
            // 登录失败时，检查是否有入驻申请记录，提供更友好的提示
            RegistrationApplication app = registrationApplicationMapper.selectOne(
                    new LambdaQueryWrapper<RegistrationApplication>()
                            .eq(RegistrationApplication::getUsername, username)
                            .orderByDesc(RegistrationApplication::getCreatedAt)
                            .last("LIMIT 1"));
            if (app != null) {
                if (app.getStatus() == 0) {
                    return Result.error(400, "您的入驻申请正在审核中，请耐心等待管理员审批");
                } else if (app.getStatus() == 2) {
                    String reason = app.getReviewComment() != null && !app.getReviewComment().isEmpty()
                            ? "，拒绝原因：" + app.getReviewComment() : "";
                    return Result.error(400, "您的入驻申请已被拒绝" + reason + "。请重新提交入驻申请");
                }
            }
            return Result.error(400, "账号或密码错误");
        }

        // 2. 认证通过后，获取封装的 LoginUser
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // 3. 生成 Token (包含 4 个参数，实现了数据隔离)
        String token = jwtUtils.generateToken(
                loginUser.getUser().getId(),
                loginUser.getUsername(),
                loginUser.getUser().getUserType(),
                loginUser.getUser().getFarmId() // 传入农场ID
        );

        // 4. 将用户的农场权限列表缓存到 Redis（FARMER 专属）
        if ("FARMER".equals(loginUser.getUser().getUserType())) {
            userFarmCacheService.cacheUserFarms(loginUser.getUser().getId());
        }

        // 5. 将 Token 写入 HttpOnly Cookie
        Cookie cookie = new Cookie("aqua_token", token);
        cookie.setHttpOnly(true); // 绝对禁止 JavaScript 读取！防 XSS
        cookie.setPath("/");      // 整个系统路径有效
        cookie.setMaxAge(24 * 60 * 60); // 设置 Cookie 过期时间为 24 小时
        response.addCookie(cookie);

        // 6. 返回前端数据
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", loginUser.getUser().getRealName());
        userInfo.put("role", loginUser.getUser().getUserType());
        userInfo.put("avatar", loginUser.getUser().getAvatar());
        data.put("user", userInfo);

        return Result.success(data);
    }

    // 配合前端的退出登录逻辑，安全销毁 Cookie 并将 Token 加入黑名单
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. 获取当前请求的 Token，并将其加入黑名单（实现"退出即失效"）
        String token = extractToken(request);
        if (token != null && jwtUtils.validateToken(token)) {
            String jti = jwtUtils.getJtiFromToken(token);
            if (jti != null) {
                long remainingTtl = jwtUtils.getRemainingTtl(token);
                tokenBlacklistService.blacklist(jti, remainingTtl);
            }
        }

        // 2. 销毁 Cookie
        Cookie cookie = new Cookie("aqua_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 立即销毁
        response.addCookie(cookie);

        return Result.success("退出成功");
    }
}
