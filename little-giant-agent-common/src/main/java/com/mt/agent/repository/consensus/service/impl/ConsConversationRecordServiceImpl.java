package com.mt.agent.repository.consensus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mt.agent.repository.consensus.entity.ConsConversationRecord;
import com.mt.agent.repository.consensus.mapper.ConsConversationRecordMapper;
import com.mt.agent.repository.consensus.service.IConsConversationRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mt.agent.utils.DateExtendUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * <p>
 * 单轮对话记录表 服务实现类
 * </p>
 *
 * @author zzq
 * @since 2025-04-24
 */
@Service
public class ConsConversationRecordServiceImpl extends ServiceImpl<ConsConversationRecordMapper, ConsConversationRecord>
        implements IConsConversationRecordService {

    @Override
    public String getIntent(String userId) {
        QueryWrapper<ConsConversationRecord> queryWrapper = new QueryWrapper<>();
        // queryWrapper.select("intent_text");
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc("chat_time");
        queryWrapper.last("limit 1");

        ConsConversationRecord one = this.list(queryWrapper).get(0);

        // 修改为直接获取 intent_text 的值，避免类型转换问题
        return Optional.ofNullable(one)
                .map(ConsConversationRecord::getIntentText)
                .orElse("");
    }

    @Override
    public String getHistoryLogs(String userId) {
        List<Map<String, Object>> list = baseMapper.selectMaps(
                new QueryWrapper<ConsConversationRecord>()
                        .eq("user_id", userId)
                        .eq("available", 1)
                        .gt("chat_time", System.currentTimeMillis() - 10 * 60 * 1000)
                        .eq("available", 1)
                        .orderByDesc("chat_time")
                        .select("chat_time", "intent_text"));

        StringBuilder historyLogs = new StringBuilder();
        historyLogs.append("历史意图信息，按照逗号分隔的格式\"时间,历史意图\"列举如下:\n");
        if (list.isEmpty()) {
            historyLogs.append("该用户暂无历史记录");



            return historyLogs.toString();
        }

        for (Map<String, Object> record : list) {
            // 处理时间戳，先转换为Long类型
            Long chatTime = Long.parseLong(record.get("chat_time").toString());
            String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new Date(chatTime));
            String intentText = (String) record.get("intent_text");
            // summary中的换行字符替换为空格
            intentText = intentText.replaceAll("\n", " ");

            historyLogs.append(formattedTime)
                    .append(",")
                    .append(intentText)
                    .append("\n");
        }

        return historyLogs.toString();
    }

}