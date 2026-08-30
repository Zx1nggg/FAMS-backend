package com.Zx1nggg.FAMS.modules.iot.service.impl;

import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.mapper.PondMapper;
import com.Zx1nggg.FAMS.modules.iot.entity.IotSensorData;
import com.Zx1nggg.FAMS.modules.iot.mapper.IotSensorDataMapper;
import com.Zx1nggg.FAMS.modules.iot.service.IIotSensorDataService;
import com.Zx1nggg.FAMS.modules.iot.vo.IotSensorDataVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class IotSensorDataServiceImpl
        extends ServiceImpl<IotSensorDataMapper, IotSensorData>
        implements IIotSensorDataService {

    private static final String REDIS_LATEST_PREFIX = "iot:latest:";
    private static final String REDIS_LATEST_FARM_PREFIX = "iot:latest:farm:";
    private static final String REDIS_HISTORY_PREFIX = "iot:history:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PondMapper pondMapper;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // ==================== 公开方法 ====================

    @Override
    public IotSensorDataVO getLatestByPondId(Long pondId) {
        Pond pond = getActivePond(pondId);
        if (pond == null) {
            cleanupPondCache(pondId, null);
            return null;
        }
        // 1. 先查 Redis Hash
        Map<Object, Object> hash = redisTemplate.opsForHash().entries(REDIS_LATEST_PREFIX + pondId);
        if (hash != null && !hash.isEmpty()) {
            return hashToVO(pondId, hash);
        }
        // 2. Redis miss → 回 MySQL
        IotSensorData data = getOne(new LambdaQueryWrapper<IotSensorData>()
                .eq(IotSensorData::getPondId, pondId)
                .orderByDesc(IotSensorData::getCollectTime)
                .last("LIMIT 1"));
        if (data == null) return null;
        return entityToVO(data);
    }

    @Override
    public List<IotSensorDataVO> getLatestByFarmId(Long farmId) {
        // 1. 先查 Redis 聚合缓存
        String farmKey = REDIS_LATEST_FARM_PREFIX + farmId;
        Map<Object, Object> farmCache = redisTemplate.opsForHash().entries(farmKey);
        if (farmCache != null && !farmCache.isEmpty()) {
            return farmCache.values().stream()
                    .map(v -> parseJson((String) v, IotSensorDataVO.class))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // 2. Redis miss → 批量查各塘最新（最多一次 MySQL）
        List<Pond> ponds = pondMapper.selectList(
                new LambdaQueryWrapper<Pond>()
                        .eq(Pond::getFarmId, farmId)
                        .eq(Pond::getIsDeleted, 0));
        List<IotSensorDataVO> result = new ArrayList<>();

        for (Pond pond : ponds) {
            IotSensorDataVO vo = getLatestByPondId(pond.getId());
            if (vo != null) result.add(vo);
        }

        // 回写 Redis 聚合缓存（120s 过期）
        if (!result.isEmpty()) {
            Map<String, String> map = new HashMap<>();
            for (IotSensorDataVO vo : result) {
                map.put(String.valueOf(vo.getPondId()), toJson(vo));
            }
            redisTemplate.opsForHash().putAll(farmKey, map);
            redisTemplate.expire(farmKey, 120, TimeUnit.SECONDS);
        }
        return result;
    }

    @Override
    public List<IotSensorDataVO> getHistory(Long pondId, int hours) {
        Pond pond = getActivePond(pondId);
        if (pond == null) {
            cleanupPondCache(pondId, null);
            return List.of();
        }
        if (hours <= 24) {
            // Redis ZSET 分钟级精度
            return getHistoryFromRedis(pondId, hours);
        }
        // MySQL 小时均值
        return getHistoryFromMySQL(pondId, hours);
    }

    // ==================== Redis 历史查询 ====================

    private List<IotSensorDataVO> getHistoryFromRedis(Long pondId, int hours) {
        String key = REDIS_HISTORY_PREFIX + pondId;
        long from = System.currentTimeMillis() - hours * 3600_000L;
        long to = System.currentTimeMillis();

        Set<String> members = redisTemplate.opsForZSet().rangeByScore(key, from, to);
        if (members == null || members.isEmpty()) {
            // Redis 无数据 → 回 MySQL
            return getHistoryFromMySQL(pondId, hours);
        }

        List<IotSensorDataVO> list = new ArrayList<>();
        for (String member : members) {
            IotSensorDataVO vo = parseJson(member, IotSensorDataVO.class);
            if (vo != null) list.add(vo);
        }
        return list;
    }

    private Pond getActivePond(Long pondId) {
        if (pondId == null) return null;
        return pondMapper.selectOne(new LambdaQueryWrapper<Pond>()
                .eq(Pond::getId, pondId)
                .eq(Pond::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    private void cleanupPondCache(Long pondId, Long farmId) {
        if (pondId != null) {
            redisTemplate.delete(REDIS_LATEST_PREFIX + pondId);
            redisTemplate.delete(REDIS_HISTORY_PREFIX + pondId);
        }
        if (farmId != null) {
            redisTemplate.delete(REDIS_LATEST_FARM_PREFIX + farmId);
        }
    }

    // ==================== MySQL 历史查询 ====================

    private List<IotSensorDataVO> getHistoryFromMySQL(Long pondId, int hours) {
        LocalDateTime from = LocalDateTime.now().minusHours(hours);
        List<IotSensorData> dataList = list(new LambdaQueryWrapper<IotSensorData>()
                .eq(IotSensorData::getPondId, pondId)
                .ge(IotSensorData::getCollectTime, from)
                .orderByAsc(IotSensorData::getCollectTime));
        return dataList.stream().map(this::entityToVO).collect(Collectors.toList());
    }

    // ==================== VO 转换 ====================

    private IotSensorDataVO entityToVO(IotSensorData data) {
        IotSensorDataVO vo = new IotSensorDataVO();
        vo.setId(data.getId());
        vo.setPondId(data.getPondId());
        vo.setDeviceSn(data.getDeviceSn());
        vo.setWaterTemp(data.getWaterTemp());
        vo.setDissolvedOxygen(data.getDissolvedOxygen());
        vo.setPhValue(data.getPhValue());
        vo.setCollectTime(data.getCollectTime());

        if (data.getPondId() != null) {
            Pond pond = pondMapper.selectById(data.getPondId());
            if (pond != null) vo.setPondName(pond.getPondName());
        }
        return vo;
    }

    private IotSensorDataVO hashToVO(Long pondId, Map<Object, Object> hash) {
        IotSensorDataVO vo = new IotSensorDataVO();
        vo.setPondId(pondId);
        vo.setPondName(getStr(hash, "pondName"));
        vo.setDeviceSn(getStr(hash, "deviceSn"));
        vo.setWaterTemp(getDecimal(hash, "waterTemp"));
        vo.setDissolvedOxygen(getDecimal(hash, "dissolvedOxygen"));
        vo.setPhValue(getDecimal(hash, "phValue"));

        String timeStr = getStr(hash, "collectTime");
        if (timeStr != null) {
            vo.setCollectTime(LocalDateTime.parse(timeStr));
        }
        return vo;
    }

    // ==================== JSON / Redis 工具方法 ====================

    private String getStr(Map<Object, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private BigDecimal getDecimal(Map<Object, Object> map, String key) {
        String v = getStr(map, key);
        return v != null ? new BigDecimal(v) : null;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private <T> T parseJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }
}
