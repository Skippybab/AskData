package com.mt.agent.test;

import com.mt.agent.ai.enums.AliModelType;
import com.mt.agent.ai.model.RequestConfig;
import com.mt.agent.ai.model.impl.DashScopeChatClient;
import com.mt.agent.ai.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * AiService测试类
 * 测试AiService的各项功能
 */
@SpringBootTest
@Slf4j
public class AiServiceTest {

    @Autowired
    private AiService aiService;

    /**
     * 测试基本同步调用
     */
    @Test
    public void testBasicChat() {
        log.info("=== 测试基本同步调用 ===");
        String prompt = """
                已知【本业务系统】支持用户通过文本对话的方式对数据进行分析和可视化，其中【系统功能】的Python函数声明如下：
                ```
                # 基于文本描述的查询条件，生成sql代码
                def gen_sql(query_text: str, table_name: str) -> str:
                    ...
                
                # 通过java的orm框架执行sql代码，固定返回List<Map>这种格式的查询结果
                def exec_sql(sql_code: str) -> list[dict[str, object]]:
                    ...
                
                # 文本框可视化：输入文本内容，在前端对话界面渲染1个文本框
                def vis_textbox(content: str) -> None:
                    ...
                
                # 信息块可视化：输入标题和数值，在前端对话界面渲染1个指标信息块
                def vis_textblock(title: str, value: float) -> None:
                    ...
                
                # 单柱状图可视化：输入标题、X轴标签列表和Y轴数据列表，在前端对话界面渲染1个单柱状图
                def vis_single_bar(title: str, x_labels: list[str], y_data: list[float]) -> None:
                    ...
                
                # 二分组柱状图可视化：输入标题、X轴标签列表，a、b两组数据的标签和a、b两组数据，在前端对话界面渲染1个二分组柱状图
                def vis_clustered_bar(title: str, x_labels: list[str], bar_a_label: str, bar_a_label: str, group_a: list[float], group_b: list[float]) -> None:
                    ...
                
                # 饼状图可视化：输入标题、标签列表和数据列表，在前端对话界面渲染1个饼状图
                def vis_pie_chart(title: str, labels: list[str], data: list[float]) -> None:
                    ...
                
                # 二维表格可视化：输入表格标题和表格数据，在前端对话界面渲染1个二维表格
                def vis_table(title: str, data: list[dict[str, object]]) -> None:
                    ...
                ```
                
                当前系统可分析的数据表及字段：
                ```
                1. “广州2021年到2023年小巨人企业年度经营数据”，包含以下字段：企业名称、所属地市、注册时间、注册资本(元)、所属区县、行业名称、全职员工数量(人)、年份(yyyy)、营业收入(元)、主营业务收入(元)、主营业务收入占营业收入比重(%)、销售费用(元)、管理费用(元)、主营业务成本(元)、毛利率(%)、人均营业收入(元)、营业收入增长率(%)、净利润总额(元)、净利润增长率(%)、从事细分市场年限、主持制修订国际\\\\国家标准数量(个)、主持制修订行业标准数量(个)、参与制修订国际\\\\国家\\\\行业标准数量(个)、研发费用总额(元)、研发费用占营业收入比重(%)、有效发明专利并实际应用数量(个)、出口额(元)
                ```
                
                【本业务系统】对用户的【最近一次回答】内容如下：
                ``` 【系统回复】系统已成功执行“查询2023年行业最高营收企业数据”的行动计划，查询了2023年行业中营业收入最高的企业数据，并获取了其营业收入金额，最终通过信息块展示了结果。
                
                【追问用户】您可能还想了解：
                - 是否需要进一步分析该高营收企业的其他经营指标（如净利润、毛利率、研发投入等）？
                - 是否希望查看该企业在不同年份的营收变化趋势？
                - 是否需要将该企业的营收与同行业其他企业进行对比？
                
                您是否需要调整当前的行动计划或开启新的分析任务？
                 ```
                用户对系统的【历史请求】依次如下
                ``` 
                2025年07月15日 16:32:46, 用户说：2023 年，通用设备制造业里小巨人企业的数量是多少？
                2025年07月15日 16:32:59, 系统答：系统统计了2023年通用设备制造业小巨人企业数量。
                2025年07月15日 16:33:01, 用户说：与 2022 年相比，企业数量有多大变化？
                2025年07月15日 16:33:26, 系统答：系统统计了通用设备制造业企业数量变化。
                2025年07月15日 16:33:27, 用户说：2023 年，行业中营收最高的企业其营收额是多少？
                2025年07月15日 16:33:42, 系统答：系统查询并展示了2023年行业最高营收企业数据。 ```
                而当前的【用户请求】是
                ``` 对比 2022 年的营收额，其变化情况是如何的？ ```
                
                # 请根据以下规则，结合【历史请求】和【用户请求】，以Python代码的方式制定行动计划
                
                ## 1. 无条件触发的规则：行业知识
                当用户提及字段A时，需要把其他字段(B,C,D等)一起分析，
                以 A->(B,C,D)的方式给出如下：
                ```
                营业收入 ->(营业收入变化率，上一年的营业收入)
                销售费用 ->(销售费用变化率，上一年的销售费用)
                管理费用 ->(销售费用变化率，上一年的销售费用)
                毛利率 ->(上一年的毛利率)
                人均营业收入 ->(人均营业收入变化率，上一年的人均营业收入)
                营业收入增长率 ->(上一年的营业收入增长率)
                净利润总额 ->(净利润总额变化率，上一年的净利润总额)
                净利润增长率 ->(上一年的净利润增长率)
                研发费用总额 ->(研发费用总额变化率，上一年的研发费用总额)
                研发费用占营业收入比重 ->(上一年的研发费用占营业收入比重)
                出口额 ->(出口额变化率，上一年的出口额)
                ```
                ## 2、制定行动计划的规则
                那么请以Python代码的形式给出，代码要求如下：
                - 禁止假设返回值或者预设结果
                - 要处理查询不到数据或者除数为0等意外情况
                - gen_sql函数的输入是自然语言文本而非代码
                - SQL代码优先做聚合和连接等统计分析
                - Python代码优先做四则运算与格式转化
                - 只允许调用【系统功能】里声明的Python函数，不能调用Python内置的输入和输出函数
                - 要注意做好数据类型转换，例如int转换为float
                - 去掉所有注释文本
                - 直接返回由“```Python”和“```”前后包裹的代码，不要返回其他任何文本
                """;
        RequestConfig config = RequestConfig.thinkingConfig();
        config.setAliModelType(AliModelType.QWEN_3_32B_INSTRUCT);
        config.setTopP(0.01);
        config.setTemperature(0.0f);
        config.setMaxTokens(4096);

        DashScopeChatClient.ThinkingResult thinkingResult = aiService.chatWithThinking(prompt, config);
        String reasoning = thinkingResult.getReasoning();
        String content = thinkingResult.getContent();
        log.info("思考: \n{}", reasoning);
        log.info("最终输出: \n{}", content);
    }

