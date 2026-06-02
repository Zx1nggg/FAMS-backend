package com.Zx1nggg.FAMS.modules.iot.simulator;

import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.mapper.PondMapper;
import com.Zx1nggg.FAMS.modules.log.entity.AlarmRecord;
import com.Zx1nggg.FAMS.modules.log.mapper.AlarmRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * IoT 传感器数据模拟器 —— 每分钟为每个池塘生成仿真实时数据，写入 Redis。
 *
 * 昼夜节律模型：
 *   水温 = 26 + 3×sin(2π×(hour-6)/24) + noise(±0.3°C)     — 午14点最热~29°C，凌晨4点最冷~23°C
 *   溶氧 = 6.5 + 1.5×sin(2π×(hour-10)/24) + noise(±0.3)  — 午16点最高~8.0，凌晨4点最低~5.0
 *   pH  = 7.5 + 0.3×sin(2π×(hour-12)/24) + noise(±0.15)  — 基本平稳
 *
 * 阈值告警：
 *   溶氧 < 3.5 mg/L  → 严重(3级) IOT_DO 告警
 *   水温 > 35°C      → 警告(2级) IOT_TEMP 告警
 *   pH  < 6.5 或 > 9.0 → 警告(2级) 告警
 */
@Slf4j
@Component
public class IotDataSimulator {

    private static final String REDIS_LATEST_PREFIX = "iot:latest:";
    private static final String REDIS_LATEST_FARM_PREFIX = "iot:latest:farm:";
    private static final String REDIS_HISTORY_PREFIX = "iot:history:";
    private static final String REDIS_ALARM_SENTINEL = "iot:alarm:sentinel:";

    // 告警阈值
    private static final double DO_CRITICAL = 3.5;
    private static final double TEMP_CRITICAL = 35.0;
    private static final double PH_LOW = 6.5;
    private static final double PH_HIGH = 9.0;

    @Autowired
    private PondMapper pondMapper;

