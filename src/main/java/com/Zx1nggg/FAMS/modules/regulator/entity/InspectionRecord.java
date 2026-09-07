package com.Zx1nggg.FAMS.modules.regulator.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_inspection_record")
public class InspectionRecord {
    @TableId(type = IdType.AUTO) private Long id;
    private Long farmId;
    private Long pondId;
    private LocalDate inspectionDate;
    private String inspectionType;
    private String inspectionItem;
    private String result;
    private String inspectorName;
    private Long inspectorId;
    private String description;
    private String unqualifiedReason;
    private String attachmentUrls;
    private String rectifyStatus;
    private LocalDate rectifyDeadline;
    private String rectifyRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic private Integer isDeleted;
}
