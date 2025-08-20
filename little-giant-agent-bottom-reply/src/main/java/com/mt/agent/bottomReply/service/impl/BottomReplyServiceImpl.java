package com.mt.agent.bottomReply.service.impl;

import com.mt.agent.ai.enums.AliModelType;
import com.mt.agent.ai.enums.SiliconFlowModelType;
import com.mt.agent.ai.model.SiliconFlowRequestConfig;
import com.mt.agent.ai.service.AiService;
import com.mt.agent.ai.service.SiliconFlowAiService;
import com.mt.agent.bottomReply.constant.Prompt;
import com.mt.agent.bottomReply.service.BottomReplyService;
import com.mt.agent.constant.PromptParam;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class BottomReplyServiceImpl implements BottomReplyService {

    @Autowired
    private AiService aiService;

    @Autowired
    private SiliconFlowAiService siliconFlowAiService;


    // 大模型配置
    private static final AliModelType ALI_MODEL_TYPE = AliModelType.QWEN_3_14B_INSTRUCT;


    @Override
    public String reply(String reason, String userId) {
        // 获取提示词模板
        String promptTemplate = Prompt.BOTTOM_REPLY;

        // 拼接提示词
        Map<String, String> params = new HashMap<>();
        // 从缓存中获取用户意图文本
        String intentText = null;
        // 获取系统支持的功能列表
        String systemFunctions = PromptParam.CHAT_SYS_FUN;

//        params.put("intentText", intentText);
        params.put("reason", reason);
        params.put("systemFunctions", systemFunctions);

        String finalPrompt = promptTemplate;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = "${" + entry.getKey() + "}";
            String value = entry.getValue();
            finalPrompt = finalPrompt.replace(key, value);
        }


        // 调用大模型生成回复
        Flowable<String> stringFlux = aiService.chatStream(finalPrompt, ALI_MODEL_TYPE);
        String reply = stringFlux.reduce((acc, str) -> acc + str).blockingGet();
        log.info("兜底回复生成结果: {}", reply);

        // 返回回复
        return reply;
    }

    @Override
    public String replyForExecution(String question, String dialogHistory, String executions, String taskName, String userId) {
        // 获取提示词模板
        String promptTemplate = Prompt.BOTTOM_REPLY_EXCUTE;

        // 拼接提示词
        String sysFunShort = PromptParam.SYS_FUN_SHORT;
        sysFunShort = sysFunShort.replace("{{all_table_names}}", PromptParam.CAICT_TABLE_1);

        promptTemplate = promptTemplate.replace("{{sys_fun_short}}", sysFunShort);
        promptTemplate = promptTemplate.replace("{{diag_history}}", dialogHistory);
        promptTemplate = promptTemplate.replace("{{task_name}}", taskName);
        promptTemplate = promptTemplate.replace("{{question}}", question);
        promptTemplate = promptTemplate.replace("{{executions}}", executions);


        return aiService.chat(promptTemplate, ALI_MODEL_TYPE);
    }

    @Override
    public String replyForExecutionSilicon(String question, String dialogHistory, String executions, String taskName, String userId) {
        // 获取提示词模板
        String promptTemplate = Prompt.BOTTOM_REPLY_EXCUTE;

        // 拼接提示词
        String sysFunShort = PromptParam.SYS_FUN_SHORT;
        sysFunShort = sysFunShort.replace("{{all_table_names}}", PromptParam.TABLE_SCHEMA);

        promptTemplate = promptTemplate.replace("{{sys_fun_short}}", sysFunShort);
        promptTemplate = promptTemplate.replace("{{diag_history}}", dialogHistory);
        promptTemplate = promptTemplate.replace("{{task_name}}", taskName);
        promptTemplate = promptTemplate.replace("{{question}}", question);
        promptTemplate = promptTemplate.replace("{{executions}}", executions);

        log.info("兜底模板: {}", promptTemplate);

        SiliconFlowRequestConfig config = SiliconFlowRequestConfig.defaultConfig();
        config.setTopP(0.01);
        config.setTemperature(0.0f);
        config.setModelType(SiliconFlowModelType.QWEN_3_32B);
        return siliconFlowAiService.chat(promptTemplate, config);
    }
}
