package com.Zx1nggg.FAMS.modules.lifecycle.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class GrowthChartVO {

    private String batchNo;
    private String pondName;
    private String seedlingName;
    private LocalDate stockingDate;
    private Integer initialQuantity;
    private List<WeekDataPoint> dataPoints;

    @Data
    public static class WeekDataPoint {
        /** 第几周 (从投放日开始算，1-based) */
        private Integer weekNumber;
        /** 周标签，如 "第1周" */
        private String weekLabel;
        /** 本周平均体长(cm) */
        private BigDecimal avgLength;
        /** 本周平均体重(g) */
        private BigDecimal avgWeight;
        /** 本周死亡数(尾) = routineDeathCount + abnormalDeathCount 之和 */
        private Integer weeklyDeaths;
        /** 截至本周累计死亡数(尾) */
        private Integer cumulativeDeaths;
        /** 截至本周存活率(%)，如 99.5 */
        private BigDecimal survivalRate;
    }
}
