package com.Zx1nggg.FAMS.modules.base.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SopTemplateVO {
    private Long id;
    private Long categoryId;
    private String stageName;
    private Integer dayOffset;
    private String taskType;
    private String taskDesc;
    private LocalDateTime createTime;
}
