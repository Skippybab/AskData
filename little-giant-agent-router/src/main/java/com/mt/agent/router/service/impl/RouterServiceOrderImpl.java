package com.mt.agent.router.service.impl;

import com.mt.agent.ai.service.AiService;
import com.mt.agent.buffer.util.BufferUtil;
import com.mt.agent.consensus.util.ConsensusUtil;
import com.mt.agent.reporter.StepResultData;
import com.mt.agent.reporter.SubEventReporter;
import com.mt.agent.router.service.RouterService;
import com.mt.agent.service.CustomTaskService;
import com.mt.agent.sysUtil.FunctionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouterServiceOrderImpl implements RouterService {

    private final BufferUtil bufferUtil;

    private final AiService aiService;

    private final FunctionUtil functionUtil;

    private final ConsensusUtil consensusUtil;

    private final CustomTaskService customTaskService;

    @Override
    public void routeMatching(String userId, SubEventReporter reporter) {

        reporter.reportStep("开始解析任务方案");
        // 获取执行的python代码
        String pythonCode = bufferUtil.getPythonCode(userId);
        log.info("【python代码】:\n" + pythonCode);
        reporter.reportStepResult(new StepResultData());

        // 创建一个保存执行步骤的结果参数的集合
        HashMap<String, Object> paramMap = new HashMap<>();

        try {
            // 使用工具类解析java指令
//            List<JavaExecutable> javaExecutables = PythonCodeParserUtil.parseJavaOrders(pythonCode);
//            PythonCodeParserUtil.printParseResults(javaExecutables);
//            for (JavaExecutable javaExecutable : javaExecutables) {
//                // 执行java指令
//                customTaskService.executeJavaOrders(javaExecutable, paramMap, reporter);
//            }
            customTaskService.executePythonCode(pythonCode, paramMap, reporter, userId);

            // todo 不确定这里是否是执行完成了
            // 任务执行完成后，清空当前任务的共识缓存
//            consensusUtil.deleteConsensusJsonByUserId(userId);
            // 清除缓存
//            bufferUtil.clearUserCache(userId);

        } catch (Exception e) {
            log.error("【路由模块-任务分发】执行JavaFileExecutor时发生错误", e);
            throw new RuntimeException("【路由模块-任务分发】执行JavaFileExecutor时发生错误", e);
        }
    }

}
