package com.Zx1nggg.FAMS.modules.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiStatusResponse {
    private boolean enabled;
    private boolean configured;
    private String provider;
    private String model;
    private boolean knowledgeEnabled;
    private boolean writeActionsEnabled;
}
