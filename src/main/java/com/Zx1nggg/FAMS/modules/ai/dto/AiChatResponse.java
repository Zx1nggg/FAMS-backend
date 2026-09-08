package com.Zx1nggg.FAMS.modules.ai.dto;

import com.Zx1nggg.FAMS.modules.ai.model.AiCitation;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiChatResponse {
    private String conversationId;
    private String message;
    private String model;
    private List<String> toolsUsed;
    private List<AiCitation> citations;
    private TokenUsage usage;

    @Data
    @Builder
    public static class TokenUsage {
        private Integer inputTokens;
        private Integer outputTokens;
        private Integer totalTokens;
    }
}
