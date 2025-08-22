package com.mt.agent.workflow.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("chat_session")
public class ChatSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long userId;
    private String sessionName;
    private Long dbConfigId;
    private Long tableId; // 新增：当前会话关联的表ID
    private Integer status; // 1=活跃,2=已结束,0=已删除
    private Integer messageCount;
    private Long lastMessageAtMs;
    private Long createdAtMs;
    private Long updatedAtMs;
}
