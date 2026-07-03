package com.Zx1nggg.FAMS.modules.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_alarm_rule")
public class AlarmRule {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String ruleCode;
    private String ruleName;
    private String sourceType;
    private String alarmCode;
    private String metricCode;
    private String thresholdOperator;
    private BigDecimal thresholdValue;
    private BigDecimal thresholdValueHigh;
    private String metricUnit;
    private Byte severity;
    private String scopeType;
    private Long farmId;
    private Long seedlingId;
    private Integer cooldownMinutes;
    private String ruleConfig;
    private Byte enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}