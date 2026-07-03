package com.Zx1nggg.FAMS.modules.regulator.vo;

import lombok.Data;

/**
 * 监管大屏宏观统计
 */
@Data
public class DashboardStatsVO {
    /** 入网养殖场总数 */
    private Long totalFarms;
    /** 当前存栏活体总量(万尾) */
    private Long totalLiveStock;
    /** 本月下发检疫证明数 */
    private Long monthlyCertificates;
    /** 未处理预警场区数 */
    private Long unhandledAlertFarms;
}
