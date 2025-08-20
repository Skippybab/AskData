package com.mt.agent.test;
import java.util.HashMap;
import java.util.Map;

import com.coze.openapi.client.workflows.run.ResumeRunReq;
import com.coze.openapi.client.workflows.run.RunWorkflowReq;
import com.coze.openapi.client.workflows.run.model.WorkflowEvent;
import com.coze.openapi.client.workflows.run.model.WorkflowEventType;
import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.service.CozeAPI;

import io.reactivex.Flowable;

/*
This example describes how to use the workflow interface to stream chat.
*/
public class StreamWorkflowExample {

    public static void main(String[] args) {
        // Get an access_token through personal access token or oauth.
        String token = "pat_EAPgPTk66MF2y4cjIk4xxppMqiMNwXDDuhlwCAHXDNo9SaX7K4eogkrmxB10J7Ip";
        TokenAuth authCli = new TokenAuth(token);

        // Init the Coze client through the access_token.
        CozeAPI coze =
                new CozeAPI.Builder()
                        .baseURL("https://api.coze.cn")
                        .auth(authCli)
                        .readTimeout(100000)
                        .build();

        String workflowID = "7508921133278986259";

        // if your workflow need input params, you can send them by map
        Map<String, Object> data = new HashMap<>();
        data.put("userId", "2");
        data.put("content", "查询2023年企业营收情况");

        RunWorkflowReq req = RunWorkflowReq.builder().workflowID(workflowID).parameters(data).build();

        Flowable<WorkflowEvent> flowable = coze.workflows().runs().stream(req);
        handleEvent(flowable, coze, workflowID);
    }

    /*
     * The stream interface will return an iterator of WorkflowEvent. Developers should iterate
     * through this iterator to obtain WorkflowEvent and handle them separately according to
     * the type of WorkflowEvent.
     */
    private static void handleEvent(Flowable<WorkflowEvent> events, CozeAPI coze, String workflowID) {
        events.subscribe(
                event -> {
                    if (event.getEvent().equals(WorkflowEventType.MESSAGE)) {
                        System.out.println("Got message" + event.getMessage());
                    } else if (event.getEvent().equals(WorkflowEventType.ERROR)) {
                        System.out.println("Got error" + event.getError());
                    } else if (event.getEvent().equals(WorkflowEventType.DONE)) {
                        System.out.println("Got message" + event.getMessage());
                    } else if (event.getEvent().equals(WorkflowEventType.INTERRUPT)) {
                        handleEvent(
                                coze.workflows()
                                        .runs()
                                        .resume(
                                                ResumeRunReq.builder()
                                                        .workflowID(workflowID)
                                                        .eventID(event.getInterrupt().getInterruptData().getEventID())
                                                        .resumeData("your data")
                                                        .interruptType(event.getInterrupt().getInterruptData().getType())
                                                        .build()),
                                coze,
                                workflowID);
                    }
                },
                Throwable::printStackTrace);
        coze.shutdownExecutor();
    }
}
