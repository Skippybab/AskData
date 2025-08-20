package com.mt.agent.test;
import com.mt.agent.ai.enums.SiliconFlowModelType;
import com.mt.agent.ai.model.SiliconFlowRequestConfig;
import com.mt.agent.ai.model.impl.SiliconFlowChatClient;
import com.mt.agent.ai.service.SiliconFlowAiService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

@SpringBootTest
@Slf4j
public class SiliconFlowAiServiceTest {
    @Autowired
    private SiliconFlowAiService siliconFlowAiService;

    // 默认模型
    @Test
    public void testBasicChat() {
        String message = "你好";
        String result = siliconFlowAiService.chat(message);
        log.info("result: {}", result);
    }

    // 流式输出测试
    @Test
    public void testStreamChat() {
        log.info("=== 流式输出测试 ===");
        String message = "请简要介绍一下人工智能的发展历程";

        Flowable<String> streamResult = siliconFlowAiService.chatStream(message);

        StringBuilder fullResponse = new StringBuilder();
        streamResult
                .doOnNext(chunk -> {
                    log.info("收到流式输出片段: {}", chunk);
                    fullResponse.append(chunk);
                })
                .doOnComplete(() -> {
                    log.info("流式输出完成，完整回复: {}", fullResponse.toString());
                })
                .doOnError(error -> {
                    log.error("流式输出出错: {}", error.getMessage());
                })
                .blockingSubscribe(); // 阻塞等待完成，仅在测试中使用
    }

