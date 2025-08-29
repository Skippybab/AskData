package com.mt.agent.workflow.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.mt.agent.workflow.api.config.DifyConfig;
import com.mt.agent.workflow.api.service.DifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DifyServiceImpl implements DifyService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private DifyConfig difyConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Flux<String> streamChat(String allTableNames, String userInput, List<Map<String, String>> history, String lastReply, String user) {
        // 使用 workflow 端点
        String url = difyConfig.getNl2sql().getBaseUrl() + "/workflows/run";
        String apiKey = difyConfig.getNl2sql().getApiKey();

        log.info("调用Dify workflow API（流式），URL: {}, 用户问题: {}", url, userInput);

        // 构造 inputs
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("all_table_names", allTableNames == null ? "" : allTableNames);
        inputs.put("input", userInput == null ? "" : userInput);
        inputs.put("history", buildHistoryString(history));
        inputs.put("last_reply", lastReply == null ? "null" : lastReply);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", inputs);
        requestBody.put("response_mode", "streaming");
        requestBody.put("user", user);

        log.info("Dify API输入参数详情:");
        log.info("  - all_table_names长度: {}", allTableNames != null ? allTableNames.length() : 0);
        log.info("  - input: {}", userInput);
        log.info("  - history处理后: {}", buildHistoryString(history));
        log.info("  - last_reply: {}", lastReply);
        log.debug("Dify workflow API请求体: {}", requestBody);

        return webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response -> {
                    log.error("Dify workflow API客户端错误: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("Dify workflow API错误响应体: {}", body);
                            return Mono.error(new RuntimeException("Dify workflow API客户端错误: " + response.statusCode() + " - " + body));
                        });
                })
                .onStatus(status -> status.is5xxServerError(), response -> {
                    log.error("Dify workflow API服务器错误: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("Dify workflow API错误响应体: {}", body);
                            return Mono.error(new RuntimeException("Dify workflow API服务器错误: " + response.statusCode() + " - " + body));
                        });
                })
                .bodyToFlux(String.class)
                .timeout(Duration.ofMinutes(5))
                .doOnNext(data -> log.debug("收到Dify workflow响应: {}", data))
                .doOnError(error -> log.error("Dify workflow API调用失败: {}", error.getMessage(), error))
                .doOnComplete(() -> log.info("Dify workflow API调用完成"))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                    .filter(error -> error instanceof WebClientResponseException &&
                           ((WebClientResponseException) error).getStatusCode().is5xxServerError()));
    }

    /**
     * 阻塞式调用Dify workflow API
     */
    @Override
    public Mono<String> blockingChat(String allTableNames, String userInput, List<Map<String, String>> history, String lastReply, String user) {
        // 使用 workflow 端点
        String url = difyConfig.getNl2sql().getBaseUrl() + "/workflows/run";
        String apiKey = difyConfig.getNl2sql().getApiKey();

        log.info("调用Dify workflow API（阻塞式），URL: {}, 用户问题: {}", url, userInput);

        // 构造 inputs
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("all_table_names", allTableNames == null ? "" : allTableNames);
        inputs.put("input", userInput == null ? "" : userInput);
        inputs.put("history", buildHistoryString(history));
        inputs.put("last_reply", lastReply == null ? "null" : lastReply);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("inputs", inputs);
        requestBody.put("response_mode", "blocking");
        requestBody.put("user", user);

        log.info("Dify API输入参数详情:");
        log.info("  - URL: {}", url);
        log.info("  - all_table_names长度: {}", allTableNames != null ? allTableNames.length() : 0);
        log.info("  - input: {}", userInput);
        log.info("  - history处理后: {}", buildHistoryString(history));
        log.info("  - last_reply: {}", lastReply);
        log.debug("Dify workflow API请求体: {}", requestBody);

        return webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response -> {
                    log.error("Dify workflow API客户端错误: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("Dify workflow API错误响应体: {}", body);
                            return Mono.error(new RuntimeException("Dify workflow API客户端错误: " + response.statusCode() + " - " + body));
                        });
                })
                .onStatus(status -> status.is5xxServerError(), response -> {
                    log.error("Dify workflow API服务器错误: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("Dify workflow API错误响应体: {}", body);
                            return Mono.error(new RuntimeException("Dify workflow API服务器错误: " + response.statusCode() + " - " + body));
                        });
                })
                .bodyToMono(String.class)
                .timeout(Duration.ofMinutes(5))
                .doOnNext(data -> {
                    // log.debug("收到Dify workflow阻塞式响应，内容: {}", data);
                    try {
                        // 解析JSON响应，提取output内容
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode jsonNode = objectMapper.readTree(data);
                        if (jsonNode.has("data") && jsonNode.get("data").has("outputs")) {
                            JsonNode outputs = jsonNode.get("data").get("outputs");
                            if (outputs.has("text")) {
                                String outputText = outputs.get("text").asText();
                                log.info("收到Dify workflow阻塞式响应内容: {}", outputText);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("解析Dify API响应JSON失败: {}", e.getMessage());
                    }
                })
                .doOnError(error -> log.error("Dify workflow API调用失败: {}", error.getMessage(), error))
                .doOnSuccess(data -> log.info("Dify workflow API调用完成，响应长度: {}", data.length()))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                    .filter(error -> error instanceof WebClientResponseException &&
                           ((WebClientResponseException) error).getStatusCode().is5xxServerError()));
    }

    private String buildHistoryString(List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) return "null";
        try {
            return history.stream()
                    .map(m -> (m.getOrDefault("role", "") + ": " + m.getOrDefault("content", "")))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.warn("聚合history失败，使用null，原因: {}", e.getMessage());
            return "null";
        }
    }
}
