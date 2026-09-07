package com.Zx1nggg.FAMS.security;

import com.Zx1nggg.FAMS.modules.system.entity.User;
import com.Zx1nggg.FAMS.modules.system.mapper.UserMapper;
import com.Zx1nggg.FAMS.modules.system.service.IUserService;
import com.Zx1nggg.FAMS.modules.system.vo.UserProfileVO;
import com.Zx1nggg.FAMS.modules.base.entity.Farm;
import com.Zx1nggg.FAMS.modules.base.mapper.FarmMapper;
import com.Zx1nggg.FAMS.modules.regulator.service.IRegulatorService;
import com.Zx1nggg.FAMS.security.service.TokenBlacklistService;
import com.Zx1nggg.FAMS.security.util.JwtUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real JWT and Spring Security chain, isolated persistence/cache boundaries. */
@SpringBootTest(properties = {"app.scheduling.enabled=false", "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"})
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class JwtSecurityVerificationTest {
    private static final String KEY = UUID.randomUUID().toString() + UUID.randomUUID();
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> KEY);
    }
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
    @MockitoBean org.springframework.data.redis.core.StringRedisTemplate farmCache;
    @MockitoBean UserMapper users;
    @MockitoBean FarmMapper farms;
    @MockitoBean IUserService userService;
    @MockitoBean IRegulatorService regulatorService;
    @MockitoBean TokenBlacklistService blacklist;
    @MockitoBean com.Zx1nggg.FAMS.modules.system.service.IRegistrationApplicationService registrations;
    private User user;
    private final Set<String> revoked = new HashSet<>();

    @BeforeEach void setUp() {
        user = new User();
        user.setId(1L); user.setPhone("test-subject"); user.setUserType("FARMER");
        user.setStatus((byte) 1); user.setFarmId(10L);
        when(users.selectById(1L)).thenReturn(user);
        Farm farm = new Farm(); farm.setId(10L); farm.setUserId(1L);
        when(farms.selectById(10L)).thenReturn(farm);
        UserProfileVO profile = new UserProfileVO(); profile.setId(1L);
        when(userService.getProfile(1L)).thenReturn(profile);
        revoked.clear();
        when(blacklist.isBlacklisted(anyString())).thenAnswer(i -> revoked.contains(i.getArgument(0)));
        doAnswer(i -> { revoked.add(i.getArgument(0)); return null; }).when(blacklist).blacklist(anyString(), anyLong());
    }
    private String token() { return jwt.generateToken(1L, "test-subject", "FARMER", 10L); }
    private void expectProfile(String token, int code) throws Exception {
        mvc.perform(get("/user/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(code));
    }
    @Test void unauthenticatedReturnsJson401() throws Exception {
        mvc.perform(get("/user/profile")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }
    @Test void malformedTokenIsRejected() throws Exception { expectProfile("invalid", 401); }
    @Test void legacyPhoneOnlyStatusQueryCannotReturnPersonalData() throws Exception {
        mvc.perform(get("/auth/registration-status").param("phone", "13900007777"))
                .andExpect(jsonPath("$.code").value(405)).andExpect(jsonPath("$.data.realName").doesNotExist());
        verifyNoInteractions(registrations);
    }
    @Test void statusQueryRequiresPasswordInBody() throws Exception {
        mvc.perform(post("/auth/registration-status").contentType("application/json").content("{\"phone\":\"13900007777\"}"))
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(registrations);
    }
    @Test void verifiedStatusRequestWorksWithoutLoginEvenWithStaleCookie() throws Exception {
        String password = UUID.randomUUID().toString();
        var result = new com.Zx1nggg.FAMS.modules.system.vo.RegistrationApplicationVO(); result.setStatus(0);
        when(registrations.queryStatusByPhone("13900007777", password)).thenReturn(result);
        String body = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("phone", "13900007777", "password", password));
        mvc.perform(post("/auth/registration-status").cookie(new Cookie("aqua_token", "invalid"))
                .contentType("application/json").content(body)).andExpect(jsonPath("$.code").value(200));
        verify(registrations).queryStatusByPhone("13900007777", password);
    }
    @Test void wrongSignatureIsRejected() throws Exception {
        String value = Jwts.builder().subject("test-subject").expiration(new Date(System.currentTimeMillis()+60000))
                .signWith(Jwts.SIG.HS256.key().build()).compact();
        expectProfile(value, 401);
    }
    @Test void expiredTokenIsRejected() throws Exception {
        String value = Jwts.builder().subject("test-subject").expiration(new Date(1))
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8))).compact();
        expectProfile(value, 401);
    }
    @Test void missingExpirationIsRejected() throws Exception {
        String value = Jwts.builder().subject("test-subject").id("test-id").claim("userId", 1L).claim("userType", "FARMER")
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8))).compact();
        expectProfile(value, 401);
    }
    @Test void malformedIdentityClaimIsRejected() throws Exception {
        String value = Jwts.builder().subject("test-subject").id("test-id").claim("userId", "invalid").claim("userType", "FARMER")
                .expiration(new Date(System.currentTimeMillis()+60000))
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8))).compact();
        expectProfile(value, 401);
    }
    @Test void validHeaderIsAuthorized() throws Exception {
        expectProfile(token(), 200);
        verify(userService).getProfile(1L);
    }
    @Test void validCookieIsAuthorized() throws Exception {
        mvc.perform(get("/user/profile").cookie(new Cookie("aqua_token", token())))
                .andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.id").value(1));
    }
    @Test void disabledUserIsRejected() throws Exception { user.setStatus((byte) 0); expectProfile(token(), 401); }
    @Test void deletedUserIsRejected() throws Exception { when(users.selectById(1L)).thenReturn(null); expectProfile(token(), 401); }
    @Test void changedRoleInvalidatesOldToken() throws Exception { user.setUserType("REGULATOR"); expectProfile(token(), 401); }
    @Test void changedPasswordVersionInvalidatesOldToken() throws Exception { user.setAuthVersion(1L); expectProfile(token(), 401); }
    @Test void revokedTokenIsRejected() throws Exception {
        String value = token(); revoked.add(jwt.getJtiFromToken(value)); expectProfile(value, 401);
    }
    @Test void crossFarmHeaderIsRejected() throws Exception {
        Farm other = new Farm(); other.setId(20L); other.setUserId(2L); when(farms.selectById(20L)).thenReturn(other);
        mvc.perform(get("/user/profile").header("Authorization", "Bearer " + token()).header("X-Current-Farm-Id", "20"))
                .andExpect(jsonPath("$.code").value(403));
        verifyNoInteractions(userService);
    }
    @Test void deletedDefaultFarmStillAllowsProfile() throws Exception { when(farms.selectById(10L)).thenReturn(null); expectProfile(token(), 200); }
    @Test void invalidFarmHeaderReturns400() throws Exception {
        mvc.perform(get("/user/profile").header("Authorization", "Bearer " + token()).header("X-Current-Farm-Id", "bad"))
                .andExpect(jsonPath("$.code").value(400));
    }
    @Test void farmerCannotReadRegulatorData() throws Exception {
        mvc.perform(get("/regulator/dashboard/stats").header("Authorization", "Bearer " + token()))
                .andExpect(jsonPath("$.code").value(403));
        verifyNoInteractions(regulatorService);
    }
    @Test void farmerCannotManageUsers() throws Exception {
        mvc.perform(get("/user/list").header("Authorization", "Bearer " + token()))
                .andExpect(jsonPath("$.code").value(403));
        verifyNoInteractions(userService);
    }
    @Test void regulatorCanReadRegulatorData() throws Exception {
        user.setUserType("REGULATOR");
        mvc.perform(get("/regulator/dashboard/stats").header("Authorization", "Bearer " + jwt.generateToken(1L, "test-subject", "REGULATOR", null)))
                .andExpect(jsonPath("$.code").value(200));
        verify(regulatorService).getDashboardStats();
    }
    @Test void requestUserIdCannotReplaceIdentity() throws Exception {
        mvc.perform(get("/user/profile").param("userId", "2").header("Authorization", "Bearer " + token()))
                .andExpect(jsonPath("$.data.id").value(1));
        verify(userService).getProfile(1L);
    }
    @Test void logoutRevokesTokenAndClearsCookie() throws Exception {
        String value = token();
        mvc.perform(post("/auth/logout").cookie(new Cookie("aqua_token", value)))
                .andExpect(jsonPath("$.code").value(200)).andExpect(cookie().maxAge("aqua_token", 0));
        expectProfile(value, 401);
    }
    @Test void trustedCorsPreflightPasses() throws Exception {
        mvc.perform(options("/auth/login").header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
    @Test void untrustedCorsOriginIsRejected() throws Exception {
        mvc.perform(options("/auth/login").header("Origin", "https://untrusted.invalid").header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
    @Test void loginVerifiesPasswordAndSetsHttpOnlyCookie() throws Exception {
        String credential = UUID.randomUUID().toString();
        user.setPassword(encoder.encode(credential)); user.setUsername("test user");
        when(userService.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(user);
        String body = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("phone", "test-subject", "password", credential));
        var response = mvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(jsonPath("$.code").value(200)).andExpect(cookie().httpOnly("aqua_token", true))
                .andExpect(jsonPath("$.data.user.role").value("FARMER")).andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andReturn().getResponse();
        org.assertj.core.api.Assertions.assertThat(jwt.validateToken(response.getCookie("aqua_token").getValue())).isTrue();
    }
    @Test void missingLoginFieldsReturn400() throws Exception {
        mvc.perform(post("/auth/login").contentType("application/json").content("{}"))
                .andExpect(jsonPath("$.code").value(400));
    }
    @Test void staleFarmHeaderCannotBlockLogout() throws Exception {
        mvc.perform(post("/auth/logout").cookie(new Cookie("aqua_token", token())).header("X-Current-Farm-Id", "20"))
                .andExpect(jsonPath("$.code").value(200)).andExpect(cookie().maxAge("aqua_token", 0));
    }
}
