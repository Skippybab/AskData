package com.mt.agent.test;

import cn.hutool.json.JSONObject;
import com.coze.openapi.client.workflows.run.*;
import com.coze.openapi.client.workflows.run.model.WorkflowEvent;
import com.coze.openapi.client.workflows.run.model.WorkflowEventType;
import com.coze.openapi.client.workflows.run.model.WorkflowExecuteStatus;
import com.coze.openapi.client.workflows.run.model.WorkflowRunHistory;
import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.config.Consts;
import com.coze.openapi.service.service.CozeAPI;
import com.mt.agent.consensus.util.ConsensusUtil;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * coze 调用测试
 *
 * @date 2025/5/28
 */
@SpringBootTest
@Slf4j
public class CozeTest2 {

    @Autowired
    private ConsensusUtil  consensusUtil;

    @Test
    public void cozeTest() {
        // 设置访问令牌
        String token = "pat_EAPgPTk66MF2y4cjIk4xxppMqiMNwXDDuhlwCAHXDNo9SaX7K4eogkrmxB10J7Ip";
        TokenAuth authCli = new TokenAuth(token);

        // 初始化 Coze 客户端
        CozeAPI coze =
                new CozeAPI.Builder()
                        .baseURL(Consts.COZE_CN_BASE_URL)
                        .auth(authCli)
                        .readTimeout(100000)
                        .build();

        String workflowID = "7509309456992845862";

        // 如果工作流需要输入参数，可以通过 Map 发送
        Map<String, Object> data = new HashMap<>();
        data.put("content", "2023年医药制造业的营收情况");
        data.put("userId", "4");

        RunWorkflowReq req =
                RunWorkflowReq.builder()
                        .workflowID(workflowID)
                        .parameters(data)
                        // if you want the workflow run asynchronously, you must set isAsync to true.
                        .isAsync(true)
                        .build();

    /*
    Call the  coze.workflows().runs().run() method to create a workflow run. The create method
    is a non-streaming chat and will return a WorkflowRunResult class.
    * */
        RunWorkflowResp resp = coze.workflows().runs().create(req);
        log.info("异步工作流执行：{}", resp.getExecuteID());

        String executeID = resp.getExecuteID();
        boolean isFinished = false;

        while (!isFinished) {
            RetrieveRunHistoryResp historyResp =
                    coze.workflows()
                            .runs()
                            .histories()
                            .retrieve(RetrieveRunHistoryReq.of(workflowID, executeID));
//            log.info(historyResp.getHistories().get(0).getOutput());
            WorkflowRunHistory history = historyResp.getHistories().get(0);
            if (history.getExecuteStatus().equals(WorkflowExecuteStatus.FAIL)) {
                log.error("Workflow run failed, reason:{}" , history.getErrorMessage());
                isFinished = true;
                break;
            } else if (history.getExecuteStatus().equals(WorkflowExecuteStatus.SUCCESS)) {
                log.info("工作流执行成功！");
                log.info("工作流执行结果：\n{}", history.getOutput());
                isFinished = true;
                break;
            }
        }
    }



}