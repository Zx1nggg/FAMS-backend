package com.Zx1nggg.FAMS.modules.ai.service;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.ai.config.AiProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class AiRateLimitService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final StringRedisTemplate redisTemplate;
    private final AiProperties properties;

    public AiRateLimitService(StringRedisTemplate redisTemplate, AiProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void check(Long userId) {
        try {
            long minuteBucket = System.currentTimeMillis() / 60_000L;
            incrementAndCheck("ai:rate:minute:" + userId + ":" + minuteBucket,
                    Math.max(1, properties.getPerMinuteRequestLimit()), Duration.ofMinutes(2),
                    "操作过于频繁，请稍后再试");

            String day = LocalDate.now(BUSINESS_ZONE).toString();
            incrementAndCheck("ai:rate:day:" + userId + ":" + day,
                    Math.max(1, properties.getDailyRequestLimit()), Duration.ofDays(2),
                    "今日AI使用次数已达上限");
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            throw new BusinessException(503, "AI限流服务暂不可用，请稍后重试");
        }
    }

    private void incrementAndCheck(String key, int limit, Duration ttl, String message) {
        Long value = redisTemplate.opsForValue().increment(key);
        if (value != null && value == 1L) redisTemplate.expire(key, ttl);
        if (value != null && value > limit) throw new BusinessException(429, message);
    }
}
