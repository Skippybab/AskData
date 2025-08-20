package com.mt.agent.intentFilter.service.impl;

import com.mt.agent.ai.enums.AliModelType;
import com.mt.agent.ai.service.AiService;
import com.mt.agent.intentFilter.constant.Prompt;
import com.mt.agent.intentFilter.service.IntentFilterService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IntentFilterServiceImpl implements IntentFilterService {

    @Autowired
    private AiService aiService;

    @Override
    public String intentionFiltering(String question, String userId) {

        // 加载安全识别提示词模板
        String safetyPrompt = Prompt.JUDGE_CUSTOMER_INTENT;
        // 拼接安全识别提示词模板
        safetyPrompt = safetyPrompt.replace("${question}", question);
        log.info("【任务安全模块】提示词:\n {}", safetyPrompt);
        // 调用大模型判断意图是否安全
        Flowable<String> flow = aiService.chatStream(safetyPrompt, AliModelType.QWEN_3_8B_INSTRUCT);
        String safetyResult = flow.reduce((acc, str) -> acc + str).blockingGet();
        log.info("【任务安全模块】调用结果: {}", safetyResult);

        return safetyResult.trim();
    }

}
