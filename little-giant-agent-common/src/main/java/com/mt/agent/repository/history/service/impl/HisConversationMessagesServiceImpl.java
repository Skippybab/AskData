package com.mt.agent.repository.history.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mt.agent.repository.history.entity.HisConversationMessages;
import com.mt.agent.repository.history.entity.HisConversations;
import com.mt.agent.repository.history.mapper.HisConversationMessagesMapper;
import com.mt.agent.repository.history.mapper.HisConversationsMapper;
import com.mt.agent.repository.history.service.IHisConversationMessagesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mt.agent.utils.DateExtendUtil;
import com.mt.agent.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 历史会话消息表 服务实现类
 * </p>
 *
 * @author wsx
 * @since 2025-07-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HisConversationMessagesServiceImpl extends ServiceImpl<HisConversationMessagesMapper, HisConversationMessages>
        implements IHisConversationMessagesService {

    private final HisConversationsMapper hisConversationsMapper;
    @Override
    public List<HisConversationMessages> getMessagesByConversationId(String conversationId) {
        try {
            LambdaQueryWrapper<HisConversationMessages> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HisConversationMessages::getConversationId, conversationId)
                    .orderByAsc(HisConversationMessages::getCreatedAt); // 按时间正序
            return this.list(queryWrapper);
        } catch (Exception e) {
            log.error("根据会话ID获取消息列表失败，conversationId: {}", conversationId, e);
            throw new RuntimeException("获取消息列表失败", e);
        }
    }

    @Override
    public List<HisConversationMessages> getMessagesByConversationId(String conversationId, Integer limit, Integer offset) {
        try {
            LambdaQueryWrapper<HisConversationMessages> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HisConversationMessages::getConversationId, conversationId)
                    .orderByAsc(HisConversationMessages::getCreatedAt) // 按时间正序
                    .last("LIMIT " + limit + " OFFSET " + offset); // 分页查询
            return this.list(queryWrapper);
        } catch (Exception e) {
            log.error("根据会话ID分页获取消息列表失败，conversationId: {}, limit: {}, offset: {}",
                    conversationId, limit, offset, e);
            throw new RuntimeException("分页获取消息列表失败", e);
        }
    }

    @Override
    public List<HisConversationMessages> getLatestMessages(String conversationId, Integer limit) {
        try {
            LambdaQueryWrapper<HisConversationMessages> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HisConversationMessages::getConversationId, conversationId)
                    .orderByDesc(HisConversationMessages::getCreatedAt) // 按时间倒序
                    .last("LIMIT " + limit); // 限制数量

            List<HisConversationMessages> messages = this.list(queryWrapper);
            // 重新按时间正序排列
            messages.sort((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()));
            return messages;
        } catch (Exception e) {
            log.error("获取最新消息列表失败，conversationId: {}, limit: {}", conversationId, limit, e);
            throw new RuntimeException("获取最新消息列表失败", e);
        }
    }

    @Override
    public void saveChatHistory(String conversationId, String messageId, String content, String think,
                                Boolean newChat, Long userId, String role, String time) {
        // 判断是否形成新会话
        if(newChat) {
            HisConversations conversations = new HisConversations();
            conversations.setId(conversationId);
            conversations.setStatus((short) 1);
            conversations.setUserId(userId);
            hisConversationsMapper.insert(conversations);
        }
        // 保存对话记录
        HisConversationMessages messages = new HisConversationMessages();
        messages.setConversationId(conversationId);
        messages.setMessageId(messageId);
        messages.setContent(content);
        messages.setRole(role);
        messages.setThink(think);
        messages.setCreatedAt(time);
        this.save(messages);
    }
}
