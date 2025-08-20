package com.mt.agent.sysUtil;

import com.mt.agent.ai.enums.AliModelType;
import com.mt.agent.ai.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AISummaryUtil {

    @Autowired
    private AiService aiService;

    private AliModelType aliModelType = AliModelType.QWEN_3_8B_INSTRUCT;

    public String stepSummary(String summaryTitle){
//        String prompt = SUMMARY;
//
//        prompt = prompt.replace("{summaryTarget}", summaryTarget.toString())
//                .replace("{executeResult}", executeResult);
//        log.info("【定制任务执行】系统总结提示词: \n{}", prompt);
//        Flowable<String> stringFlowable = aiService.chatStream(prompt, aliModelType);
//        String summary = stringFlowable.reduce((acc, str) -> acc + str).blockingGet();
//        log.info("【定制任务执行】系统总结调用结果: \n{}", summary);
        return "生成总结文本（待实现）";
    }
}
