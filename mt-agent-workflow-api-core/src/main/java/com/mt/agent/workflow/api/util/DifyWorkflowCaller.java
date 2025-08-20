package com.mt.agent.workflow.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DifyWorkflowCaller {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public DifyWorkflowResponse executeWorkflow(String baseUrl, String apiKey, Map<String, Object> inputs, String user) {
        log.info("开始执行Dify工作流(WebClient)，baseUrl: {}, user: {}, inputs参数数量: ",
                baseUrl, user, inputs != null ? inputs.size() : 0);

        try {
            String url = baseUrl + "/workflows/run";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputs", inputs != null ? inputs : new HashMap<>());
            requestBody.put("response_mode", "blocking");
            
            if (user != null && !user.trim().isEmpty()) {
                requestBody.put("user", user);
            } else {
                String generatedUser = "user_" + UUID.randomUUID().toString().substring(0, 8);
                requestBody.put("user", generatedUser);
            }

            String responseBody = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMinutes(5)) // Set a timeout
                    .block(); // Block to wait for the response

            if (responseBody != null) {
                log.debug("工作流执行成功，响应内容长度: {}", responseBody.length());
                return parseWorkflowResponse(responseBody);
            } else {
                log.error("工作流执行失败，响应体为空");
                return DifyWorkflowResponse.error("工作流执行失败，响应体为空");
            }

        } catch (Exception e) {
            log.error("执行Dify工作流异常: {}", e.getMessage(), e);
            return DifyWorkflowResponse.error("工作流执行异常: " + e.getMessage());
        }
    }

    private DifyWorkflowResponse parseWorkflowResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            DifyWorkflowResponse response = new DifyWorkflowResponse();
            response.setSuccess(true);

            if (jsonNode.has("workflow_run_id")) {
                response.setWorkflowRunId(jsonNode.get("workflow_run_id").asText());
            }
            if (jsonNode.has("task_id")) {
                response.setTaskId(jsonNode.get("task_id").asText());
            }
            if (jsonNode.has("status")) {
                response.setStatus(jsonNode.get("status").asText());
            }
            if (jsonNode.has("data") && jsonNode.get("data").has("outputs")) {
                JsonNode outputsNode = jsonNode.get("data").get("outputs");
                Map<String, Object> outputs = new HashMap<>();
                outputsNode.fields().forEachRemaining(entry -> {
                    outputs.put(entry.getKey(), entry.getValue().asText());
                });
                response.setOutputs(outputs);
            }
            if (jsonNode.has("metadata")) {
                JsonNode metadataNode = jsonNode.get("metadata");
                Map<String, Object> metadata = new HashMap<>();
                metadataNode.fields().forEachRemaining(entry -> {
                    metadata.put(entry.getKey(), entry.getValue().asText());
                });
                response.setMetadata(metadata);
            }
            return response;
        } catch (Exception e) {
            log.error("解析工作流响应失败: {}", e.getMessage(), e);
            return DifyWorkflowResponse.error("响应解析失败: " + e.getMessage());
        }
    }

    // DifyWorkflowResponse inner class remains the same
    public static class DifyWorkflowResponse {
        private boolean success;
        private String errorMessage;
        private String workflowRunId;
        private String taskId;
        private String status;
        private Map<String, Object> outputs;
        private Map<String, Object> metadata;

        public DifyWorkflowResponse() {
            this.outputs = new HashMap<>();
            this.metadata = new HashMap<>();
        }

        public static DifyWorkflowResponse error(String message) {
            DifyWorkflowResponse response = new DifyWorkflowResponse();
            response.setSuccess(false);
            response.setErrorMessage(message);
            return response;
        }

        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getWorkflowRunId() {
            return workflowRunId;
        }

        public void setWorkflowRunId(String workflowRunId) {
            this.workflowRunId = workflowRunId;
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Map<String, Object> getOutputs() {
            return outputs;
        }

        public void setOutputs(Map<String, Object> outputs) {
            this.outputs = outputs;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        public String getOutput(String key) {
            Object value = outputs.get(key);
            return value != null ? value.toString() : null;
        }

        public String getText() {
            return getOutput("text");
        }

        @Override
        public String toString() {
            return "DifyWorkflowResponse{"
                    + "success=" + success +
                    ", errorMessage='" + errorMessage + '\'' +
                    ", workflowRunId='" + workflowRunId + '\'' +
                    ", taskId='" + taskId + '\'' +
                    ", status='" + status + '\'' +
                    ", outputs=" + outputs +
                    ", metadata=" + metadata +
                    '}';
        }
    }
}