    // 配置模型测试 DEEPSEEK_R1
    @Test
    public void testDeepSeekR1Model() {
        log.info("=== DEEPSEEK_R1 模型测试 ===");
        String message = """
                已知【本业务系统】支持用户通过文本对话的方式对数据进行分析，并将分析结果进行可视化，其中                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  - 【数据范围】仅限于以下内容
                ```
                并且字段按照逗号分隔的格式“字段名,数据类型,字段描述”列举如下：
                id,bigint,数据编号,
                district_code,varchar,企业代号,
                city,varchar,所属地市,
                region,varchar,所属区县,
                registered_time,varchar,注册时间,
                registered_capital,double,注册资本_元,
                industry_code,varchar,所属行业数字编号及名称,
                market_experience,int,从事细分市场年限_年,
                year,int,年份,
                employee_num,int,全职员工数量_人,
                total_revenue,double,营业收入_元,
                main_revenue,double,主营业务收入_元,
                main_revenue_ratio,double,主营业务收入占比_%,
                preside_international_std,int ,主持国际/国家标准_个,
                preside_industry_std,int,主持行业标准_个,
                participate_std,int,参与制修订标准总数_个,
                valid_patents,int,有效发明专利数量_个,
                sales_expenses,double,销售费用_元,
                management_expenses,double,管理费用_元,
                main_business_cost,double,主营业务成本_元,
                gross_margin_pct,double,毛利率百分比_%,
                revenue_per_capita,double,人均营业收入_元,
                export_amount,double,出口额_元,
                rd_expense_total,double,研发费用总额_元,
                rd_revenue_ratio,double,研发费用占比_%,
                revenue_growth_rate,double,营业收入增长率_%,
                net_profit,double,净利润总额_元,
                profit_growth_rate,double,净利润增长率_%,
                employee_num_growth_rate,double,全职员工数量增长率_%,
                main_revenue_growth_rate,double,主营业务收入增长率_%,
                net_profit_margin,double,净利率_%,
                gross_margin_pct_growth_rate,double,毛利率增长率_%,
                net_profit_margin_growth_rate,double,净利率增长率_%
                
                ```
                - 【分析能力】仅限于以下功能
                ```
                1、通过文本对话设定“数据查询条件”和结果的“可视化参数”
                2、根据“数据查询条件”生成SQL查询语句
                3、执行SQL语句，获得“数据查询结果”
                4、对若干“数据查询结果”进行二次分析（包括统计、排序、四则运算等）
                5、通过网页将查询结果和分析结果进行可视化
                ```
                
                
                用户和系统的【历史对话】如下：
                ```
                ```
                
                【系统回复】根据您的要求，我已查询出2024年制造业中亏损的企业名单，并进一步筛选出其中创新投入（即研发费用总额）同比降低的企业。以下是相关结果。
                
                【追问用户】您可能还想了解这些企业亏损的具体原因，或者它们在其他财务指标上的表现，例如资产负债率、销售费用占比等。是否需要我进一步分析这些企业的经营状况？","plan_result":"
                
                ```python
                sql = gen_sql("2024年净利润总额小于2023年且年份为2024和2023的企业名称", "2023年到2025年广州市各企业年度营收数据表")
                if sql:
                    result = exec_sql(sql)
                    if result:
                        data = [{"企业名称": item["企业名称"]} for item in result]
                        vis_table("2024年净利润总同比下降企业清单", data)
                        vis_textbox(f"共找到{len(data)}家企业")
                    else:
                        vis_textbox("未查询到符合条件的企业数据")
                else:
                    vis_textbox("未能生成有效SQL查询语句")
                ```","status_code":"002","target_and_view":"【任务目标】统计2024年净利润同比下降的企业数量"}
                ```
                而当前【用户说的话】是最后一句。
                
                系统识别到用户的【任务目标】是 统计2024年净利润同比下降的企业数量
                当前【行动计划】的执行情况如下
                ```
                
                ```
                
                请扮演“业务助手”的角色，以友好的语气按照以下格式给出回答
                ```
                【系统回复】...
                【追问用户】您可能还想了解 ...
                ```
                其中【系统回复】的内容包括：
                - 对【行动计划】的执行状态进行总结，例如查询了xx，获得了yy，输出了zz
                - 为所有执行状态为“失败”的模块给出解释，如果没有“失败”模块则不用解释
                【追问用户】的内容包括
                - 询问用户是否还需要调整【行动计划】的细节
                - 分析用户可能还会继续开启的新任务，并给出建议
                
                """;

        // 注意：DEEPSEEK_R1 是 THINKING_ONLY 模型，只能使用流式思考方法
        try {
            // 尝试使用普通chat方法（应该会自动切换模型或报错）
            log.info("尝试使用普通chat方法调用DEEPSEEK_R1模型...");
            String result = siliconFlowAiService.chat(message, SiliconFlowModelType.QWEN_3_14B);
            log.info("DEEPSEEK_R1 模型回复: {}", result);
        } catch (Exception e) {
            log.warn("DEEPSEEK_R1 模型不支持普通对话模式: {}", e.getMessage());
        }
    }

    // 思考模式测试 非流式 DEEPSEEK_R1
    @Test
    public void testThinkingMode() {
        log.info("=== 思考模式测试 ===");
        String message = "如何解决城市交通拥堵问题？请提供三个具体可行的方案";

        // 测试同步思考模式（使用支持思考的模型）
        log.info("--- 同步思考模式测试 ---");
        SiliconFlowChatClient.ThinkingResult thinkingResult =
                siliconFlowAiService.chatWithThinking(message, SiliconFlowModelType.DEEPSEEK_R1_DISTILL_QWEN_7B);

        log.info("思考过程: {}", thinkingResult.getReasoning());
        log.info("最终回复: {}", thinkingResult.getContent());

    }


