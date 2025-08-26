package com.mt.agent.workflow.api.bottomReply.service.impl;

import com.mt.agent.workflow.api.ai.enums.AliModelType;
import com.mt.agent.workflow.api.ai.service.AiService;
import com.mt.agent.workflow.api.bottomReply.constant.Prompt;
import com.mt.agent.workflow.api.bottomReply.service.BottomReplyService;
import com.mt.agent.workflow.api.constant.PromptParam;

import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BottomReplyServiceImpl implements BottomReplyService {

    @Autowired
    private AiService aiService;


    // 大模型配置
    private static final AliModelType ALI_MODEL_TYPE = AliModelType.QWEN_3_14B_INSTRUCT;


    @Override
    public String replyForExecution(String question, String dialogHistory, String executions, String taskName, String userId, String background) {
        // 获取提示词模板
        String promptTemplate = Prompt.BOTTOM_REPLY_EXCUTE;

        // 拼接提示词
        String sysFunShort = PromptParam.SYS_FUN_SHORT;
        sysFunShort = sysFunShort.replace("{{all_table_names}}", PromptParam.TABLE_SCHEMA);

        promptTemplate = promptTemplate.replace("{{sys_fun_short}}", sysFunShort != null ? sysFunShort : "");
        promptTemplate = promptTemplate.replace("{{diag_history}}", dialogHistory != null ? dialogHistory : "");
        promptTemplate = promptTemplate.replace("{{task_name}}", taskName != null ? taskName : "");
        promptTemplate = promptTemplate.replace("{{question}}", question != null ? question : "");
        promptTemplate = promptTemplate.replace("{{executions}}", executions != null ? executions : "");
        promptTemplate = promptTemplate.replace("{{background}}", background != null ? background : "");

//        return aiService.chatStream(promptTemplate, ALI_MODEL_TYPE);
        return aiService.chat(promptTemplate, ALI_MODEL_TYPE);
    }


}
