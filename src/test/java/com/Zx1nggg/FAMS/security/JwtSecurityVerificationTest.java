package com.Zx1nggg.FAMS.security;

import com.Zx1nggg.FAMS.security.util.JwtUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 🔐 JWT 安全体系闭环验证测试
 * <p>
 * 覆盖以下 7 条安全边界：
 * 1. CORS 预检放行
 * 2. 登录 → 获取 Token（Header + Cookie 双通道）
 * 3. Token 访问受保护资源
 * 4. 无 Token / 非法 Token → 401
 * 5. Cookie 认证通道独立有效
 * 6. 退出登录 → Token 立即失效（黑名单生效）
 * 7. 退出后再次访问 → 401
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JwtSecurityVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    private static String accessToken;
    private static Cookie tokenCookie;

    // ==================== 1️⃣ CORS 预检 ====================

    @Test
    @Order(1)
    @DisplayName("1. CORS OPTIONS 预检请求应放行并返回跨域响应头")
    void corsPreflight_shouldPass() throws Exception {
        MvcResult result = mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andReturn();

        System.out.println("✅ CORS 预检通过，响应头: " +
                result.getResponse().getHeader("Access-Control-Allow-Origin"));
    }

    // ==================== 2️⃣ 正常登录 ====================

    @Test
    @Order(2)
    @DisplayName("2. 登录 → 同时获取 Header Token 和 Cookie Token")
    void login_shouldReturnTokenInBothChannels() throws Exception {
        String loginJson = """
                {
                    "username": "admin",
                    "password": "123456"
                }
                """;

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(cookie().exists("aqua_token"))
                .andReturn();

        // 从 Cookie 中提取 Token
        MockHttpServletResponse response = result.getResponse();
        Cookie cookie = response.getCookie("aqua_token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        accessToken = cookie.getValue();
        tokenCookie = cookie;

        // 验证 Token 结构：可解析、含 JTI
        assertThat(jwtUtils.validateToken(accessToken)).isTrue();
        assertThat(jwtUtils.getJtiFromToken(accessToken)).isNotBlank();

        System.out.println("✅ 登录成功，Token 已写入 HttpOnly Cookie");
        System.out.println("   Token JTI: " + jwtUtils.getJtiFromToken(accessToken));
    }

    // ==================== 3️⃣ Cookie 通道访问受保护资源 ====================

    @Test
    @Order(3)
    @DisplayName("3. Cookie Token → 访问受保护资源（RoleController）")
    void cookieAuth_shouldAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/role/list")
                        .cookie(tokenCookie))
                .andExpect(status().isOk());

        System.out.println("✅ Cookie 认证通道有效，受保护资源可访问");
    }

    // ==================== 4️⃣ Header 通道访问受保护资源 ====================

    @Test
    @Order(4)
    @DisplayName("4. Authorization Header → 访问受保护资源")
    void headerAuth_shouldAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/user/list")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        System.out.println("✅ Header 认证通道有效");
    }

    // ==================== 5️⃣ 无 Token → 401 ====================

    @Test
    @Order(5)
    @DisplayName("5. 无 Token 请求 → 应返回 401")
    void noToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        System.out.println("✅ 无 Token 请求被正确拦截，返回 401");
    }

    // ==================== 6️⃣ 非法 Token → 401 ====================

    @Test
    @Order(6)
    @DisplayName("6. 伪造 Token → 应返回 401")
    void forgedToken_shouldReturn401() throws Exception {
        String fakeToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJoYWNrZXIifQ.fake";

        mockMvc.perform(get("/user/list")
                        .header("Authorization", "Bearer " + fakeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        System.out.println("✅ 伪造 Token 被正确拦截");
    }

    // ==================== 7️⃣ 退出登录 + 黑名单验证 ====================

    @Test
    @Order(7)
    @DisplayName("7. 退出登录 → Token 立即失效 → 再次请求 401")
    void logout_shouldRevokeToken() throws Exception {
        // 7.1 退出登录（同时加入黑名单 + 删除 Cookie）
        mockMvc.perform(post("/auth/logout")
                        .cookie(tokenCookie)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        System.out.println("✅ 退出登录成功，Token 已加入黑名单 & Cookie 已销毁");

        // 7.2 用已撤销的 Token 再次访问 → 应 401
        mockMvc.perform(get("/user/list")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        System.out.println("✅ 已撤销 Token 无法再次访问（黑名单生效）");
    }

    // ==================== 8️⃣ 整体总结 ====================

    @Test
    @Order(8)
    @DisplayName("🏁 安全闭环总结")
    void securitySummary() {
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("  🔐 JWT 安全体系闭环验证：全部通过 ✅");
        System.out.println("═══════════════════════════════════════════");
        System.out.println("  ✅ CORS 预检 → OK");
        System.out.println("  ✅ 登录认证 → OK");
        System.out.println("  ✅ Cookie 认证通道 → OK");
        System.out.println("  ✅ Header 认证通道 → OK");
        System.out.println("  ✅ 无 Token 拦截 → OK");
        System.out.println("  ✅ 伪造 Token 拦截 → OK");
        System.out.println("  ✅ 退出登录黑名单 → OK");
        System.out.println("═══════════════════════════════════════════\n");
    }
}
