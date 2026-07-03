package com.Zx1nggg.FAMS.modules.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 一个完整告警事件；同一 dedupKey 的活动异常持续更新此记录。 */
@Data
@TableName("sys_alarm_record")
public class AlarmRecord implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long farmId;
    private Long pondId;
    private Long ruleId;
    private String alarmCode;
    private String title;
    private String message;
    private String sourceType;
    private Long sourceId;
    private Byte severity;
    private Byte status;
    private String metricCode;
    private BigDecimal triggerValue;
    private String thresholdOperator;
    private BigDecimal thresholdValue;
    private BigDecimal thresholdValueHigh;
    private String metricUnit;
    private String dedupKey;
    @TableField(exist = false)
    private String activeDedupKey;
    private Integer occurrenceCount;
    private LocalDateTime firstOccurredAt;
    private LocalDateTime lastOccurredAt;
    private Long acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private Long resolvedBy;
    private LocalDateTime resolvedAt;
    private String resolutionRemark;
    private LocalDateTime recoveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}