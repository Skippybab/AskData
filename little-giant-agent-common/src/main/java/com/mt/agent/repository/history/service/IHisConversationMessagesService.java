package com.mt.agent.repository.history.service;

import com.mt.agent.repository.history.entity.HisConversationMessages;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 历史会话消息表 服务类
 * </p>
 *
 * @author wsx
 * @since 2025-07-10
 */
public interface IHisConversationMessagesService extends IService<HisConversationMessages> {

    /**
     * 根据会话ID获取消息列表
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<HisConversationMessages> getMessagesByConversationId(String conversationId);

    /**
     * 根据会话ID获取消息列表（分页）
     * @param conversationId 会话ID
     * @param limit 限制数量
     * @param offset 偏移量
     * @return 消息列表
     */
    List<HisConversationMessages> getMessagesByConversationId(String conversationId, Integer limit, Integer offset);



    /**
     * 获取最新的N条消息
     * @param conversationId 会话ID
     * @param limit 限制数量
     * @return 消息列表
     */
    List<HisConversationMessages> getLatestMessages(String conversationId, Integer limit);

    /**
     * 保存对话记录
     * @param conversationId 会话ID
     * @param messageId 消息ID，平台专属
     * @param content 内容
     * @param think 思考过程
     * @param newChat 是否新会话
     * @param userId 用户id
     * @param role 角色
     * @param time 时间
     */
    void saveChatHistory(String conversationId, String messageId, String content, String think,
                         Boolean newChat, Long userId, String role, String time);
}
