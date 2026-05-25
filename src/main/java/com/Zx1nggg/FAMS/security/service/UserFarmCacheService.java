package com.Zx1nggg.FAMS.security.service;

import com.Zx1nggg.FAMS.modules.base.entity.Farm;
import com.Zx1nggg.FAMS.modules.base.mapper.FarmMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户农场权限缓存服务：
 * 登录时将用户拥有的农场 ID 列表存入 Redis（Set 结构），
 * 请求拦截器从 Redis 校验 X-Current-Farm-Id，避免每次请求都查库。
 */
@Service
public class UserFarmCacheService {

    private static final Logger log = LoggerFactory.getLogger(UserFarmCacheService.class);
    private static final String CACHE_PREFIX = "user:farms:";
    private static final long CACHE_TTL_HOURS = 24;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private FarmMapper farmMapper;

    /**
     * 从 t_farm 表加载用户拥有的农场 ID 列表并写入 Redis
     */
    public void cacheUserFarms(Long userId) {
        String key = CACHE_PREFIX + userId;
        List<Farm> farms = farmMapper.selectList(
                new LambdaQueryWrapper<Farm>().eq(Farm::getUserId, userId));
        stringRedisTemplate.delete(key);
        if (farms != null && !farms.isEmpty()) {
            String[] farmIds = farms.stream()
                    .map(f -> f.getId().toString())
                    .toArray(String[]::new);
            stringRedisTemplate.opsForSet().add(key, farmIds);
            stringRedisTemplate.expire(key, CACHE_TTL_HOURS, TimeUnit.HOURS);
            log.info("已缓存用户 {} 的农场权限: {}", userId, farmIds);
        }
    }

    /**
     * 获取用户的农场 ID 集合（先查 Redis，未命中则回源 DB）
     */
    public Set<Long> getUserFarmIds(Long userId) {
        String key = CACHE_PREFIX + userId;
        Set<String> members = stringRedisTemplate.opsForSet().members(key);
        if (members != null && !members.isEmpty()) {
            return members.stream().map(Long::valueOf).collect(Collectors.toSet());
        }
        // 缓存未命中，从 DB 加载
        cacheUserFarms(userId);
        members = stringRedisTemplate.opsForSet().members(key);
        if (members == null) {
            return Collections.emptySet();
        }
        return members.stream().map(Long::valueOf).collect(Collectors.toSet());
    }

    /**
     * 校验用户是否有权操作指定农场
     * @return true 表示授权通过
     */
    public boolean isAuthorized(Long userId, Long farmId) {
        if (userId == null || farmId == null) {
            return false;
        }
        String key = CACHE_PREFIX + userId;
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, farmId.toString());
        if (Boolean.TRUE.equals(isMember)) {
            return true;
        }
        // 缓存可能过期或不存在，回源 DB 重试一次
        cacheUserFarms(userId);
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(key, farmId.toString()));
    }

    /**
     * 清除用户的农场权限缓存（管理员修改权限后调用）
     */
    public void evictUserFarms(Long userId) {
        stringRedisTemplate.delete(CACHE_PREFIX + userId);
        log.info("已清除用户 {} 的农场权限缓存", userId);
    }
}
