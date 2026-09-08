package com.Zx1nggg.FAMS.modules.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Set;

public interface AiTool {
    String name();

    String description();

    Set<String> allowedRoles();

    Map<String, Object> parameters();

    Object execute(AiToolContext context, JsonNode arguments);

    default Map<String, Object> definition() {
        return Map.of(
                "type", "function",
                "name", name(),
                "description", description(),
                "parameters", parameters(),
                "strict", true);
    }
}
