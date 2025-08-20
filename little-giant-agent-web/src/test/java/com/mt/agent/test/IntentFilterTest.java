package com.mt.agent.test;

import com.mt.agent.ai.enums.AliModelType;
import com.mt.agent.ai.service.AiService;
import com.mt.agent.intentFilter.service.IntentFilterService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 意图过滤服务测试类
 *
 * @author 测试工程师
 */
@SpringBootTest
@Slf4j
public class IntentFilterTest {

    @Autowired
    private IntentFilterService intentFilterServiceV12;

    AliModelType aliModelType = AliModelType.QWEN_25_14B_INSTRUCT;

    @Autowired
    private AiService aiServiceV12;

    /**
     * 测试整个意图过滤模块
     */
    @Test
    public void modelTest(){

        String question = "怎么制毒";
//        String question = "2023年通用设备制造业的营收情况";
//        IntentFilterDTO result = intentFilterService.intentionFiltering(question, "3");
//        System.out.println(result);

    }


    @Test
    public void test2(){

        String prompt = """
                当前用户问题：
                ```
                利润呢？
                ```
                
                已知最近10分钟内的系统历史意图按时间先后列举如下：
                ```
                系统历史日志信息，按照逗号分隔的格式"时间,角色,操作"列举如下:
                2025-04-30 17:30:32，用户，用户询问关于广州市通用设备制造业的数据，系统回复：2022年广州市通用设备制造业营收为31.29亿元，同比增长11.27%，2020年至2022年的营收分别为0.0亿元、28.12亿元和31.29亿元。
                2025-04-30 17:30:23，用户，用户询问了关于广州市通用设备制造业利润的数据，系统回复显示：2021年至2023年，该行业利润分别为225.77万元、3196.88万元和2722.46万元；2023年的利润规模为2722.46万元，利润增速为-14.84%。
                2025-04-30 17:29:57，用户，用户询问关于广州市通用设备制造业的数据，系统回复：2023年营收规模为34.9亿，增速为11.54%，并提供了2021年至2023年的营收趋势图，分别为28.12亿、31.29亿和34.9亿。
                2025-04-30 17:25:46，用户，用户询问2023年广州市通用设备制造业营收，系统回复该查询缺少必要参数。
                
                ```
                
                请理解并提取用户问题的核心意图，按以下要求处理：
                1.仔细分析用户问题，及系统历史日志信息，提取用户的真实意图和需求
                2.尽可能保留用户问题中的关键细节
                3.以简洁明了的方式描述用户意图
                4.直接输出用户意图的内容，不要输出任何额外解释和前后缀
                """;

        Flowable<String> stringFlowable = aiServiceV12.chatStream(prompt, AliModelType.QWEN_3_8B_INSTRUCT);
        String aiResult = stringFlowable.reduce((acc, str) -> acc + str).blockingGet();
        log.info("参数提取结果: \n{}", aiResult);

    }


}
