package com.Zx1nggg.FAMS.security.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 从 JwtAuthenticationFilter 注入的 request 属性中提取当前用户信息。
 * 仅在 Web 请求线程中可用。
 */
public final class SecurityUtils {

    private static final String KEY_USER_ID = "currentUserId";
    private static final String KEY_FARM_ID = "currentFarmId";
    private static final String KEY_USER_TYPE = "currentUserType";

    public static final String ROLE_FARMER = "FARMER";
    public static final String ROLE_REGULATOR = "REGULATOR";
    public static final String ROLE_ADMIN = "ADMIN";

    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        return (Long) attrs.getRequest().getAttribute(KEY_USER_ID);
    }

    public static Long getCurrentFarmId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        return (Long) attrs.getRequest().getAttribute(KEY_FARM_ID);
    }

    public static String getCurrentUserType() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        return (String) attrs.getRequest().getAttribute(KEY_USER_TYPE);
    }

    public static boolean isFarmer() {
        return ROLE_FARMER.equals(getCurrentUserType());
    }

    public static boolean isRegulator() {
        return ROLE_REGULATOR.equals(getCurrentUserType());
    }

    public static boolean isAdmin() {
        return ROLE_ADMIN.equals(getCurrentUserType());
    }
}
