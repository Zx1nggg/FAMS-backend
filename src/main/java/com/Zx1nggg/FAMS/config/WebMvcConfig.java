package com.Zx1nggg.FAMS.config;


import com.Zx1nggg.FAMS.security.filter.JwtAuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @org.springframework.beans.factory.annotation.Value("${app.upload.avatar-dir:uploads/avatar}")
    private String avatarDir;

    @Autowired
    private JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/**")           // 拦截所有请求
                .excludePathPatterns(
                        "/auth/login",             // 放行登录接口
                        "/auth/register",          // 放行入驻申请
                        "/auth/check-phone",       // 放行手机号检查
                        "/auth/registration-status", // 放行申请状态查询
                        "/error",                 // 放行错误流转
                        "/uploads/**",            // 放行静态资源（头像等上传文件）
                        "/swagger-ui/**",         // 放行可能存在的接口文档
                        "/swagger-ui.html",
                        "/swagger-ui/index.html",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/test/health" //放行连接测试接口
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /uploads/** 到文件系统中的绝对路径
        registry.addResourceHandler("/uploads/avatar/**")
                .addResourceLocations(java.nio.file.Path.of(avatarDir).toAbsolutePath().normalize().toUri().toString() + "/");
    }

    // CORS 由 Spring Security 统一处理，避免与 SecurityConfig 中的 cors() 冲突
}
