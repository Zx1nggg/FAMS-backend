package com.Zx1nggg.FAMS.security.filter;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.security.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${jwt.token-header:Authorization}")
    private String tokenHeader;

    @Value("${jwt.token-prefix:Bearer }")
    private String tokenPrefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行跨域的 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader(tokenHeader);

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(tokenPrefix)) {
            String token = authHeader.substring(tokenPrefix.length());

            if (jwtUtils.validateToken(token)) {
                Claims claims = jwtUtils.getClaimsFromToken(token);
                // 将解析出的用户信息挂载到 Request 上，后续 Controller 可直接取用
                request.setAttribute("currentUserId", claims.get("userId"));
                request.setAttribute("currentUserType", claims.get("userType"));
                return true;
            }
        }

        // 🌟 核心修改：直接抛出自定义业务异常，彻底抛弃 response.getWriter()
        // 这个异常会精准飞向你的 GlobalExceptionHandler，并被包装成标准 Result 格式返回给前端！
        throw new BusinessException(401, "Token已过期或非法，请重新登录");
    }
}