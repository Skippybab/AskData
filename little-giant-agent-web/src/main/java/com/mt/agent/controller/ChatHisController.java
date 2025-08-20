package com.mt.agent.controller;

import com.mt.agent.model.Result;
import com.mt.agent.repository.history.entity.HisConversationMessages;
import com.mt.agent.repository.history.entity.HisConversations;
import com.mt.agent.repository.history.service.IHisConversationMessagesService;
import com.mt.agent.repository.history.service.IHisConversationsService;
import com.mt.agent.utils.SessionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话历史控制器
 * 提供会话历史消息查看和新会话功能
 *
 * @author wsx
 * @date 2025/1/19
 */
@RestController
@RequestMapping("/api/chat/history")
@Slf4j
@RequiredArgsConstructor
public class ChatHisController {

    private final IHisConversationsService hisConversationsService;
    private final IHisConversationMessagesService hisConversationMessagesService;

    /**
     * 查看会话历史消息
     * 根据用户id去conversation表查找此用户状态为1的会话信息
     * 根据会话id去查对应的对话信息记录列表
     * 如果没有状态为1的会话，则返回空对话信息列表
     *
     * @param httpRequest HTTP请求对象
     * @return 会话历史消息
     */
    @GetMapping("/messages")
    public Result getHistoryMessages(HttpServletRequest httpRequest) {
        log.info("[ChatHisController:getHistoryMessages] 开始获取会话历史消息");

        try {
            // 获取用户ID
            Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
            String userId = longUserId.toString();

            // 查找用户状态为1的会话信息
            List<HisConversations> conversations = hisConversationsService.getConversationsByUserId(userId);
            List<HisConversations> activeConversations = conversations.stream()
                    .filter(conversation -> conversation.getStatus() != null && conversation.getStatus() == 1)
                    .toList();

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            List<HisConversationMessages> conversationList = new ArrayList<>();

            // 如果没有活跃会话，返回空列表
            if (activeConversations.isEmpty()) {
                result.put("messageList", conversationList);
                return Result.success(result);
            }
            HisConversations conversation = activeConversations.get(0);
            conversationList = hisConversationMessagesService
                    .getMessagesByConversationId(conversation.getId());

            result.put("messageList", conversationList);

            return Result.success(result);

        } catch (Exception e) {
            log.error("[ChatHisController:getHistoryMessages] 获取会话历史消息失败", e);
            return Result.error("获取会话历史消息失败: " + e.getMessage());
        }
    }

    /**
     * 新会话
     * 根据用户id和conversationId将会话信息状态改为2
     *
     * @param conversationId 会话ID
     * @param httpRequest HTTP请求对象
     * @return 操作结果
     */
    @PostMapping("/new-conversation")
    public Result newConversation(@RequestParam String conversationId, HttpServletRequest httpRequest) {
        log.info("[ChatHisController:newConversation] 开始新会话，会话ID: {}", conversationId);

        try {
            // 获取用户ID
            Long longUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
            String userId = longUserId.toString();

            // 验证会话是否属于当前用户
            HisConversations conversation = hisConversationsService
                    .getConversationByIdAndUserId(conversationId, userId);

            if (conversation == null) {
                return Result.error("会话不存在或无权限操作");
            }

            // 检查当前状态
            if (conversation.getStatus() != null && conversation.getStatus() == 2) {
                return Result.success("会话已关闭");
            }

            hisConversationsService.updateConversationStatus(conversationId, "2");

            return Result.success();
        } catch (Exception e) {
            log.error("[ChatHisController:newConversation] 新会话操作失败，会话ID: {}", conversationId, e);
            return Result.error("新会话操作失败: " + e.getMessage());
        }
    }

}
