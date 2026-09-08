package com.Zx1nggg.FAMS.modules.ai.tool;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.lifecycle.service.IPondTaskService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class PendingPondTasksTool implements AiTool {
    private final IPondTaskService pondTaskService;

    public PendingPondTasksTool(IPondTaskService pondTaskService) {
        this.pondTaskService = pondTaskService;
    }

    @Override public String name() { return "get_pending_pond_tasks"; }

    @Override
    public String description() {
        return "读取当前养殖场待执行和逾期的SOP任务。回答今日工作、待办、巡塘安排或行动优先级时调用。";
    }

    @Override public Set<String> allowedRoles() { return Set.of("FARMER"); }

    @Override public Map<String, Object> parameters() { return AiToolSchemas.noArguments(); }

    @Override
    public Object execute(AiToolContext context, JsonNode arguments) {
        if (context.farmId() == null) throw new BusinessException(400, "请先选择养殖场");
        var pending = pondTaskService.pageQuery(1, 20, null, context.farmId(), null, (byte) 0, null);
        var overdue = pondTaskService.pageQuery(1, 20, null, context.farmId(), null, (byte) 2, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("farmId", context.farmId());
        result.put("pendingTotal", pending.getTotal());
        result.put("overdueTotal", overdue.getTotal());
        result.put("pending", pending.getRecords());
        result.put("overdue", overdue.getRecords());
        return result;
    }
}
