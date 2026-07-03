package com.Zx1nggg.FAMS.modules.regulator.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * GIS 养殖场分布坐标
 */
@Data
public class FarmGeoVO {
    private Long farmId;
    private String farmName;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String address;
    /** 所在省份 (从 address 提取或在业务层填充) */
    private String province;
    /** 下属池塘数 */
    private Integer pondCount;
    /** normal / warning / critical */
    private String alertStatus;
    /** 当前未处理告警数 */
    private Integer activeAlarmCount;
    /** 主要养殖品种 */
    private String mainSpecies;
    /** 当前存栏量（原始尾数，前端展示应使用此字段） */
    private Long stockCount;
    /** 当前存栏量（万尾，兼容旧版调用） */
    private Long stockAmount;
}
