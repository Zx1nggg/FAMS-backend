package com.Zx1nggg.FAMS.modules.lifecycle.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PatrolLogVO {
    private Long id;
    private Long pondId;
    private String pondName;
    private Long farmId;
    private String farmName;
    private String batchNo;
    private LocalDateTime patrolTime;
    private String weather;
    private BigDecimal waterTemp;
    private String waterColor;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
