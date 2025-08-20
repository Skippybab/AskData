package com.mt.agent.controller;

import com.mt.agent.model.Result;
import com.mt.agent.repository.consensus.service.IConsConversationRecordService;
import com.mt.agent.utils.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/consensus")
@Slf4j
@RequiredArgsConstructor
public class ConsensusController {

    private final IConsConversationRecordService consConversationRecordService;

    /**
     * 获取意图
     * @param httpRequest
     * @Return: Result
     * @Author: zzq
     * @Date: 2025/4/29 10:24
     */
    @GetMapping("/intent")
    public Result getIntent(HttpServletRequest httpRequest) {
        try {
            Long LongUserId = (Long) SessionUtil.getAttribute(httpRequest, SessionUtil.LOGIN_USER_ID);
            String userId = LongUserId.toString();

            String intent = consConversationRecordService.getIntent(userId);
            return Result.success(intent);
        } catch (Exception e) {
            log.error("ChatController.chat error", e);
            return Result.error("系统繁忙！");
        }
    }

}
