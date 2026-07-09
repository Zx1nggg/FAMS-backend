package com.Zx1nggg.FAMS.modules.regulator.vo;

import lombok.Data;

import java.util.Map;

@Data
public class AlertStatsVO {
    private Long totalCount;
    private Long activeCount;
    private Long pendingCount;
    private Long processingCount;
    private Long resolvedCount;
    private Long criticalCount;
    private Long todayNewCount;
    private Map<String, Long> byType;
    private Map<String, Long> byLevel;
}