    // 思考模式测试 流式 QWEN3-8B
    @Test
    public void testStreamWithThinkingMode() {
        String message = "如何解决城市交通拥堵问题？请提供三个具体可行的方案";

        log.info("--- 流式思考模式测试 (QWEN3-8B) ---");
        SiliconFlowChatClient.ThinkingStreamResult streamThinkingResult =
                siliconFlowAiService.chatStreamWithThinking(message, SiliconFlowModelType.QWEN_3_8B);

        StringBuilder reasoningBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();

        streamThinkingResult.getResponseFlowable()
                .doOnNext(response -> {
                    if (!response.getIncrementalReasoning().isEmpty()) {
                        log.info("增量思考: {}", response.getIncrementalReasoning());
                        reasoningBuilder.append(response.getIncrementalReasoning());
                    }
                    if (!response.getIncrementalContent().isEmpty()) {
                        log.info("增量回复: {}", response.getIncrementalContent());
                        contentBuilder.append(response.getIncrementalContent());
                    }
                })
                .doOnComplete(() -> {
                    log.info("流式思考完成");
                    log.info("完整思考过程: {}", reasoningBuilder.toString());
                    log.info("完整回复内容: {}", contentBuilder.toString());
                })
                .blockingSubscribe();
    }

    // 模型参数配置测试 DEEPSEEK_R1
    @Test
    public void testModelConfiguration() {
        log.info("=== 模型参数配置测试 ===");
        String message = """
                你好啊
                """;


        // 创建高创造性配置
        SiliconFlowRequestConfig config = SiliconFlowRequestConfig.builder()
                .modelType(SiliconFlowModelType.QWEN_3_14B)
                .temperature(0.00f)              // 高温度，增加创造性
                .topP(0.01)                     // 较高的topP，增加多样性
                .topK(20)                      // 较高的topK
                .frequencyPenalty(0.5)         // 适度的频率惩罚，减少重复
                .maxTokens(8000)               // 限制输出长度
                .enableThinking(false)          // 启用思考过程
                .stream(false)                  // 启用流式输出
                .build();

        String conservativeResult = siliconFlowAiService.chat(message, config);
        log.info("保守配置结果: {}", conservativeResult);
    }

    // 错误处理和模型自动切换测试
    @Test
    public void testModelAutoSwitch() {
        log.info("=== 模型自动切换测试 ===");
        String message = "解释一下机器学习的基本概念";

        // 测试普通对话模式的模型自动切换
        log.info("测试普通对话时的模型自动切换...");
        String result = siliconFlowAiService.chat(message, SiliconFlowModelType.DEEPSEEK_R1);
        log.info("自动切换后的结果: {}", result);

        // 测试流式输出的模型选择
        log.info("测试流式输出的模型选择...");
        Flowable<String> streamResult = siliconFlowAiService.chatStream(message, SiliconFlowModelType.DEEPSEEK_V3);

        StringBuilder fullText = new StringBuilder();
        streamResult
                .doOnNext(chunk -> fullText.append(chunk))
                .doOnComplete(() -> log.info("流式输出完成: {}", fullText.toString()))
                .blockingSubscribe();
    }

    // 性能测试
    @Test
    public void testPerformance() {
        log.info("=== 性能测试 ===");
        String message = "简要说明区块链技术的特点";

        // 测试同步调用性能
        long startTime = System.currentTimeMillis();
        String syncResult = siliconFlowAiService.chat(message);
        long syncTime = System.currentTimeMillis() - startTime;
        log.info("同步调用耗时: {}ms, 结果长度: {}", syncTime, syncResult.length());

        // 测试流式调用性能
        startTime = System.currentTimeMillis();
        StringBuilder streamBuilder = new StringBuilder();
        siliconFlowAiService.chatStream(message)
                .doOnNext(streamBuilder::append)
                .blockingSubscribe();
        long streamTime = System.currentTimeMillis() - startTime;
        log.info("流式调用耗时: {}ms, 结果长度: {}", streamTime, streamBuilder.length());

        // 测试思考模式性能
        startTime = System.currentTimeMillis();
        SiliconFlowChatClient.ThinkingResult thinkingResult = siliconFlowAiService.chatWithThinking(message);
        long thinkingTime = System.currentTimeMillis() - startTime;
        log.info("思考模式耗时: {}ms, 思考长度: {}, 回复长度: {}",
                thinkingTime,
                thinkingResult.getReasoning().length(),
                thinkingResult.getContent().length());
    }
}