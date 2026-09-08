package com.Zx1nggg.FAMS.modules.ai.tool;

import com.Zx1nggg.FAMS.modules.log.service.IAlarmRecordService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class ActiveAlarmsTool implements AiTool {
    private final IAlarmRecordService alarmRecordService;

    public ActiveAlarmsTool(IAlarmRecordService alarmRecordService) {
        this.alarmRecordService = alarmRecordService;
    }

    @Override public String name() { return "get_active_alarms"; }

    @Override
    public String description() {
        return "读取尚未关闭的告警，结果按状态、严重程度和发生时间排序。农户只能读取当前场区；监管或管理员可传养殖场ID，传null表示全局。";
    }

    @Override public Set<String> allowedRoles() { return Set.of("FARMER", "REGULATOR", "ADMIN"); }

    @Override public Map<String, Object> parameters() { return AiToolSchemas.nullableFarmId(); }

    @Override
    public Object execute(AiToolContext context, JsonNode arguments) {
        Long farmId = "FARMER".equals(context.role()) ? context.farmId() : nullableLong(arguments.get("farm_id"));
        Page<?> page = alarmRecordService.pageQuery(1, 20, farmId, null, true, null, null, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("farmId", farmId);
        result.put("total", page.getTotal());
        result.put("alarms", page.getRecords());
        return result;
    }

    private Long nullableLong(JsonNode node) {
        return node == null || node.isNull() ? null : node.longValue();
    }
}
