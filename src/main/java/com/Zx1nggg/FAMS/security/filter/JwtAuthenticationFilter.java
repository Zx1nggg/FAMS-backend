package com.Zx1nggg.FAMS.security.filter;

import com.Zx1nggg.FAMS.security.service.TokenBlacklistService;
import com.Zx1nggg.FAMS.security.service.UserFarmCacheService;
import com.Zx1nggg.FAMS.security.util.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private UserFarmCacheService userFarmCacheService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
            String phone = claims.getSubject();
            String userType = claims.get("userType", String.class);
            Long userId = claims.get("userId", Long.class);
            // 从 Claims 中获取 farmId，用于跨域数据隔离校验
            Long farmId = claims.get("farmId", Long.class);

            // 4. 设置认证信息到 Spring Security 上下文
            if (phone != null && SecurityContextHolder.getContext().getAuthentication() == null) {
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

                // 5. 校验 X-Current-Farm-Id（FARMER 专属，从 Redis 校验权限）
                String headerFarmId = request.getHeader("X-Current-Farm-Id");
                if ("FARMER".equals(userType) && StringUtils.hasText(headerFarmId)) {
                    try {
                        Long requestedFarmId = Long.valueOf(headerFarmId);
                        if (!userFarmCacheService.isAuthorized(userId, requestedFarmId)) {
                            log.warn("用户 {} 无权操作农场 {}，拒绝请求", userId, requestedFarmId);
                            sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                                    403, "您没有该农场的操作权限");
                            return;
                        }
                        request.setAttribute("currentFarmId", requestedFarmId);
                    } catch (NumberFormatException e) {
                        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                                400, "X-Current-Farm-Id 格式错误");
                        return;
                    }
                } else {
                    request.setAttribute("currentFarmId", farmId);
                }
            }
        }

        // 6. 放行请求
        chain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, int httpStatus,
                                    int code, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> body = Map.of("code", code, "message", message, "data", null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
