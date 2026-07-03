package com.Zx1nggg.FAMS.modules.regulator.vo;

import lombok.Data;

/**
 * 监管大屏督办名单项
 */
@Data
public class DashboardWatchlistVO {
    private Long farmId;
    private String farmName;
    /** 风险类型: 环境异常 / 违规操作 / 死亡率异常 */
    private String riskType;
    private String riskDescription;
    /** 近期告警次数 */
    private Integer alarmCount;
    /** 异常死亡率 或 问题比率 */
    private String riskMetric;
}
