package com.Zx1nggg.FAMS.modules.ai.tool;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiToolRegistry {
    private final Map<String, AiTool> tools;
    private final ObjectMapper objectMapper;

    public AiToolRegistry(List<AiTool> tools, ObjectMapper objectMapper) {
        this.tools = tools.stream().collect(Collectors.toMap(
                AiTool::name, Function.identity(), (left, right) -> {
                    throw new IllegalStateException("重复的AI工具名称: " + left.name());
                }, LinkedHashMap::new));
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> definitionsFor(String role) {
        return tools.values().stream()
                .filter(tool -> tool.allowedRoles().contains(role))
                .map(AiTool::definition)
                .toList();
    }

    public Object execute(String name, String rawArguments, AiToolContext context) {
        AiTool tool = tools.get(name);
        if (tool == null) throw new BusinessException(400, "模型请求了未注册的工具");
        if (!tool.allowedRoles().contains(context.role())) {
            throw new BusinessException(403, "当前角色无权调用该AI工具");
        }
        try {
            JsonNode arguments = objectMapper.readTree(
                    rawArguments == null || rawArguments.isBlank() ? "{}" : rawArguments);
            return tool.execute(context, arguments);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(400, "AI工具参数格式错误");
        }
    }
}
