package com.Zx1nggg.FAMS.security.filter;

import com.Zx1nggg.FAMS.security.service.TokenBlacklistService;
import com.Zx1nggg.FAMS.security.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = null;

        // 1. 优先从请求头 Authorization 中获取 Token（兼容多种客户端）
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // 截取 "Bearer " 后面的 Token 值
        }

        // 2. 如果 Header 中没有 Token，则从 Cookie 中获取
        if (!StringUtils.hasText(token)) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("aqua_token".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }

        // 3. 校验 Token
        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
            // 3.1 检查 Token 是否已被加入黑名单（退出登录后立即失效）
            String jti = jwtUtils.getJtiFromToken(token);
            if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
                log.warn("Token 已被撤销（jti={}），拒绝授权", jti);
                chain.doFilter(request, response);
                return;
            }

            // 获取载荷
            Claims claims = jwtUtils.getClaimsFromToken(token);
            String username = claims.getSubject();
            String userType = claims.get("userType", String.class);
            Long userId = claims.get("userId", Long.class);
            // 从 Claims 中获取 farmId，用于跨域数据隔离校验
            Long farmId = claims.get("farmId", Long.class);

            // 4. 设置认证信息到 Spring Security 上下文
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userId, // Principal: 存入 userId，方便后续业务随时获取
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userType))
                        );
                authenticationToken.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                // 将用户信息也设置到 request 属性中，供 Controller 或 Interceptor 使用
                request.setAttribute("currentUserId", userId);
                request.setAttribute("currentUserType", userType);
                request.setAttribute("currentFarmId", farmId);
            }
        }

        // 5. 放行请求（后续由 Spring Security 根据 SecurityContext 决定是否拒绝）
        chain.doFilter(request, response);
    }
}
