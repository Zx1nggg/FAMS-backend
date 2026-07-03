package com.Zx1nggg.FAMS.modules.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_alarm_action_log")
public class AlarmActionLog {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long alarmId;
    private String actionType;
    private Byte fromStatus;
    private Byte toStatus;
    private Long operatorId;
    private String actionRemark;
    private LocalDateTime createdAt;
}