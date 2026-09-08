package com.Zx1nggg.FAMS.modules.ai.service;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.ai.config.AiProperties;
import com.Zx1nggg.FAMS.modules.ai.entity.AiConversation;
import com.Zx1nggg.FAMS.modules.ai.entity.AiMessage;
import com.Zx1nggg.FAMS.modules.ai.mapper.AiConversationMapper;
import com.Zx1nggg.FAMS.modules.ai.mapper.AiMessageMapper;
import com.Zx1nggg.FAMS.modules.ai.tool.AiToolContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class AiAuditService {
    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final AiProperties properties;

    public AiAuditService(AiConversationMapper conversationMapper, AiMessageMapper messageMapper,
                          AiProperties properties) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.properties = properties;
    }

    @Transactional
    public AiConversation getOrCreate(String conversationKey, String firstMessage, AiToolContext context) {
        if (conversationKey != null && !conversationKey.isBlank()) {
            AiConversation existing = conversationMapper.selectOne(new LambdaQueryWrapper<AiConversation>()
                    .eq(AiConversation::getConversationKey, conversationKey).last("LIMIT 1"));
            if (existing == null) throw new BusinessException(404, "AI会话不存在或已被删除");
            if (!Objects.equals(existing.getUserId(), context.userId())) {
                throw new BusinessException(403, "无权访问其他用户的AI会话");
            }
            if (!Objects.equals(existing.getUserRole(), context.role())
                    || ("FARMER".equals(context.role()) && !Objects.equals(existing.getFarmId(), context.farmId()))) {
                throw new BusinessException(409, "用户角色或当前养殖场已变化，请新建AI会话");
            }
            if (!Byte.valueOf((byte) 1).equals(existing.getStatus())) {
                throw new BusinessException(409, "AI会话已关闭，请新建会话");
            }
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        AiConversation conversation = new AiConversation();
        conversation.setConversationKey(UUID.randomUUID().toString());
        conversation.setUserId(context.userId());
        conversation.setFarmId(context.farmId());
        conversation.setUserRole(context.role());
        conversation.setTitle(toTitle(firstMessage));
        conversation.setStatus((byte) 1);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversationMapper.insert(conversation);
        return conversation;
    }

    public List<Object> loadHistory(Long conversationId) {
        List<AiMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .in(AiMessage::getMessageRole, "user", "assistant")
                .orderByDesc(AiMessage::getId)
                .last("LIMIT " + Math.max(2, Math.min(40, properties.getHistoryLimit()))));
        Collections.reverse(messages);
        List<Object> input = new ArrayList<>();
        for (AiMessage message : messages) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", message.getMessageRole());
            item.put("content", message.getContent());
            input.add(item);
        }
        return input;
    }

    @Transactional
    public void record(AiConversation conversation, String role, String content, String model,
                       String providerResponseId, String toolCallsJson,
                       Integer inputTokens, Integer outputTokens, Integer totalTokens,
                       long latencyMs, String errorCode) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversation.getId());
        message.setMessageRole(role);
        message.setContent(content);
        message.setModel(model);
        message.setProviderResponseId(providerResponseId);
        message.setToolCallsJson(toolCallsJson);
        message.setInputTokens(inputTokens);
        message.setOutputTokens(outputTokens);
        message.setTotalTokens(totalTokens);
        message.setLatencyMs(latencyMs);
        message.setErrorCode(errorCode);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
    }

    private String toTitle(String message) {
        String normalized = message == null ? "新会话" : message.strip().replaceAll("\\s+", " ");
        return normalized.length() <= 60 ? normalized : normalized.substring(0, 60);
    }
}
