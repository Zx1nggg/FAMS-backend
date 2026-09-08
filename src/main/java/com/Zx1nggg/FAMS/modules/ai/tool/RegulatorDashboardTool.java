package com.Zx1nggg.FAMS.modules.ai.tool;

import com.Zx1nggg.FAMS.modules.regulator.service.IRegulatorService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class RegulatorDashboardTool implements AiTool {
    private final IRegulatorService regulatorService;

    public RegulatorDashboardTool(IRegulatorService regulatorService) {
        this.regulatorService = regulatorService;
    }

    @Override public String name() { return "get_regulator_dashboard"; }

    @Override
    public String description() {
        return "读取监管全局统计、告警统计和风险督办名单。回答监管态势、重点风险场区或宏观情况时调用。";
    }

    @Override public Set<String> allowedRoles() { return Set.of("REGULATOR", "ADMIN"); }

    @Override public Map<String, Object> parameters() { return AiToolSchemas.noArguments(); }

    @Override
    public Object execute(AiToolContext context, JsonNode arguments) {
        return Map.of(
                "dashboard", regulatorService.getDashboardStats(),
                "alertStats", regulatorService.getAlertStats(),
                "watchlist", regulatorService.getDashboardWatchlist(10));
    }
}
