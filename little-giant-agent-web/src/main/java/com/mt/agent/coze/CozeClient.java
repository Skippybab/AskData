package com.mt.agent.coze;

import com.coze.openapi.client.chat.CreateChatReq;
import com.coze.openapi.client.chat.model.ChatEvent;
import com.coze.openapi.client.connversations.message.model.Message;
import com.coze.openapi.client.workflows.run.RunWorkflowReq;
import com.coze.openapi.client.workflows.run.RunWorkflowResp;
import com.coze.openapi.client.workflows.run.model.WorkflowEvent;
import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.config.Consts;
import com.coze.openapi.service.service.CozeAPI;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.util.Collections;
import java.util.Map;

/**
 * Coze API客户端
 * 提供工作流执行等功能
 *
 * @author MT Team
 * @date 2025/5/28
 */
@Slf4j
@Component
public class CozeClient {

    /**
     * Coze API Token，从配置文件中读取
     */
    @Value("${coze.api.token:}")
    private String apiToken;

    /**
     * Coze API Base URL，从配置文件中读取，默认使用中国区域
     */
    @Value("${coze.api.base-url:}")
    private String baseUrl;

    /**
     * API调用超时时间（毫秒），默认100秒
     */
    @Value("${coze.api.timeout:1000000}")
    private int timeout;

    /**
     * CozeAPI实例，延迟初始化
     */
    private volatile CozeAPI cozeAPI;

    /**
     * 获取CozeAPI实例（单例模式，线程安全）
     *
     * @return CozeAPI实例
     * @throws IllegalStateException 当API Token未配置时抛出异常
     */
    private CozeAPI getCozeAPI() {
        if (cozeAPI == null) {
            synchronized (this) {
                if (cozeAPI == null) {
                    if (apiToken == null || apiToken.trim().isEmpty()) {
                        throw new IllegalStateException("Coze API Token未配置，请在配置文件中设置 coze.api.token");
                    }

                    // 创建认证客户端
                    TokenAuth authCli = new TokenAuth(apiToken);

                    // 确定Base URL
                    String finalBaseUrl = (baseUrl == null || baseUrl.trim().isEmpty())
                            ? Consts.COZE_CN_BASE_URL
                            : baseUrl;

                    // 构建CozeAPI客户端
                    cozeAPI = new CozeAPI.Builder()
                            .baseURL(finalBaseUrl)
                            .auth(authCli)
                            .readTimeout(timeout)
                            .build();

                    log.info("CozeAPI客户端初始化成功，Base URL: {}, Timeout: {}ms", finalBaseUrl, timeout);
                }
            }
        }
        return cozeAPI;
    }

