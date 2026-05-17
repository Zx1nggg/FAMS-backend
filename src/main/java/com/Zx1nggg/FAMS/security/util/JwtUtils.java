package com.Zx1nggg.FAMS.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @PostConstruct
    public void init() {
        // 校验密钥强度（HMAC-SHA 算法要求密钥至少 256 bits = 32 字节）
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.warn("⚠️ JWT secret 长度不足 32 字节（当前 {} 字节），存在被暴力破解风险！生产环境请使用 256 位以上随机密钥。", keyBytes.length);
        }
    }

    // 获取签名 Key（明确为 SecretKey）
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token（含 JTI，支持黑名单撤销）
     */
    public String generateToken(Long userId, String username, String userType, Long farmId) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // 生成唯一 JWT ID，用于 Token 黑名单撤销
                .claim("userId", userId)
                .claim("username", username)
                .claim("userType", userType)
                .claim("farmId", farmId) // 塞入农场ID
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从 Token 中提取 JTI（用于黑名单校验）
     */
    public String getJtiFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            return claims.getId();
        }
        return null;
    }

    /**
     * 获取 Token 剩余有效时间（毫秒），用于黑名单过期时间
     */
    public long getRemainingTtl(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null && claims.getExpiration() != null) {
            long diff = claims.getExpiration().getTime() - System.currentTimeMillis();
            return Math.max(diff, 0);
        }
        return 0;
    }

    /**
     * 解析 Token 获取 Claims
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null; // 签名错误、过期、格式错误等均返回 null
        }
    }

    /**
     * 校验 Token 是否有效（不含黑名单检查，黑名单检查在 Filter 中执行）
     */
    public boolean validateToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null && !claims.getExpiration().before(new Date());
    }
}
