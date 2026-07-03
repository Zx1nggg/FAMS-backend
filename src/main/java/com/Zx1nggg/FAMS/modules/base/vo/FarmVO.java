package com.Zx1nggg.FAMS.modules.base.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FarmVO {
    private Long id;
    private Long userId;
    private String farmName;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String address;
}