    @Autowired
    private AlarmRecordMapper alarmRecordMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    /**
     * 每 60 秒执行一次：为每个池塘生成一条仿真传感器数据
     */
    @Scheduled(fixedRate = 60_000)
    public void generate() {
        List<Pond> ponds = pondMapper.selectList(
                new LambdaQueryWrapper<Pond>().eq(Pond::getIsDeleted, 0));

        if (ponds.isEmpty()) {
            log.debug("[IoT-Sim] 无池塘数据，跳过仿真");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int count = 0;

        for (Pond pond : ponds) {
            try {
                Map<String, String> data = generateSensorData(pond, hour, now);
                writeToRedis(pond, data);
                checkAlarms(pond, data);
                count++;
            } catch (Exception e) {
                log.error("[IoT-Sim] 池塘 {} 模拟失败: {}", pond.getId(), e.getMessage());
            }
        }

        log.info("[IoT-Sim] 本轮完成：{} 口池塘传感器数据已更新", count);
    }

    // ==================== 数据生成 ====================

    private Map<String, String> generateSensorData(Pond pond, int hour, LocalDateTime now) {
        // 基于池塘 ID 的小幅度随机偏移种子（让不同池塘数据有差异）
        long seed = pond.getId() * 31 + now.getMinute();
        double pondBias = Math.sin(seed * 0.1) * 0.5;

        // 水温：昼夜正弦 + 随机噪声 + 池塘偏置
        double temp = 26.0
                + 3.0 * Math.sin(2 * Math.PI * (hour - 6) / 24.0)
                + (random.nextDouble() - 0.5) * 0.6
                + pondBias;

        // 溶氧：反向于水温的正弦 + 随机噪声
        double doxygen = 6.5
                + 1.5 * Math.sin(2 * Math.PI * (hour - 10) / 24.0)
                + (random.nextDouble() - 0.5) * 0.6
                - pondBias * 0.3;

        // pH：基本稳定，微小波动
        double ph = 7.5
                + 0.3 * Math.sin(2 * Math.PI * (hour - 12) / 24.0)
                + (random.nextDouble() - 0.5) * 0.3
                + pondBias * 0.05;

        // 边界裁剪
        temp = clamp(temp, 20.0, 38.0);
        doxygen = clamp(doxygen, 2.0, 12.0);
        ph = clamp(ph, 6.0, 9.5);

        Map<String, String> data = new LinkedHashMap<>();
        data.put("pondId", String.valueOf(pond.getId()));
        data.put("pondName", pond.getPondName());
        data.put("deviceSn", "SIM-" + pond.getId());
        data.put("waterTemp", round2(temp));
        data.put("dissolvedOxygen", round2(doxygen));
        data.put("phValue", round2(ph));
        data.put("collectTime", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return data;
    }

    // ==================== Redis 写入 ====================

    private void writeToRedis(Pond pond, Map<String, String> data) {
        String pondId = String.valueOf(pond.getId());

        // 1. 更新最新值 Hash（直接覆盖）
        String latestKey = REDIS_LATEST_PREFIX + pondId;
        redisTemplate.opsForHash().putAll(latestKey, data);
        redisTemplate.expire(latestKey, 120, TimeUnit.SECONDS);

        // 2. 追加到历史 ZSET（score=毫秒时间戳）
        String historyKey = REDIS_HISTORY_PREFIX + pondId;
        String json = toJson(data);
        double score = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(historyKey, json, score);
        // TTL = 25小时，保证始终有24h+数据
        redisTemplate.expire(historyKey, 25, TimeUnit.HOURS);
        // 裁剪 25 小时外的旧数据
        long cutoff = System.currentTimeMillis() - 25 * 3600_000L;
        redisTemplate.opsForZSet().removeRangeByScore(historyKey, 0, cutoff);

        // 3. 失效 farm 聚合缓存（下次查询时重建）
        String farmCacheKey = REDIS_LATEST_FARM_PREFIX + pond.getFarmId();
        redisTemplate.delete(farmCacheKey);
    }

    // ==================== 告警检查 ====================

    private void checkAlarms(Pond pond, Map<String, String> data) {
        double temp = Double.parseDouble(data.get("waterTemp"));
        double doxy = Double.parseDouble(data.get("dissolvedOxygen"));
        double ph = Double.parseDouble(data.get("phValue"));

        if (doxy < DO_CRITICAL) {
            fireAlarm(pond, "IOT_DO", (byte) 3,
                    String.format("%s溶氧量低至 %.1f mg/L，低于安全阈值 %.1f mg/L，请立即开启增氧设备并检查水质！",
                            pond.getPondName(), doxy, DO_CRITICAL));
        }

        if (temp > TEMP_CRITICAL) {
            fireAlarm(pond, "IOT_TEMP", (byte) 2,
                    String.format("%s水温高达 %.1f°C，超过警戒线 %.0f°C，建议加注低温新水或开启遮阳网！",
                            pond.getPondName(), temp, TEMP_CRITICAL));
        }

        if (ph < PH_LOW) {
            fireAlarm(pond, "IOT_PH", (byte) 2,
                    String.format("%spH值降至 %.1f，低于安全下限 %.1f，建议泼洒生石灰调节酸碱度！",
                            pond.getPondName(), ph, PH_LOW));
        } else if (ph > PH_HIGH) {
            fireAlarm(pond, "IOT_PH", (byte) 2,
                    String.format("%spH值升至 %.1f，超过安全上限 %.1f，建议换水或使用有机酸调节！",
                            pond.getPondName(), ph, PH_HIGH));
        }
    }

    /**
     * 触发告警（带 Redis 哨兵去重：同一池塘同类型告警 30 分钟内不重复）
     */
    private void fireAlarm(Pond pond, String alarmType, byte level, String content) {
        String sentinelKey = REDIS_ALARM_SENTINEL + pond.getId() + ":" + alarmType;
        Boolean exists = redisTemplate.hasKey(sentinelKey);
        if (Boolean.TRUE.equals(exists)) {
            return; // 30 分钟内已发过同类告警
        }

        AlarmRecord alarm = new AlarmRecord();
        alarm.setFarmId(pond.getFarmId());
        alarm.setAlarmLevel(level);
        alarm.setAlarmType(alarmType);
        alarm.setAlarmContent(content);
        alarm.setIsHandled((byte) 0);
        alarm.setCreateTime(LocalDateTime.now());
        alarmRecordMapper.insert(alarm);

        // 设置 30 分钟去重窗口
        redisTemplate.opsForValue().set(sentinelKey, "1", 30, TimeUnit.MINUTES);

        log.warn("[IoT-Alarm] {} 级告警 | 类型={} | {} | {}", level, alarmType,
                pond.getPondName(), content.substring(0, Math.min(50, content.length())));
    }

    // ==================== 工具方法 ====================

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private String round2(double val) {
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP).toString();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
