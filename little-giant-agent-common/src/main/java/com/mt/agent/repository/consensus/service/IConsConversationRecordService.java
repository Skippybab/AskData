package com.mt.agent.repository.consensus.service;

import com.mt.agent.repository.consensus.entity.ConsConversationRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 单轮对话记录表 服务类
 * </p>
 *
 * @author zzq
 * @since 2025-04-24
 */
public interface IConsConversationRecordService extends IService<ConsConversationRecord> {

    /**
     * 获取意图
     * @param userId
     * @Return: String
     * @Author: zzq
     * @Date: 2025/4/29 10:36
     */
    String getIntent(String userId);


    /**
     * 获取历史记录
     * @param userId
     * @Return: String
     * @Author: zzq
     * @Date: 2025/4/29 10:36
     */
    String getHistoryLogs(String userId);
}
