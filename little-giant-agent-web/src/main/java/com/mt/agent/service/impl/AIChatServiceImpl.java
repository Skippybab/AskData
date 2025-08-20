package com.mt.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mt.agent.ai.enums.AliModelType;
import com.mt.agent.ai.service.AiService;
import com.mt.agent.bottomReply.service.BottomReplyService;
import com.mt.agent.buffer.util.BufferUtil;
import com.mt.agent.consensus.util.ConsensusUtil;
import com.mt.agent.constant.Prompt;
import com.mt.agent.intentFilter.service.IntentFilterService;
import com.mt.agent.model.ChatMessage;
import com.mt.agent.reporter.SubEventReporter;
import com.mt.agent.repository.consensus.entity.ConsConversationRecord;
import com.mt.agent.repository.consensus.mapper.ConsConversationRecordMapper;
import com.mt.agent.service.AIChatService;
import com.mt.agent.utils.PromptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * AI聊天服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatServiceImpl implements AIChatService {

    private final RedissonClient redissonClient;

    private final BufferUtil bufferUtil;

    private final ConsensusUtil consensusUtil;

    private final ConsConversationRecordMapper consConversationRecordMapper;


    private final IntentFilterService intentFilterService;

    private final BottomReplyService bottomReplyService;

    private final AiService aiService;

    /**
     * 聊天历史在Redis中的Map名称
     */
    private static final String CHAT_HISTORY_MAP = "chat_history_map";

    /**
     * 获取RMap实例，使用JsonJacksonCodec序列化方式
     */
    private RMap<Long, List<ChatMessage>> getChatHistoryMap() {
        return redissonClient.getMap(CHAT_HISTORY_MAP, JsonJacksonCodec.INSTANCE);
    }

    @Override
    public void saveChatHistory(Long userId, List<ChatMessage> chatHistory) {
        if (userId == null || chatHistory == null) {
            log.warn("保存聊天历史失败：用户ID或聊天记录为空");
            return;
        }

        try {
            // 创建新的ArrayList，避免使用subList潜在问题
            List<ChatMessage> historyToSave = new ArrayList<>(chatHistory);

            // // 只保留最新的6条对话信息
            // if (historyToSave.size() > MAX_HISTORY_SIZE) {
            // historyToSave = new ArrayList<>(
            // historyToSave.subList(historyToSave.size() - MAX_HISTORY_SIZE,
            // historyToSave.size()));
            // }

            // 获取带JsonJacksonCodec的Map
            RMap<Long, List<ChatMessage>> chatHistoryMap = getChatHistoryMap();
            chatHistoryMap.put(userId, historyToSave);

            log.info("用户[{}]的聊天历史保存成功，共{}条记录", userId, historyToSave.size());
        } catch (Exception e) {
            log.error("保存聊天历史出错：", e);
        }
    }

    @Override
    public List<ChatMessage> loadChatHistory(Long userId) {
        if (userId == null) {
            log.warn("加载聊天历史失败：用户ID为空");
            return new ArrayList<>();
        }

        try {
            // 获取带JsonJacksonCodec的Map
            RMap<Long, List<ChatMessage>> chatHistoryMap = getChatHistoryMap();
            List<ChatMessage> chatHistory = chatHistoryMap.get(userId);

            if (chatHistory == null) {
                log.info("用户[{}]无聊天历史记录", userId);
                return new ArrayList<>();
            }

            // 创建新的ArrayList，避免使用可能有问题的List实现
            List<ChatMessage> result = new ArrayList<>(chatHistory);

            log.info("用户[{}]的聊天历史加载成功，共{}条记录", userId, result.size());
            return result;
        } catch (Exception e) {
            log.error("加载聊天历史出错：", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void resetChatHistory(Long userId) {
        if (userId == null) {
            log.warn("重置聊天历史失败：用户ID为空");
            return;
        }

        try {
            // 获取带JsonJacksonCodec的Map
            RMap<Long, List<ChatMessage>> chatHistoryMap = getChatHistoryMap();

            // 删除该用户的聊天历史
            chatHistoryMap.remove(userId);
            String StringUserId = userId + "";
            bufferUtil.clearUserCache(StringUserId);
            bufferUtil.clearDataSourceCache(StringUserId);
            bufferUtil.clearPythonCode(StringUserId);


            // 删除数据库的历史对话记录
            ConsConversationRecord record = ConsConversationRecord.builder().available(0).build();
            LambdaQueryWrapper<ConsConversationRecord> wrapper = new LambdaQueryWrapper<ConsConversationRecord>()
                    .eq(ConsConversationRecord::getUserId, userId);

            consConversationRecordMapper.update(record, wrapper);

            // 清除Redis历史对话缓存
            bufferUtil.clearAllHistoryLogs(StringUserId);

            // 清除任务状态
            consensusUtil.deleteConsensusJsonByUserId(StringUserId);

            log.info("用户[{}]的聊天历史已成功重置", userId);
        } catch (Exception e) {
            log.error("重置聊天历史出错：", e);
        }
    }

    @Override
    public String planNewTask(String message, String userId, SubEventReporter reporter) {

        //安全过滤

        String filtering = intentFilterService.intentionFiltering(message, userId);

        // 判断用户问题是否安全
        if (filtering.equals("Y")) {
            // 【共识管理模块】
//            return consensusService.planTask(message, userId, reporter);
            return null;
        } else {
            // 【兜底回复】
            return bottomReplyService.reply("用户的提问存在潜在风险", userId);
        }
    }

    @Override
    public String adjustTaskPlan(String message, String userId, SubEventReporter reporter) {

        //调用共识模块
//        return consensusService.planTask(message, userId, reporter);

        return null;
    }

    @Override
    public boolean judgeUserAgree(String message, String userId) {

        // 获取当前任务的历史对话
        String diagHistory = bufferUtil.getTaskHistoryLogs(userId);

        // 获取方案内容
//        String planning = consensusUtil.getConsensusInfoNoStatus(userId);

        HashMap<String, Object> params = new HashMap<>();
        params.put("diag_history", diagHistory);
//        params.put("plan", planning);
        params.put("user_input", message);

        // 拼接提示词
        String prompt = PromptUtil.replacePromptParams(Prompt.AGREE_OR_DISAGREE_SCHEME, params);
        String result = aiService.chat(prompt, AliModelType.QWEN_3_32B_INSTRUCT);
        if (result.equals("Y")) {
            // 更新共识信息状态全部为已确认
//            consensusUtil.confirmAllConsensus(userId);
            log.info("用户[{}]完全认同任务方案", userId);
            return true;
        }
        log.info("用户[{}]不认同任务方案", userId);
        return false;
    }
}
