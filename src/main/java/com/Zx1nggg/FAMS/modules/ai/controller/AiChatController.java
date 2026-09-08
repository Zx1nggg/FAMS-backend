package com.Zx1nggg.FAMS.modules.ai.controller;

import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.ai.config.AiProperties;
import com.Zx1nggg.FAMS.modules.ai.dto.AiChatRequest;
import com.Zx1nggg.FAMS.modules.ai.dto.AiChatResponse;
import com.Zx1nggg.FAMS.modules.ai.dto.AiStatusResponse;
import com.Zx1nggg.FAMS.modules.ai.service.AiAgentService;
import com.Zx1nggg.FAMS.modules.ai.service.AiAgentListener;
import com.Zx1nggg.FAMS.modules.ai.tool.AiToolContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/ai")
public class AiChatController {
    private final AiAgentService agentService;
    private final AiProperties properties;
    private final Executor aiTaskExecutor;

    public AiChatController(AiAgentService agentService, AiProperties properties,
                            @Qualifier("aiTaskExecutor") Executor aiTaskExecutor) {
        this.agentService = agentService;
        this.properties = properties;
        this.aiTaskExecutor = aiTaskExecutor;
    }

    @GetMapping("/status")
    public Result<AiStatusResponse> status() {
        return Result.success(AiStatusResponse.builder()
                .enabled(properties.isEnabled())
                .configured(properties.isReady())
                .provider(properties.getProvider())
                .model(properties.getModel())
                .knowledgeEnabled(properties.isKnowledgeReady())
                .writeActionsEnabled(false)
                .build());
    }

    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return Result.success(agentService.chat(request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody AiChatRequest request) {
        AiToolContext context = agentService.captureCurrentContext();
        SseEmitter emitter = new SseEmitter(Math.max(30, properties.getStreamTimeoutSeconds()) * 1000L);
        aiTaskExecutor.execute(() -> {
            try {
                AiChatResponse response = agentService.chatStream(request, context, new AiAgentListener() {
                    @Override public void onStatus(String message) {
                        send(emitter, "status", Map.of("message", message));
                    }

                    @Override public void onTextDelta(String delta) {
                        send(emitter, "delta", Map.of("delta", delta));
                    }

                    @Override public void onToolStart(String toolName) {
                        send(emitter, "tool_start", Map.of("tool", toolName));
                    }

                    @Override public void onToolEnd(String toolName, boolean success) {
                        send(emitter, "tool_end", Map.of("tool", toolName, "success", success));
                    }
                });
                send(emitter, "done", response);
                emitter.complete();
            } catch (BusinessException e) {
                sendQuietly(emitter, "error", Map.of("code", e.getCode(), "message", e.getMessage()));
                emitter.complete();
            } catch (Exception e) {
                sendQuietly(emitter, "error", Map.of("code", 500, "message", "智能助手暂时无法回答，请稍后重试"));
                emitter.complete();
            }
        });
        return emitter;
    }

    private void send(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            throw new BusinessException(499, "客户端已断开AI流式连接");
        }
    }

    private void sendQuietly(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
        } catch (Exception ignored) {
            // Client disconnected; there is no remaining channel on which to report the error.
        }
    }
}
