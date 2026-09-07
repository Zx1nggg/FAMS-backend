package com.Zx1nggg.FAMS.modules.regulator.vo;

import com.Zx1nggg.FAMS.modules.regulator.entity.InspectionRecord;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InspectionRecordVO extends InspectionRecord {
    private String farmName;
    private String pondName;
}
