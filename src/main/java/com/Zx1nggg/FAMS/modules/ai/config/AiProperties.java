package com.Zx1nggg.FAMS.modules.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
    private boolean enabled = false;
    private String provider = "openai-responses";
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey;
    private String model;
    private int maxToolRounds = 6;
    private int maxOutputTokens = 1600;
    private int historyLimit = 16;
    private int requestTimeoutSeconds = 60;
    private int perMinuteRequestLimit = 10;
    private int dailyRequestLimit = 200;
    private boolean knowledgeEnabled = false;
    private String vectorStoreId;
    private int knowledgeMaxResults = 8;
    private int streamTimeoutSeconds = 120;

    public boolean isReady() {
        return enabled && hasText(apiKey) && hasText(model) && hasText(baseUrl);
    }

    public boolean isKnowledgeReady() {
        return isReady() && knowledgeEnabled && hasText(vectorStoreId);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
