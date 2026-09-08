package com.Zx1nggg.FAMS.modules.ai.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record AiModelTurn(
        String responseId,
        String text,
        List<JsonNode> outputItems,
        List<AiFunctionCall> functionCalls,
        List<AiCitation> citations,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens) {
}
