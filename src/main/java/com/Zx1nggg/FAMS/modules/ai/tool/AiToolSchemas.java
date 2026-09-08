package com.Zx1nggg.FAMS.modules.ai.tool;

import java.util.List;
import java.util.Map;

public final class AiToolSchemas {
    private AiToolSchemas() {
    }

    public static Map<String, Object> noArguments() {
        return Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", List.of(),
                "additionalProperties", false);
    }

    public static Map<String, Object> nullableFarmId() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "farm_id", Map.of(
                                "type", List.of("integer", "null"),
                                "description", "养殖场ID。农户身份必须传null，系统会强制使用当前养殖场。")),
                "required", List.of("farm_id"),
                "additionalProperties", false);
    }
}
