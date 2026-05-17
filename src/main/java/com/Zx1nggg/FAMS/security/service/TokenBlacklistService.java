package com.Zx1nggg.FAMS.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务：
 * 用户退出登录或管理员强制下线时，将 JWT 的 jti（JWT ID）加入 Redis 黑名单，
 * 使得该 Token 即使未过期也立即失效，实现"退出登录即失效"的效果。
 */
@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 将 Token 加入黑名单
     * @param tokenJti JWT 的 jti（唯一标识）
     * @param ttlMillis Token 的剩余有效时间（毫秒），黑名单过期时间与之相同
     */
    public void blacklist(String tokenJti, long ttlMillis) {
        String key = BLACKLIST_PREFIX + tokenJti;
        redisTemplate.opsForValue().set(key, "1", ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 检查 Token 是否在黑名单中
     * @param tokenJti JWT 的 jti
     * @return true 表示已被拉黑
     */
    public boolean isBlacklisted(String tokenJti) {
        return redisTemplate.hasKey(BLACKLIST_PREFIX + tokenJti);
    }
}
