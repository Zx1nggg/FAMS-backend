package com.Zx1nggg.FAMS.modules.iot.simulator;

import com.Zx1nggg.FAMS.modules.base.entity.Pond;
import com.Zx1nggg.FAMS.modules.base.mapper.PondMapper;
import com.Zx1nggg.FAMS.modules.log.entity.AlarmActionLog;
import com.Zx1nggg.FAMS.modules.log.entity.AlarmRecord;
import com.Zx1nggg.FAMS.modules.log.entity.AlarmRule;
import com.Zx1nggg.FAMS.modules.log.mapper.AlarmActionLogMapper;
import com.Zx1nggg.FAMS.modules.log.mapper.AlarmRecordMapper;
import com.Zx1nggg.FAMS.modules.log.mapper.AlarmRuleMapper;
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
 * 告警规则从 sys_alarm_rule 动态读取；异常持续期间聚合同一事件，恢复后自动解决。
 */
@Slf4j
@Component
public class IotDataSimulator {

    private static final String REDIS_LATEST_PREFIX = "iot:latest:";
    private static final String REDIS_LATEST_FARM_PREFIX = "iot:latest:farm:";
    private static final String REDIS_HISTORY_PREFIX = "iot:history:";
    private static final byte ALARM_PENDING = 0;
    private static final byte ALARM_RESOLVED = 3;

    @Autowired
    private PondMapper pondMapper;

    @Autowired
    private AlarmRecordMapper alarmRecordMapper;

    @Autowired
    private AlarmRuleMapper alarmRuleMapper;

    @Autowired
    private AlarmActionLogMapper alarmActionLogMapper;

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
        List<AlarmRule> iotRules = alarmRuleMapper.selectList(
                new LambdaQueryWrapper<AlarmRule>()
                        .eq(AlarmRule::getEnabled, (byte) 1)
                        .eq(AlarmRule::getSourceType, "IOT"));

