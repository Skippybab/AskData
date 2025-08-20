package com.mt.agent.sysUtil;

import com.mt.agent.ai.enums.AliModelType;
import com.mt.agent.ai.service.AiService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.mt.agent.constant.PromptParam.*;
import static com.mt.agent.customtask.constant.Prompt.*;

@Slf4j
@Component
public class AIQueryAnswerUtil {

    @Autowired
    private AiService aiServiceV12;

    private AliModelType aliModelType = AliModelType.QWEN_3_8B_INSTRUCT;



    // 系统功能解答
    public String sysQueryAnswer(Object text) {
        String naturalLanguage = null;
        if (text instanceof String) {
            naturalLanguage = (String) text;
        }
        String prompt = SYS_QUESTION_ANSWER;
        prompt = prompt.replace("${sqlQueryText}", naturalLanguage);
        prompt = prompt.replace("${chatFun}", SYS_FUN_PARAMS);
        prompt = prompt.replace("${operateFun}", OPERATE_FUN);
        prompt = prompt.replace("${visualizations}", SYS_VISUAL_STYLE);
        prompt = prompt.replace("${tableSchema}", TABLE_SCHEMA);
        log.info("【定制任务执行】系统问题回复提示词: \n{}", prompt);
        Flowable<String> stringFlowable = aiServiceV12.chatStream(prompt, aliModelType);
        String result = stringFlowable.reduce((acc, str) -> acc + str).blockingGet();
        log.info("【定制任务执行】系统问题回复结果: \n{}",  result);
        return result;
    }


}
