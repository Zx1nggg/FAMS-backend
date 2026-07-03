package com.Zx1nggg.FAMS.modules.regulator.vo;

import lombok.Data;
import java.util.List;

/**
 * 全链路追溯详情
 */
@Data
public class TraceChainVO {
    private String batchNo;
    private String seedlingName;
    /** 按时间顺序排列的追溯节点 */
    private List<TraceNodeVO> nodes;
}
