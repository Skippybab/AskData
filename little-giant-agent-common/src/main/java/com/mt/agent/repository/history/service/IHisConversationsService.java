package com.mt.agent.repository.history.service;

import com.mt.agent.repository.history.entity.HisConversations;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 历史会话表 服务类
 * </p>
 *
 * @author wsx
 * @since 2025-07-10
 */
public interface IHisConversationsService extends IService<HisConversations> {

    /**
     * 根据用户ID获取会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    List<HisConversations> getConversationsByUserId(String userId);

    /**
     * 根据会话ID和用户ID获取会话
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 会话信息
     */
    HisConversations getConversationByIdAndUserId(String conversationId, String userId);


    /**
     * 更新会话状态
     * @param conversationId 会话ID
     * @param status 新状态
     * @return 是否更新成功
     */
    boolean updateConversationStatus(String conversationId, String status);

    /**
     * 更新会话标题
     * @param conversationId 会话ID
     * @param title 新标题
     * @return 是否更新成功
     */
    boolean updateConversationTitle(String conversationId, String title);

}