    /**
     * 执行工作流
     *
     * @param params     工作流参数，包含工作流所需的输入数据
     * @param workflowId 工作流ID，标识要执行的具体工作流
     * @return Flowable<WorkflowEvent> 工作流事件流，可以订阅来接收工作流执行过程中的各种事件
     * @throws IllegalArgumentException 当参数无效时抛出异常
     * @throws IllegalStateException    当API客户端未正确初始化时抛出异常
     */
    public Flowable<WorkflowEvent> executeWorkflow(Map<String, Object> params, String workflowId) {
        // 参数验证
        if (workflowId == null || workflowId.trim().isEmpty()) {
            throw new IllegalArgumentException("工作流ID不能为空");
        }

        if (params == null) {
            throw new IllegalArgumentException("工作流参数不能为null");
        }

        log.debug("开始执行工作流，ID: {}, 参数: {}", workflowId, params);

        try {
            // 获取CozeAPI实例
            CozeAPI api = getCozeAPI();

            // 构建工作流执行请求
            RunWorkflowReq request = RunWorkflowReq.builder()
                    .workflowID(workflowId)
                    .parameters(params)
                    .build();

            // 执行工作流并返回事件流
            Flowable<WorkflowEvent> eventStream = api.workflows().runs().stream(request);

            // 增加详细的事件流日志
            return eventStream.doOnSubscribe(subscription -> {
//                log.debug("工作流事件流已订阅，工作流ID: {}", workflowId);
            }).doOnNext(event -> {
//                log.debug("接收到工作流事件: {}, 工作流ID: {}", event.getEvent(), workflowId);
            }).doOnError(error -> {
                log.error("工作流执行出错，工作流ID: {}, 错误: {}", workflowId, error.getMessage(), error);
            }).doOnComplete(() -> {
                log.debug("工作流事件流完成，工作流ID: {}", workflowId);
            });

        } catch (Exception e) {
            log.error("执行工作流时发生异常，工作流ID: {}, 错误信息: {}", workflowId, e.getMessage(), e);
            throw new RuntimeException("执行工作流失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行工作流(非流式)
     *
     * @param params     工作流参数，包含工作流所需的输入数据
     * @param workflowId 工作流ID，标识要执行的具体工作流
     * @return RunWorkflowResp 结果
     * @throws IllegalArgumentException 当参数无效时抛出异常
     * @throws IllegalStateException    当API客户端未正确初始化时抛出异常
     */
    public RunWorkflowResp runWorkflow(Map<String, Object> params, String workflowId) {
        // 参数验证
        if (workflowId == null || workflowId.trim().isEmpty()) {
            throw new IllegalArgumentException("工作流ID不能为空");
        }

        if (params == null) {
            throw new IllegalArgumentException("工作流参数不能为null");
        }

        log.debug("开始执行工作流，ID: {}, 参数: {}", workflowId, params);

        try {
            // 获取CozeAPI实例
            CozeAPI api = getCozeAPI();

            // 构建工作流执行请求
            RunWorkflowReq request = RunWorkflowReq.builder()
                    .workflowID(workflowId)
                    .parameters(params)
                    .build();

            // 执行工作流并返回事件流
            RunWorkflowResp resp = api.workflows().runs().create(request);
            log.info(resp.toString());
            return resp;

        } catch (Exception e) {
            log.error("执行工作流时发生异常，工作流ID: {}, 错误信息: {}", workflowId, e.getMessage(), e);
            throw new RuntimeException("执行工作流失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行工作流
     *
     * @param userId   智能体用户id
     * @param botId 智能体ID
     * @param content 对话内容
     * @throws IllegalArgumentException 当参数无效时抛出异常
     * @throws IllegalStateException    当API客户端未正确初始化时抛出异常
     */
    public Flowable<ChatEvent> botChat(String userId, String botId, String content) {
        log.debug("开始执行智能体对话，userId: {}, botId: {}", userId, botId);

        try {
            // 获取CozeAPI实例
            CozeAPI api = getCozeAPI();

            // 构建智能体对话请求
            CreateChatReq req = CreateChatReq.builder()
                            .botID(botId)
                            .userID(userId)
                            .messages(Collections.singletonList(Message.buildUserQuestionText(content)))
                            .build();

            // 执行工作流并返回事件流
            Flowable<ChatEvent> resp = api.chat().stream(req);

            // 增加详细的事件流日志
            return resp.doOnSubscribe(subscription -> {
            }).doOnNext(event -> {
            }).doOnError(error -> {
                log.error("智能体执行出错，智能体ID: {}, 错误: {}", botId, error.getMessage(), error);
            }).doOnComplete(() -> {
            });

        } catch (Exception e) {
            log.error("执行智能体时发生异常，智能体ID: {}, 错误信息: {}", botId, e.getMessage(), e);
            throw new RuntimeException("执行智能体失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查API客户端是否已初始化且可用
     *
     * @return true表示客户端可用，false表示不可用
     */
    public boolean isAvailable() {
        try {
            return apiToken != null && !apiToken.trim().isEmpty();
        } catch (Exception e) {
            log.warn("检查Coze客户端可用性时发生异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前配置的API Token（脱敏显示）
     *
     * @return 脱敏后的Token字符串
     */
    public String getMaskedToken() {
        if (apiToken == null || apiToken.length() < 8) {
            return "未配置";
        }
        return apiToken.substring(0, 8) + "***";
    }

    /**
     * 应用关闭时清理资源
     * 注意：调用此方法后，如果需要继续使用，会自动重新初始化
     */
    @PreDestroy
    public void destroy() {
        if (cozeAPI != null) {
            try {
                cozeAPI.shutdownExecutor();
                log.info("CozeAPI客户端资源已清理");
            } catch (Exception e) {
                log.warn("清理CozeAPI客户端资源时发生异常: {}", e.getMessage());
            } finally {
                // 重置实例，以便后续可以重新初始化
                synchronized (this) {
                    cozeAPI = null;
                }
            }
        }
    }

    /**
     * 检查当前API客户端是否可用（未被销毁）
     *
     * @return true表示客户端可用，false表示已被销毁或未初始化
     */
    public boolean isClientActive() {
        return cozeAPI != null;
    }
}
