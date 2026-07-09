package com.Zx1nggg.FAMS.modules.regulator.vo;

import lombok.Data;

@Data
public class AlertTrendVO {
    private String date;
    private Integer totalCount;
    private Integer criticalCount;
    private Integer warningCount;
    private Integer infoCount;
}
