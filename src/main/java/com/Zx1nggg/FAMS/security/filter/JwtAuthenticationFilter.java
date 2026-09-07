package com.Zx1nggg.FAMS.security.filter;

import com.Zx1nggg.FAMS.security.service.TokenBlacklistService;
import com.Zx1nggg.FAMS.security.service.UserFarmCacheService;
import com.Zx1nggg.FAMS.security.util.JwtUtils;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.system.entity.User;
import com.Zx1nggg.FAMS.modules.system.mapper.UserMapper;
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
import java.util.Objects;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private UserFarmCacheService userFarmCacheService;

    @Autowired
    private UserMapper userMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 旧 Cookie 不应阻止重新登录或提交公开申请。
        return Set.of("/auth/login", "/auth/register", "/auth/check-phone", "/auth/registration-status", "/test/health")
                .contains(requestPath(request));
    }

    private String requestPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

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

        // 签名、必需声明与过期时间仅解析一次；异常令牌不建立认证上下文。
        Claims claims = StringUtils.hasText(token) ? jwtUtils.getClaimsFromToken(token) : null;
        if (claims != null) {
            Long userId;
            String userType;
            Long authVersion;
            try {
                userId = claims.get("userId", Long.class);
                userType = claims.get("userType", String.class);
                authVersion = claims.get("authVersion", Long.class);
                if (authVersion == null) authVersion = 0L; // 兼容首次迁移前的 JWT。
                if (userId == null || userId <= 0 || !StringUtils.hasText(claims.getSubject())
                        || !StringUtils.hasText(claims.getId()) || claims.getExpiration() == null
                        || userType == null || !Set.of("ADMIN", "REGULATOR", "FARMER").contains(userType)) {
                    chain.doFilter(request, response);
                    return;
                }
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
                chain.doFilter(request, response);
                return;
            }
            try {
                User user = userMapper.selectById(userId);
                if (user == null || !Byte.valueOf((byte) 1).equals(user.getStatus())
                        || !Objects.equals(authVersion, user.getAuthVersion() == null ? 0L : user.getAuthVersion())
                        || !Objects.equals(userType, user.getUserType())
                        || !Objects.equals(claims.getSubject(), user.getPhone())
                        || tokenBlacklistService.isBlacklisted(claims.getId())) {
                    sendErrorResponse(response, 200, 401, "登录状态已失效，请重新登录");
                    return;
                }
                Long farmId = null;
                String path = requestPath(request);
                boolean farmIndependent = path.equals("/auth/logout")
                        || path.equals("/base/farm") || path.startsWith("/base/farm/");
                if ("FARMER".equals(userType) && !farmIndependent) {
                    String headerFarmId = request.getHeader("X-Current-Farm-Id");
                    if (StringUtils.hasText(headerFarmId)) {
                        try {
                            farmId = Long.valueOf(headerFarmId);
                            if (farmId <= 0) throw new NumberFormatException();
                        } catch (NumberFormatException e) {
                            sendErrorResponse(response, 200, 400, "X-Current-Farm-Id 格式错误");
                            return;
                        }
                    } else {
                        farmId = user.getFarmId();
                    }
                    if (farmId != null && !userFarmCacheService.isAuthorized(userId, farmId)) {
                        if (StringUtils.hasText(headerFarmId)) {
                            sendErrorResponse(response, 200, 403, "您没有该农场的操作权限，请重新选择养殖场");
                            return;
                        }
                        farmId = null; // 旧默认农场已删除/转让；业务接口仍必须选择有权农场。
                    }
                }
                // 所有检查完成后再提交认证，客户端字段不能覆盖身份与角色。
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userId, null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userType)));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                request.setAttribute("currentUserId", userId);
                request.setAttribute("currentUserType", userType);
                request.setAttribute("currentFarmId", farmId);
            } catch (org.springframework.dao.DataAccessException e) {
                sendErrorResponse(response, 200, 503, "认证服务暂不可用，请稍后重试");
                return;
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
        objectMapper.writeValue(response.getWriter(), Result.error(code, message));
    }
}
