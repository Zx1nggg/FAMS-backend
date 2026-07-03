package com.Zx1nggg.FAMS.modules.regulator.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 追溯链中单个节点
 */
@Data
public class TraceNodeVO {
    /** supplier / purchase / stocking / growth / patrol / feed / harvest */
    private String nodeType;
    /** 节点中文名 */
    private String nodeName;
    private LocalDateTime nodeTime;
    /** 节点具体数据(动态字段) */
    private Map<String, Object> detail;
}
