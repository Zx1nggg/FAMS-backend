package com.Zx1nggg.FAMS.modules.ai.tool;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.iot.service.IIotSensorDataService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class LatestWaterQualityTool implements AiTool {
    private final IIotSensorDataService sensorDataService;

    public LatestWaterQualityTool(IIotSensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    @Override public String name() { return "get_latest_water_quality"; }

    @Override
    public String description() {
        return "读取指定养殖场所有池塘的最新水温、溶氧、pH和采集时间。分析水质、传感器时效性或池塘优先级时必须调用。";
    }

    @Override public Set<String> allowedRoles() { return Set.of("FARMER", "REGULATOR", "ADMIN"); }

    @Override public Map<String, Object> parameters() { return AiToolSchemas.nullableFarmId(); }

    @Override
    public Object execute(AiToolContext context, JsonNode arguments) {
        Long farmId = "FARMER".equals(context.role()) ? context.farmId() : nullableLong(arguments.get("farm_id"));
        if (farmId == null) throw new BusinessException(400, "查询水质时必须指定养殖场ID");
        return Map.of("farmId", farmId, "readings", sensorDataService.getLatestByFarmId(farmId));
    }

    private Long nullableLong(JsonNode node) {
        return node == null || node.isNull() ? null : node.longValue();
    }
}
