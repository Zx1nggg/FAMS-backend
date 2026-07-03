package com.Zx1nggg.FAMS.modules.log.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlarmActionDTO {
    @Size(max = 500, message = "处理说明不能超过500个字符")
    private String remark;
}