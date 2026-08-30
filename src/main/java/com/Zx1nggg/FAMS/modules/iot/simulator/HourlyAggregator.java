package com.Zx1nggg.FAMS.modules.iot.simulator;

import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.mapper.PondMapper;
import com.Zx1nggg.FAMS.modules.iot.entity.IotSensorData;
import com.Zx1nggg.FAMS.modules.iot.mapper.IotSensorDataMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 每小时整点执行：读取 Redis 中过去1小时的分钟级数据，
 * 计算每小时均值后持久化写入 MySQL t_iot_sensor_data 表。
 *
 * 设计意图：
 *   Redis 保留 24 小时分钟级热数据（供实时曲线图）
 *   MySQL 保留每小时均值（供长期趋势分析，极大压缩存储）
 */
@Slf4j
@Component
public class HourlyAggregator {

    private static final String REDIS_HISTORY_PREFIX = "iot:history:";

    @Autowired
    private PondMapper pondMapper;

    @Autowired
    private IotSensorDataMapper iotSensorDataMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 每小时整点过 1 分钟后执行（给 Simulator 多一点余量）
     * 例如：14:01:00 聚合 13:00-14:00 的数据
     */
    @Scheduled(cron = "0 1 * * * ?")
    public void aggregate() {
        List<Pond> ponds = pondMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Pond>()
                        .eq(Pond::getIsDeleted, 0));
        if (ponds.isEmpty()) return;

        int count = 0;
        LocalDateTime hourStart = LocalDateTime.now()
                .withMinute(0).withSecond(0).withNano(0);
        long fromMs = hourStart.minusHours(1)
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long toMs = hourStart
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        for (Pond pond : ponds) {
            try {
                IotSensorData avg = aggregatePond(pond, fromMs, toMs, hourStart);
                if (avg != null) {
                    iotSensorDataMapper.insert(avg);
                    count++;
                }
            } catch (Exception e) {
                log.error("[HourlyAgg] 池塘 {} 聚合失败: {}", pond.getId(), e.getMessage());
            }
        }

        log.info("[HourlyAgg] 完成：{} 口池塘小时均值已写入 MySQL（时段 {} ~ {}）",
                count,
                new java.util.Date(fromMs),
                new java.util.Date(toMs));
    }

    private IotSensorData aggregatePond(Pond pond, long fromMs, long toMs,
                                        LocalDateTime collectTime) {
        String key = REDIS_HISTORY_PREFIX + pond.getId();
        Set<String> members = redisTemplate.opsForZSet().rangeByScore(key, fromMs, toMs);

        if (members == null || members.isEmpty()) return null;

        double sumTemp = 0, sumDo = 0, sumPh = 0;
        int n = 0;

        for (String json : members) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = objectMapper.readValue(json, Map.class);
                sumTemp += Double.parseDouble(String.valueOf(map.get("waterTemp")));
                sumDo += Double.parseDouble(String.valueOf(map.get("dissolvedOxygen")));
                sumPh += Double.parseDouble(String.valueOf(map.get("phValue")));
                n++;
            } catch (Exception ignored) {
                // 跳过格式异常的数据
            }
        }

        if (n == 0) return null;

        IotSensorData entity = new IotSensorData();
        entity.setPondId(pond.getId());
        entity.setDeviceSn("SIM-" + pond.getId() + "-H");
        entity.setWaterTemp(round(avg(sumTemp, n)));
        entity.setDissolvedOxygen(round(avg(sumDo, n)));
        entity.setPhValue(round(avg(sumPh, n)));
        entity.setCollectTime(collectTime);

        return entity;
    }

    private BigDecimal round(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
    }

    private double avg(double sum, int n) {
        return sum / n;
    }
}
