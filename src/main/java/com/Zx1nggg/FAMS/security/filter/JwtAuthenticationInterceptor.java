package com.Zx1nggg.FAMS.security.filter;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 此拦截器已不再负责 Token 解析鉴权（由 JwtAuthenticationFilter 统一处理），
 * 仅作为请求属性校验的二次防线：
 * - 检查 JwtAuthenticationFilter 是否已将用户信息注入到 request 属性中
 * - 如果 JwtAuthenticationFilter 已做了认证，则直接放行
 * - 否则拦截并返回 401 错误
 */
@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行跨域的 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 检查 Filter 是否已将用户信息注入到请求属性中
        Object userId = request.getAttribute("currentUserId");
        if (userId != null) {
            return true; // Filter 已完成了认证，放行
        }

        // 如果 Filter 没有注入用户信息，说明请求未携带有效 Token
        throw new BusinessException(401, "Token已过期或非法，请重新登录");
    }
}