        for (Pond pond : ponds) {
            try {
                Map<String, String> data = generateSensorData(pond, hour, now);
                writeToRedis(pond, data);
                checkAlarms(pond, data, iotRules);
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

    private void checkAlarms(Pond pond, Map<String, String> data, List<AlarmRule> rules) {
        Map<String, BigDecimal> metricValues = Map.of(
                "water_temp", new BigDecimal(data.get("waterTemp")),
                "dissolved_oxygen", new BigDecimal(data.get("dissolvedOxygen")),
                "ph_value", new BigDecimal(data.get("phValue")));

        for (AlarmRule rule : rules) {
            if (!appliesToPond(rule, pond)) continue;
            BigDecimal value = metricValues.get(rule.getMetricCode());
            if (value == null) continue;
            if (matches(rule, value)) {
                upsertAlarm(pond, rule, value);
            } else {
                recoverAlarm(pond, rule);
            }
        }
    }

    private boolean appliesToPond(AlarmRule rule, Pond pond) {
        if ("GLOBAL".equals(rule.getScopeType())) return true;
        return "FARM".equals(rule.getScopeType()) && Objects.equals(rule.getFarmId(), pond.getFarmId());
    }

    private boolean matches(AlarmRule rule, BigDecimal value) {
        if (rule.getThresholdOperator() == null || rule.getThresholdValue() == null) return false;
        int low = value.compareTo(rule.getThresholdValue());
        return switch (rule.getThresholdOperator()) {
            case "LT" -> low < 0;
            case "LE" -> low <= 0;
            case "GT" -> low > 0;
            case "GE" -> low >= 0;
            case "EQ" -> low == 0;
            case "BETWEEN" -> rule.getThresholdValueHigh() != null
                    && low >= 0 && value.compareTo(rule.getThresholdValueHigh()) <= 0;
            default -> false;
        };
    }

    private void upsertAlarm(Pond pond, AlarmRule rule, BigDecimal value) {
        String dedupKey = buildDedupKey(pond, rule);
        LocalDateTime now = LocalDateTime.now();
        AlarmRecord alarm = findActiveAlarm(dedupKey);
        String message = buildAlarmMessage(pond, rule, value);

        if (alarm == null) {
            alarm = new AlarmRecord();
            alarm.setFarmId(pond.getFarmId());
            alarm.setPondId(pond.getId());
            alarm.setRuleId(rule.getId());
            alarm.setAlarmCode(rule.getAlarmCode());
            alarm.setTitle(rule.getRuleName());
            alarm.setMessage(message);
            alarm.setSourceType("IOT");
            alarm.setSeverity(rule.getSeverity());
            alarm.setStatus(ALARM_PENDING);
            alarm.setMetricCode(rule.getMetricCode());
            alarm.setTriggerValue(value);
            alarm.setThresholdOperator(rule.getThresholdOperator());
            alarm.setThresholdValue(rule.getThresholdValue());
            alarm.setThresholdValueHigh(rule.getThresholdValueHigh());
            alarm.setMetricUnit(rule.getMetricUnit());
            alarm.setDedupKey(dedupKey);
            alarm.setOccurrenceCount(1);
            alarm.setFirstOccurredAt(now);
            alarm.setLastOccurredAt(now);
            alarmRecordMapper.insert(alarm);
            log.warn("[IoT-Alarm] 新告警 | {} | {} | {}", rule.getAlarmCode(), pond.getPondName(), message);
            return;
        }

        alarm.setTriggerValue(value);
        alarm.setMessage(message);
        alarm.setSeverity(rule.getSeverity());
        alarm.setLastOccurredAt(now);
        alarm.setOccurrenceCount((alarm.getOccurrenceCount() == null ? 0 : alarm.getOccurrenceCount()) + 1);
        alarmRecordMapper.updateById(alarm);
    }

    private void recoverAlarm(Pond pond, AlarmRule rule) {
        AlarmRecord alarm = findActiveAlarm(buildDedupKey(pond, rule));
        if (alarm == null) return;

        LocalDateTime now = LocalDateTime.now();
        byte previous = alarm.getStatus();
        alarm.setStatus(ALARM_RESOLVED);
        alarm.setRecoveredAt(now);
        alarm.setResolvedAt(now);
        alarm.setResolutionRemark("监测指标已自动恢复正常");
        alarmRecordMapper.updateById(alarm);

        AlarmActionLog action = new AlarmActionLog();
        action.setAlarmId(alarm.getId());
        action.setActionType("AUTO_RECOVER");
        action.setFromStatus(previous);
        action.setToStatus(ALARM_RESOLVED);
        action.setActionRemark("监测指标已自动恢复正常");
        action.setCreatedAt(now);
        alarmActionLogMapper.insert(action);
    }

    private AlarmRecord findActiveAlarm(String dedupKey) {
        return alarmRecordMapper.selectOne(
                new LambdaQueryWrapper<AlarmRecord>()
                        .eq(AlarmRecord::getDedupKey, dedupKey)
                        .in(AlarmRecord::getStatus, 0, 1, 2)
                        .last("LIMIT 1"));
    }

    private String buildDedupKey(Pond pond, AlarmRule rule) {
        return pond.getFarmId() + ":" + pond.getId() + ":" + rule.getAlarmCode() + ":" + rule.getMetricCode();
    }

    private String buildAlarmMessage(Pond pond, AlarmRule rule, BigDecimal value) {
        String unit = rule.getMetricUnit() == null ? "" : " " + rule.getMetricUnit();
        return String.format("%s%s当前值为 %s%s，触发规则“%s”",
                pond.getPondName(), metricLabel(rule.getMetricCode()), value.stripTrailingZeros().toPlainString(),
                unit, rule.getRuleName());
    }

    private String metricLabel(String metricCode) {
        return switch (metricCode) {
            case "dissolved_oxygen" -> "溶解氧";
            case "water_temp" -> "水温";
            case "ph_value" -> "pH";
            default -> metricCode;
        };
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
