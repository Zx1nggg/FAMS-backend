package com.Zx1nggg.FAMS.modules.log.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "告警记录视图")
public class AlarmRecordVO {

    private Long id;
    private Long farmId;
    private String farmName;
    private Byte alarmLevel;
    private String alarmType;
    private String alarmContent;
    private Byte isHandled;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
}
