package com.mt.agent.repository.history.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mt.agent.repository.history.entity.HisConversations;
import com.mt.agent.repository.history.mapper.HisConversationsMapper;
import com.mt.agent.repository.history.service.IHisConversationsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 历史会话表 服务实现类
 * </p>
 *
 * @author wsx
 * @since 2025-07-10
 */
@Slf4j
@Service
public class HisConversationsServiceImpl extends ServiceImpl<HisConversationsMapper, HisConversations>
        implements IHisConversationsService {

    @Override
    public List<HisConversations> getConversationsByUserId(String userId) {
        try {
            LambdaQueryWrapper<HisConversations> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HisConversations::getUserId, userId)
                    .orderByDesc(HisConversations::getId); // 按创建时间倒序
            return this.list(queryWrapper);
        } catch (Exception e) {
            log.error("根据用户ID获取会话列表失败，userId: {}", userId, e);
            throw new RuntimeException("获取会话列表失败", e);
        }
    }

    @Override
    public HisConversations getConversationByIdAndUserId(String conversationId, String userId) {
        try {
            LambdaQueryWrapper<HisConversations> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(HisConversations::getId, conversationId)
                    .eq(HisConversations::getUserId, userId);
            return this.getOne(queryWrapper);
        } catch (Exception e) {
            log.error("根据会话ID和用户ID获取会话失败，conversationId: {}, userId: {}", conversationId, userId, e);
            throw new RuntimeException("获取会话失败", e);
        }
    }


    @Override
    public boolean updateConversationStatus(String conversationId, String status) {
        try {
            LambdaUpdateWrapper<HisConversations> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(HisConversations::getId, conversationId)
                    .set(HisConversations::getStatus, status);

            boolean updated = this.update(updateWrapper);
            if (updated) {
                log.info("更新会话状态成功，conversationId: {}, status: {}", conversationId, status);
            }
            return updated;
        } catch (Exception e) {
            log.error("更新会话状态失败，conversationId: {}, status: {}", conversationId, status, e);
            throw new RuntimeException("更新会话状态失败", e);
        }
    }

    @Override
    public boolean updateConversationTitle(String conversationId, String title) {
        try {
            LambdaUpdateWrapper<HisConversations> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(HisConversations::getId, conversationId)
                    .set(HisConversations::getTitle, title);

            boolean updated = this.update(updateWrapper);
            if (updated) {
                log.info("更新会话标题成功，conversationId: {}, title: {}", conversationId, title);
            }
            return updated;
        } catch (Exception e) {
            log.error("更新会话标题失败，conversationId: {}, title: {}", conversationId, title, e);
            throw new RuntimeException("更新会话标题失败", e);
        }
    }

}
