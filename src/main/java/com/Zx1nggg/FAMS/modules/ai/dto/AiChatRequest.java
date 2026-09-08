package com.Zx1nggg.FAMS.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatRequest {
    @Size(max = 36, message = "会话标识长度不合法")
    private String conversationId;

    @NotBlank(message = "消息不能为空")
    @Size(max = 2000, message = "单条消息不能超过2000个字符")
    private String message;
}