    /**
     * 测试指定模型调用
     */
    @Test
    public void testSpecificModelChat() {
        log.info("=== 测试指定模型调用 ===");
        String response = aiService.chat("什么是量子计算？", AliModelType.QWEN_3_8B_INSTRUCT);
        log.info("指定模型调用响应: \n{}", response);
    }

    /**
     * 测试自定义配置调用
     */
    @Test
    public void testCustomConfigChat() {
        log.info("=== 测试自定义配置调用 ===");

        RequestConfig config = RequestConfig.builder()
                .aliModelType(AliModelType.QWEN_PLUS)
                .temperature(0.7f)
                .topP(0.9)
                .maxTokens(300)
                .build();

        String response = aiService.chat("写一首关于春天的诗", config);
        log.info("自定义配置调用响应: \n{}", response);
    }

    /**
     * 测试流式调用
     */
    @Test
    public void testStreamChat() throws InterruptedException {
        log.info("=== 测试流式调用 ===");

        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder resultBuilder = new StringBuilder();



        aiService.chatStream("请介绍中国的四大发明", AliModelType.QWEN_3_32B_INSTRUCT)
                .subscribe(
                        chunk -> {
                            resultBuilder.append(chunk);
                            log.info("收到流式片段: {}", chunk);
                        },
                        error -> {
                            log.error("流式调用出错", error);
                            latch.countDown();
                        },
                        () -> {
                            log.info("流式调用完成");
                            latch.countDown();
                        });

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        if (!completed) {
            log.warn("流式调用超时");
        }

        log.info("流式调用完整响应: \n{}", resultBuilder.toString());
    }

    /**
     * 测试思考过程获取
     */
    @Test
    public void testThinkingChat() {
        log.info("=== 测试思考过程获取 ===");

        DashScopeChatClient.ThinkingResult result = aiService.chatWithThinking("分析一下人工智能可能带来的风险和对策");

        log.info("思考过程: \n{}", result.getReasoning());
        log.info("最终回答: \n{}", result.getContent());
    }

