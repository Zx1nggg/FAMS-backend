package com.Zx1nggg.FAMS.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_conversation")
public class AiConversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationKey;
    private Long userId;
    private Long farmId;
    private String userRole;
    private String title;
    private Byte status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