    /**
     * 测试流式思考过程获取
     */
    @Test
    public void testStreamThinkingChat() throws InterruptedException {
        log.info("=== 测试流式思考过程获取 ===");

        // 创建自定义配置，增加超时时间
        RequestConfig config = RequestConfig.builder()
                .aliModelType(AliModelType.getModelWithCapability(com.mt.agent.ai.enums.ModelCapability.SUPPORTS_THINKING))
                .enableThinking(true)
                .timeoutSeconds(600) // 10分钟超时
                .build();

        CountDownLatch latch = new CountDownLatch(1);

        DashScopeChatClient.ThinkingStreamResult result = aiService.chatStreamWithThinking(
                "分析未来十年全球经济发展趋势", config);

        // 订阅思考过程流
        result.getReasoningFlowable().subscribe(
                reasoning -> log.info("思考过程片段: {}", reasoning),
                error -> {
                    log.error("获取思考过程出错", error);
                    latch.countDown();
                });

        // 订阅内容流
        result.getContentFlowable().subscribe(
                content -> log.info("内容片段: {}", content),
                error -> {
                    log.error("获取内容出错", error);
                    latch.countDown();
                },
                () -> {
                    log.info("流式思考过程获取完成");
                    latch.countDown();
                });

        // 使用配置中的超时时间，而不是硬编码的值
        log.info("等待流式思考过程完成，最长等待时间: {}秒", config.getTimeoutSeconds());
        boolean completed = latch.await(config.getTimeoutSeconds(), TimeUnit.SECONDS);
        if (!completed) {
            log.warn("流式思考过程获取超时（超过{}秒）", config.getTimeoutSeconds());
        }

        // 获取完整结果
        DashScopeChatClient.ThinkingResult completeResult = result.getCompleteResult();
        log.info("完整思考过程: \n{}", completeResult.getReasoning());
        log.info("完整回答: \n{}", completeResult.getContent());
    }

    /**
     * 测试复杂问题的流式思考
     * 这类问题可能需要更长的思考时间
     */
    @Test
    public void testComplexThinkingChat() throws InterruptedException {
        log.info("=== 测试复杂问题的流式思考过程获取 ===");

        String complexQuestion = "请详细分析人工智能在未来20年对全球经济、就业市场、社会结构和伦理道德的潜在"
                + "影响，并提出政府、企业和个人应当如何应对这些变化的具体建议。请从多个角度思考，包括技术发展路径、"
                + "法律法规调整、教育体系改革、社会保障机制完善等方面进行系统性分析。";

        // 创建长超时配置
        RequestConfig config = RequestConfig.builder()
                .aliModelType(AliModelType.getModelWithCapability(com.mt.agent.ai.enums.ModelCapability.SUPPORTS_THINKING))
                .enableThinking(true)
                .temperature(0.7f) // 适当提高创造性
                .timeoutSeconds(900) // 15分钟超时
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder reasoningBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();

        log.info("发送复杂问题: {}", complexQuestion);
        DashScopeChatClient.ThinkingStreamResult result = aiService.chatStreamWithThinking(complexQuestion, config);

        // 订阅思考过程流
        result.getReasoningFlowable().subscribe(
                reasoning -> {
                    reasoningBuilder.append(reasoning);
                    log.info("思考过程片段 [{}字符]", reasoning.length());
                },
                error -> {
                    log.error("获取思考过程出错", error);
                    latch.countDown();
                });

        // 订阅内容流
        result.getContentFlowable().subscribe(
                content -> {
                    contentBuilder.append(content);
                    log.info("内容片段 [{}字符]", content.length());
                },
                error -> {
                    log.error("获取内容出错", error);
                    latch.countDown();
                },
                () -> {
                    log.info("流式思考过程获取完成");
                    latch.countDown();
                });

        // 使用配置中的超时时间
        log.info("等待复杂思考过程完成，最长等待时间: {}秒", config.getTimeoutSeconds());
        boolean completed = latch.await(config.getTimeoutSeconds(), TimeUnit.SECONDS);
        if (!completed) {
            log.warn("复杂思考过程获取超时（超过{}秒）", config.getTimeoutSeconds());
        } else {
            log.info("复杂思考过程获取成功完成");
        }

        // 获取完整结果
        DashScopeChatClient.ThinkingResult completeResult = result.getCompleteResult();
        log.info("完整思考过程长度: {}字符", completeResult.getReasoning().length());
        log.info("完整回答长度: {}字符", completeResult.getContent().length());

        // 输出摘要
        String reasoningSummary = completeResult.getReasoning().length() > 200
                ? completeResult.getReasoning().substring(0, 200) + "..."
                : completeResult.getReasoning();
        String contentSummary = completeResult.getContent().length() > 200
                ? completeResult.getContent().substring(0, 200) + "..."
                : completeResult.getContent();

        log.info("思考过程摘要: \n{}", reasoningSummary);
        log.info("回答摘要: \n{}", contentSummary);
    }

    /**
     * 测试创造性配置调用
     */
    @Test
    public void testCreativeChat() {
        log.info("=== 测试创造性配置调用 ===");

        RequestConfig config = RequestConfig.creativeConfig();
        String response = aiService.chat("创作一个短篇科幻故事", config);

        log.info("创造性配置调用响应: \n{}", response);
    }
}